package com.autodb.mockdb.seeder.implementation;

import com.autodb.mockdb.seeder.CollectionPopulatorService;
import jakarta.persistence.EntityManager;
import com.autodb.mockdb.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

public class CollectionPopulator implements CollectionPopulatorService {

    private final EntityManager em;
    private final Map<Class<?>, List<Object>> created;
    private final Random rnd = new Random();

    public CollectionPopulator(EntityManager em, Map<Class<?>, List<Object>> created) {
        this.em = em;
        this.created = created;
    }

    @Override
    public void populateCollections() throws IllegalAccessException {
        for (Map.Entry<Class<?>, List<Object>> parentEntry : created.entrySet()) {
            Class<?> parentClass = parentEntry.getKey();
            List<Object> parentInstances = parentEntry.getValue();

            for (Field parentField : ReflectionUtils.getAllFields(parentClass)) {
                if (!parentField.isAnnotationPresent(jakarta.persistence.OneToMany.class)) continue;
                parentField.setAccessible(true);

                Class<?> childClass = ReflectionUtils.extractGenericListType(parentField);
                if (childClass == null) continue;

                List<Object> childPool = created.getOrDefault(childClass, Collections.emptyList());

                for (Object parentInst : parentInstances) {
                    List<Object> children = new ArrayList<>();
                    for (Object childInst : childPool) {
                        for (Field childField : ReflectionUtils.getAllFields(childClass)) {
                            if (childField.getType().equals(parentClass) &&
                                    (childField.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                                            childField.isAnnotationPresent(jakarta.persistence.OneToOne.class))) {
                                childField.setAccessible(true);
                                Object val = childField.get(childInst);
                                if (val != null && val.equals(parentInst)) children.add(childInst);
                            }
                        }
                    }
                    parentField.set(parentInst, children);
                }
            }
        }
        em.flush();

    }

    @Override
    public void populateManyToManyRelations() throws IllegalAccessException {
        for (Map.Entry<Class<?>, List<Object>> entry : created.entrySet()) {
            Class<?> cls = entry.getKey();
            List<Object> instances = entry.getValue();

            for (Field f : ReflectionUtils.getAllFields(cls)) {
                if (!f.isAnnotationPresent(jakarta.persistence.ManyToMany.class)) continue;
                f.setAccessible(true);

                Class<?> targetType = ReflectionUtils.extractGenericListType(f);
                if (targetType == null) continue;

                List<Object> targetPool = created.getOrDefault(targetType, Collections.emptyList());
                if (targetPool.isEmpty()) continue;

                for (Object source : instances) {
                    int linkCount = 1 + rnd.nextInt(Math.min(5, targetPool.size()));
                    Collections.shuffle(targetPool);
                    List<Object> selected = new ArrayList<>(targetPool.subList(0, linkCount));
                    f.set(source, selected);
                }
            }
        }
        em.flush();

        System.out.println("[mockdb] seeding completed; persisted counts:");
        created.forEach((k, v) -> System.out.println("  " + k.getSimpleName() + ": " + v.size()));
    }
}

