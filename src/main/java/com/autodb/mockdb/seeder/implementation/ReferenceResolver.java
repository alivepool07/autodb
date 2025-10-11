package com.autodb.mockdb.seeder.implementation;
import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.seeder.ReferenceResolverService;
import jakarta.persistence.EntityManager;
import com.autodb.mockdb.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ReferenceResolver implements ReferenceResolverService {

    private final EntityManager em;
    private final MockDbProperties props;
    private final Map<Class<?>, List<Object>> created;
    private final Random rnd = new Random();

    public ReferenceResolver(EntityManager em, MockDbProperties props, Map<Class<?>, List<Object>> created) {
        this.em = em;
        this.props = props;
        this.created = created;
    }

    @Override
    public void fixMissingReferences() throws IllegalAccessException {
        for (Map.Entry<Class<?>, List<Object>> e : created.entrySet()) {
            Class<?> cls = e.getKey();
            List<Object> instances = e.getValue();
            for (Object inst : instances) {
                for (Field f : ReflectionUtils.getAllFields(cls)) {
                    if (f.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                            f.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {

                        f.setAccessible(true);
                        if (f.get(inst) == null) {
                            Class<?> target = f.getType();
                            List<Object> pool = created.get(target);
                            if (pool == null || pool.isEmpty()) {
                                new EntityCreator(em, props, null).createAll();
                                pool = created.get(target);
                            }
                            if (!pool.isEmpty()) f.set(inst, pool.get(rnd.nextInt(pool.size())));
                        }
                    }
                }
            }
        }
        em.flush();

    }
}
