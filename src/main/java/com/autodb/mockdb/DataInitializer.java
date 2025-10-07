package com.autodb.mockdb;

import com.autodb.mockdb.Entity.Product;
import com.autodb.mockdb.Entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        // Check if DB is empty before inserting dummy data
        List<User> users = entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
        List<Product> products = entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        if (users.isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                User u = new User();
                u.setName("User " + i);
                u.setEmail("user" + i + "@example.com");
                entityManager.persist(u);
            }
        }

        if (products.isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                Product p = new Product();
                p.setName("Product " + i);
                p.setPrice(BigDecimal.valueOf(10.5 + i));
                entityManager.persist(p);
            }
        }

        // Query again after insertion
        users = entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
        products = entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        System.out.println("=== Users in DB ===");
        users.forEach(u -> System.out.println(u.getId() + " | " + u.getName() + " | " + u.getEmail()));

        System.out.println("=== Products in DB ===");
        products.forEach(p -> System.out.println(p.getId() + " | " + p.getName() + " | " + p.getPrice()));
    }
}
