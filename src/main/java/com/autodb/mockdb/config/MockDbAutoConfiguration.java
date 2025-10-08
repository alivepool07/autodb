package com.autodb.mockdb.config;

import com.autodb.mockdb.provider.FakerValueProvider;
import com.autodb.mockdb.provider.RandomValueProvider;
import com.autodb.mockdb.provider.ValueProvider;
import com.autodb.mockdb.seeder.MockDbSeeder;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfiguration
@EnableConfigurationProperties(MockDbProperties.class)
public class MockDbAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mockdb", name = "use-faker", havingValue = "true")
    public ValueProvider fakerValueProvider() {
        return new FakerValueProvider();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mockdb", name = "use-faker", havingValue = "false", matchIfMissing = true)
    public ValueProvider randomValueProvider() {
        return new RandomValueProvider();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mockdb", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner mockDbRunner(EntityManager entityManager,
                                          MockDbProperties props,
                                          ValueProvider provider,
                                          PlatformTransactionManager txMgr) {
        return args -> {
            TransactionTemplate tx = new TransactionTemplate(txMgr);
            tx.execute(status -> {
                MockDbSeeder seeder = new MockDbSeeder(entityManager, props, provider);
                try {
                    seeder.seedAll();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        };
    }
}
