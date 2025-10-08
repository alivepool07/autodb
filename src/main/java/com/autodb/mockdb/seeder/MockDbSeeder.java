package com.autodb.mockdb.seeder;

import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.provider.ValueProvider;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MockDbSeeder {

    private final EntityManager em;
    private final MockDbProperties props;
    private final ValueProvider provider;
    private final Random rnd = new Random();
    private final Map<Class<?>, List<Object>> created = new HashMap<>();

    public MockDbSeeder(EntityManager em, MockDbProperties props, ValueProvider provider) {
        this.em = em;
        this.props = props;
        this.provider = provider;
    }

    public void seedAll() throws IllegalAccessException {
        if (!props.isEnabled()) return;

        int perEntity = props.resolveCount();
        System.out.println("[mockdb] seeding mode=" + props.getLevel() + " count=" + perEntity);

        // get all entities
        Set<EntityType<?>> entityTypes = em.getMetamodel().getEntities();
        List<Class<?>> entities = entityTypes.stream()
                .map(EntityType::getJavaType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // order by dependencies safely (handles cycles)
        List<Class<?>> ordered = orderByDependenciesCycleSafe(entities);

        // 1) create instances
        for (Class<?> cls : ordered) {
            createInstances(cls, perEntity);
        }

        // 2) fix missing references
        fixMissingReferences();

        // 3) populate OneToMany collections
        populateCollections();

        // 4) populate ManyToMany join relations
        populateManyToManyRelations();

        System.out.println("[mockdb] seeding completed; persisted counts:");
        created.forEach((k, v) -> System.out.println("  " + k.getSimpleName() + ": " + v.size()));
    }

    private void createInstances(Class<?> cls, int count) {
        List<Object> list = created.computeIfAbsent(cls, k -> new ArrayList<>());
        int toCreate = Math.max(0, count - list.size());
        for (int i = 0; i < toCreate; i++) {
            try {
                Object inst = instantiateAndPopulate(cls, i);
                em.persist(inst);
                list.add(inst);
            } catch (Exception ex) {
                System.err.println("[mockdb] failed to create " + cls.getSimpleName() + ": " + ex.getMessage());
            }
        }
    }

    private Object instantiateAndPopulate(Class<?> cls, int index) throws Exception {
        Object inst = cls.getDeclaredConstructor().newInstance();

        for (Field f : getAllFields(cls)) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);

            if (f.isAnnotationPresent(Id.class) && f.isAnnotationPresent(GeneratedValue.class)) continue;
            if (f.isAnnotationPresent(OneToMany.class) || f.isAnnotationPresent(ManyToMany.class)) continue;

            if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                Class<?> target = f.getType();
                List<Object> pool = created.get(target);
                if (pool != null && !pool.isEmpty()) {
                    f.set(inst, pool.get(rnd.nextInt(pool.size())));
                }
                continue;
            }

            try {
                Object val = provider.provideValue(cls, f, index);
                if (val != null) f.set(inst, val);
            } catch (Throwable ignored) {}
        }

        return inst;
    }

    private void fixMissingReferences() {
        for (Map.Entry<Class<?>, List<Object>> e : created.entrySet()) {
            Class<?> cls = e.getKey();
            List<Object> instances = e.getValue();
            for (Object inst : instances) {
                for (Field f : getAllFields(cls)) {
                    if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                        try {
                            f.setAccessible(true);
                            if (f.get(inst) == null) {
                                Class<?> target = f.getType();
                                List<Object> pool = created.get(target);
                                if (pool == null || pool.isEmpty()) {
                                    createInstances(target, Math.max(1, props.resolveCount() / 10));
                                    pool = created.get(target);
                                }
                                if (!pool.isEmpty()) f.set(inst, pool.get(rnd.nextInt(pool.size())));
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        em.flush();
    }

    private void populateCollections() throws IllegalAccessException {
        for (Map.Entry<Class<?>, List<Object>> parentEntry : created.entrySet()) {
            Class<?> parentClass = parentEntry.getKey();
            List<Object> parentInstances = parentEntry.getValue();

            for (Field parentField : getAllFields(parentClass)) {
                if (!parentField.isAnnotationPresent(OneToMany.class)) continue;
                parentField.setAccessible(true);

                Class<?> childClass = extractGenericListType(parentField);
                if (childClass == null) continue;

                List<Object> childPool = created.getOrDefault(childClass, Collections.emptyList());

                for (Object parentInst : parentInstances) {
                    List<Object> children = new ArrayList<>();
                    for (Object childInst : childPool) {
                        for (Field childField : getAllFields(childClass)) {
                            if (childField.getType().equals(parentClass) &&
                                    (childField.isAnnotationPresent(ManyToOne.class) || childField.isAnnotationPresent(OneToOne.class))) {
                                childField.setAccessible(true);
                                Object val = childField.get(childInst);
                                if (val != null && val.equals(parentInst)) {
                                    children.add(childInst);
                                }
                            }
                        }
                    }
                    try { parentField.set(parentInst, children); } catch (Throwable ignored) {}
                }
            }
        }
        em.flush();
    }

    private void populateManyToManyRelations() {
        for (Map.Entry<Class<?>, List<Object>> entry : created.entrySet()) {
            Class<?> cls = entry.getKey();
            List<Object> instances = entry.getValue();

            for (Field f : getAllFields(cls)) {
                if (!f.isAnnotationPresent(ManyToMany.class)) continue;
                f.setAccessible(true);

                Class<?> targetType = extractGenericListType(f);
                if (targetType == null) continue;

                List<Object> targetPool = created.getOrDefault(targetType, Collections.emptyList());
                if (targetPool.isEmpty()) continue;

                for (Object source : instances) {
                    try {
                        int linkCount = 1 + rnd.nextInt(Math.min(5, targetPool.size()));
                        Collections.shuffle(targetPool);
                        List<Object> selected = new ArrayList<>(targetPool.subList(0, linkCount));
                        f.set(source, selected);
                    } catch (Throwable ignored) {}
                }
            }
        }
        em.flush();
    }

    private Class<?> extractGenericListType(Field field) {
        try {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType p) {
                Type[] args = p.getActualTypeArguments();
                if (args.length == 1) {
                    if (args[0] instanceof Class<?> c) return c;
                    if (args[0] instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) return raw;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private List<Field> getAllFields(Class<?> cls) {
        List<Field> res = new ArrayList<>();
        for (Class<?> t = cls; t != null && t != Object.class; t = t.getSuperclass()) {
            Collections.addAll(res, t.getDeclaredFields());
        }
        return res;
    }

    /**
     * Orders entities by dependency graph while handling cycles gracefully.
     * If a cycle is detected, those entities are grouped and placed together.
     */
    private List<Class<?>> orderByDependenciesCycleSafe(List<Class<?>> entities) {
        Map<Class<?>, Set<Class<?>>> deps = new HashMap<>();
        for (Class<?> cls : entities) {
            Set<Class<?>> dset = new HashSet<>();
            for (Field f : getAllFields(cls)) {
                if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                    Class<?> target = f.getType();
                    if (entities.contains(target) && !target.equals(cls)) dset.add(target);
                }
            }
            deps.put(cls, dset);
        }

        // use Kahn’s algorithm with cycle fallback
        List<Class<?>> result = new ArrayList<>();
        Set<Class<?>> remaining = new HashSet<>(entities);

        while (!remaining.isEmpty()) {
            List<Class<?>> free = remaining.stream()
                    .filter(c -> deps.getOrDefault(c, Collections.emptySet())
                            .stream().noneMatch(remaining::contains))
                    .collect(Collectors.toList());

            if (free.isEmpty()) {
                // cycle detected — add all remaining in arbitrary order and break
                result.addAll(remaining);
                break;
            }

            result.addAll(free);
            remaining.removeAll(free);
        }

        return result;
    }
}
