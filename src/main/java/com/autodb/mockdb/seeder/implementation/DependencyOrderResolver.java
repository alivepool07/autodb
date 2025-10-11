package com.autodb.mockdb.seeder.implementation;

import com.autodb.mockdb.util.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.*;


public class DependencyOrderResolver {

    public static List<Class<?>> orderByDependenciesCycleSafe(List<Class<?>> entities) {
        Map<Class<?>, Set<Class<?>>> deps = new HashMap<>();
        for (Class<?> cls : entities) {
            Set<Class<?>> dset = new HashSet<>();
            for (Field f : ReflectionUtils.getAllFields(cls)) {
                if (f.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                        f.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
                    Class<?> target = f.getType();
                    if (entities.contains(target) && !target.equals(cls)) dset.add(target);
                }
            }
            deps.put(cls, dset);
        }

        List<Class<?>> result = new ArrayList<>();
        Set<Class<?>> remaining = new HashSet<>(entities);

        while (!remaining.isEmpty()) {
            List<Class<?>> free = remaining.stream()
                    .filter(c -> deps.getOrDefault(c, Collections.emptySet())
                            .stream().noneMatch(remaining::contains))
                    .toList();

            if (free.isEmpty()) {
                result.addAll(remaining);
                break;
            }

            result.addAll(free);
            free.forEach(remaining::remove);
        }

        return result;
    }
}
