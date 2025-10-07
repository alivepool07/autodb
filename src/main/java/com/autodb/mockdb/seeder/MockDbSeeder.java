package com.autodb.mockdb.seeder;

import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.provider.ValueProvider;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class MockDbSeeder {

    private final EntityManager em;
    private final MockDbProperties props;
    private final ValueProvider provider;
    private final Random rnd = new Random();

    // hold created managed instances per entity class for linking
    private final Map<Class<?>, List<Object>> created = new HashMap<>();

    public MockDbSeeder(EntityManager em, MockDbProperties props, ValueProvider provider) {
        this.em = em;
        this.props = props;
        this.provider = provider;
    }

    public void seedAll() {
        if (!props.isEnabled()) {
            return;
        }
        int perEntity = props.resolveCount();
        System.out.println("[mockdb] seeding mode=" + props.getLevel() + " count=" + perEntity);

        Set<EntityType<?>> entityTypes = em.getMetamodel().getEntities();
        // filter out entities that map to non-class types (safety)
        List<Class<?>> entities = entityTypes.stream()
                .map(EntityType::getJavaType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // topological-like ordering by ManyToOne dependencies (best-effort)
        List<Class<?>> order = orderByDependencies(entities);

        // 1) create initial instances (scalars + try to wire relations to already-created)
        for (Class<?> cls : order) {
            createInstances(cls, perEntity);
        }

        // 2) second pass: ensure references are fixed (if a related type had no instances at first)
        fixMissingReferences();

        // 3) build OneToMany collections on parent side (based on child -> parent links)
        populateCollections();

        // flush to ensure DB has data
        em.flush();
        System.out.println("[mockdb] seeding completed; persisted counts:");
        created.forEach((k, v) -> System.out.println("  " + k.getSimpleName() + ": " + v.size()));
    }

    // Create count instances for cls (if already partially created, we create more to reach count)
    private void createInstances(Class<?> cls, int count) {
        List<Object> list = created.computeIfAbsent(cls, k -> new ArrayList<>());
        int toCreate = Math.max(0, count - list.size());
        for (int i = 0; i < toCreate; i++) {
            try {
                Object inst = instantiateAndPopulate(cls, i);
                em.persist(inst);
                list.add(inst);
            } catch (Exception ex) {
                System.err.println("[mockdb] failed to create instance of " + cls.getName() + ": " + ex.getMessage());
            }
        }
    }

    private Object instantiateAndPopulate(Class<?> cls, int index) throws Exception {
        Object inst = cls.getDeclaredConstructor().newInstance();

        for (Field f : getAllFields(cls)) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);

            if (f.isAnnotationPresent(Id.class) && f.isAnnotationPresent(GeneratedValue.class)) {
                continue; // leave DB to generate
            }

            // skip collection sides now; will be populated later
            if (f.isAnnotationPresent(OneToMany.class) || f.isAnnotationPresent(ManyToMany.class)) {
                continue;
            }

            // Relations: try to set ManyToOne/OneToOne from existing pools
            if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                Class<?> target = f.getType();
                List<Object> pool = created.get(target);
                if (pool != null && !pool.isEmpty()) {
                    Object pick = pool.get(rnd.nextInt(pool.size()));
                    try { f.set(inst, pick); } catch (Throwable ignore) {}
                } else {
                    // leave null for now; we'll fix in second pass
                }
                continue;
            }

            // scalar fields -> use provider
            try {
                Object val = provider.provideValue(cls, f, index);
                if (val != null) f.set(inst, val);
            } catch (Throwable t) {
                // ignore
            }
        }

        return inst;
    }

    // After initial creation, ensure all relations have some linked entities
    private void fixMissingReferences() {
        for (Map.Entry<Class<?>, List<Object>> e : created.entrySet()) {
            Class<?> cls = e.getKey();
            List<Object> instances = e.getValue();
            for (Object inst : instances) {
                for (Field f : getAllFields(cls)) {
                    if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                        try {
                            f.setAccessible(true);
                            Object cur = f.get(inst);
                            if (cur == null) {
                                Class<?> target = f.getType();
                                List<Object> pool = created.get(target);
                                if (pool == null || pool.isEmpty()) {
                                    // if the target has no instances, create at least one
                                    createInstances(target, Math.max(1, props.resolveCount()/10));
                                    pool = created.get(target);
                                }
                                if (pool != null && !pool.isEmpty()) {
                                    Object pick = pool.get(rnd.nextInt(pool.size()));
                                    f.set(inst, pick);
                                }
                            }
                        } catch (Throwable t) {
                            // ignore per-field failures
                        }
                    }
                }
            }
        }
        // ensure DB knows about relations we set (merge not required for managed entities, but flush to be safe)
        em.flush();
    }

    // Build parent collections (OneToMany) by scanning child->parent links we already persisted
    private void populateCollections() {
        // for each parent class, for each OneToMany field, find child class and collect
        for (Map.Entry<Class<?>, List<Object>> parentEntry : created.entrySet()) {
            Class<?> parentClass = parentEntry.getKey();
            List<Object> parentInstances = parentEntry.getValue();

            for (Field parentField : getAllFields(parentClass)) {
                if (!parentField.isAnnotationPresent(OneToMany.class)) continue;
                parentField.setAccessible(true);

                // determine child type from generic param
                Class<?> childClass = extractGenericListType(parentField);
                if (childClass == null) continue;

                List<Object> childPool = created.getOrDefault(childClass, Collections.emptyList());

                // map parent -> children
                for (Object parentInst : parentInstances) {
                    List<Object> childrenForThisParent = new ArrayList<>();
                    for (Object childInst : childPool) {
                        // find a field on child that references parentClass (ManyToOne or OneToOne)
                        for (Field childField : getAllFields(childClass)) {
                            if (childField.getType().equals(parentClass) &&
                                    (childField.isAnnotationPresent(ManyToOne.class) || childField.isAnnotationPresent(OneToOne.class))) {
                                try {
                                    childField.setAccessible(true);
                                    Object val = childField.get(childInst);
                                    if (val != null && val.equals(parentInst)) {
                                        childrenForThisParent.add(childInst);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                    // set the collection on parent
                    try {
                        parentField.set(parentInst, childrenForThisParent);
                    } catch (Throwable ignored) {}
                }
            }
        }

        // flush so changes are applied
        em.flush();
    }

    // Extract generic type of List<X> for OneToMany
    private Class<?> extractGenericListType(Field field) {
        try {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType p) {
                Type[] args = p.getActualTypeArguments();
                if (args != null && args.length == 1) {
                    Type arg = args[0];
                    if (arg instanceof Class<?> c) return (Class<?>) c;
                    if (arg instanceof java.lang.reflect.ParameterizedType pt) {
                        if (pt.getRawType() instanceof Class<?> raw) return raw;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // get all fields including superclasses
    private List<Field> getAllFields(Class<?> cls) {
        List<Field> res = new ArrayList<>();
        Class<?> t = cls;
        while (t != null && t != Object.class) {
            Collections.addAll(res, t.getDeclaredFields());
            t = t.getSuperclass();
        }
        return res;
    }

    // Best-effort ordering by dependencies (entities that depend on others come later)
    private List<Class<?>> orderByDependencies(List<Class<?>> entities) {
        Map<Class<?>, Set<Class<?>>> deps = new HashMap<>();
        for (Class<?> cls : entities) {
            Set<Class<?>> dset = new HashSet<>();
            for (Field f : getAllFields(cls)) {
                if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                    Class<?> target = f.getType();
                    if (entities.contains(target)) dset.add(target);
                }
            }
            deps.put(cls, dset);
        }

        // Kahn-like algorithm
        List<Class<?>> result = new ArrayList<>();
        Map<Class<?>, Integer> indeg = new HashMap<>();
        for (Class<?> c : entities) indeg.put(c, deps.getOrDefault(c, Collections.emptySet()).size());

        Queue<Class<?>> q = new ArrayDeque<>();
        for (Map.Entry<Class<?>, Integer> entry : indeg.entrySet()) {
            if (entry.getValue() == 0) q.add(entry.getKey());
        }

        while (!q.isEmpty()) {
            Class<?> c = q.poll();
            result.add(c);
            // remove c from others' dependency sets
            for (Class<?> other : entities) {
                Set<Class<?>> s = deps.get(other);
                if (s != null && s.remove(c)) {
                    indeg.put(other, indeg.getOrDefault(other, 0) - 1);
                    if (indeg.get(other) == 0) q.add(other);
                }
            }
        }

        // if cycles remain, add them in any order
        for (Class<?> c : entities) if (!result.contains(c)) result.add(c);

        return result;
    }
}
