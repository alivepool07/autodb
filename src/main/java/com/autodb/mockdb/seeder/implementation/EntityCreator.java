package com.autodb.mockdb.seeder.implementation;
import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.provider.ValueProvider;
import com.autodb.mockdb.seeder.EntityCreatorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import com.autodb.mockdb.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class EntityCreator implements EntityCreatorService {

    private final EntityManager em;
    private final MockDbProperties props;
    private final ValueProvider provider;
    private final Random rnd = new Random();
    private final Map<Class<?>, List<Object>> created = new HashMap<>();

    public EntityCreator(EntityManager em, MockDbProperties props, ValueProvider provider) {
        this.em = em;
        this.props = props;
        this.provider = provider;
    }

    public Map<Class<?>, List<Object>> getCreatedEntities() {
        return created;
    }

    @Override
    public void createAll() {



        if (!props.isEnabled()) return;
        int perEntity = props.resolveCount();

        System.out.println("[mockdb] seeding mode=" + props.getLevel() + " count=" + perEntity);

        Set<EntityType<?>> entityTypes = em.getMetamodel().getEntities();
        List<Class<?>> entities = entityTypes.stream()
                .map(EntityType::getJavaType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Class<?>> ordered = DependencyOrderResolver.orderByDependenciesCycleSafe(entities);

        for (Class<?> cls : ordered) createInstances(cls, perEntity);
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

        for (Field f : ReflectionUtils.getAllFields(cls)) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);

            if (f.isAnnotationPresent(jakarta.persistence.Id.class) &&
                    f.isAnnotationPresent(jakarta.persistence.GeneratedValue.class)) continue;

            if (f.isAnnotationPresent(jakarta.persistence.OneToMany.class) ||
                    f.isAnnotationPresent(jakarta.persistence.ManyToMany.class)) continue;

            if (f.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                    f.isAnnotationPresent(jakarta.persistence.OneToOne.class)) continue;

            try {
                Object val = provider.provideValue(cls, f, index);
                if (val != null) f.set(inst, val);
            } catch (Throwable ignored) {}
        }

        return inst;
    }
}

