package com.autodb.mockdb.initializer;

import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.provider.ValueProvider;
import com.autodb.mockdb.seeder.Seeder;
import com.autodb.mockdb.seeder.implementation.MockDbSeeder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MockDbAutoInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final MockDbProperties properties;
    private final ValueProvider valueProvider;

    public MockDbAutoInitializer(MockDbProperties properties, ValueProvider valueProvider) {
        this.properties = properties;
        this.valueProvider = valueProvider;
    }

    @Override
    @Transactional
    public void run(String... args) throws IllegalAccessException {
        System.out.println("[mockdb] Auto initializer running...");
        Seeder seeder = new MockDbSeeder(entityManager, properties, valueProvider);
        seeder.seedAll();
    }
}
