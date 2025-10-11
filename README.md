# MockDB Auto Seeder for Spring Boot

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JPA](https://img.shields.io/badge/JPA-Hibernate-blue.svg)](https://hibernate.org/)

A zero-configuration library that automatically populates your JPA database with mock data when your Spring Boot application starts up. Perfect for development, testing, and demo environments.

## 📋 Table of Contents

- [Why MockDB Auto Seeder?](#why-mockdb-auto-seeder)
- [Core Concepts](#core-concepts)
- [Features](#features)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [How It Works](#how-it-works)
- [Best Practices](#best-practices)
- [License](#license)

## 🎯 Why MockDB Auto Seeder?

In modern application development, especially during early stages, developers need a populated database to:
- Test functionality with realistic data scenarios
- Build and validate UI components
- Demonstrate progress to stakeholders
- Perform integration testing

Manually creating this data is **tedious, repetitive, and time-consuming**. MockDB Auto Seeder eliminates this friction by automatically generating and persisting mock data based on your JPA entity definitions.

## 🧠 Core Concepts

### The Problem Space

When building applications with relational databases, entities rarely exist in isolation. They form complex webs of relationships:

```
Category ──< Product >── Order >── User
              │
              └── Review
```

Creating mock data manually requires:
1. Understanding the dependency graph
2. Respecting foreign key constraints
3. Maintaining referential integrity
4. Generating realistic values
5. Handling circular dependencies

MockDB Auto Seeder automates all of this.

### How It Solves The Problem

The library leverages **JPA metamodel introspection** to:
- Discover all entities in your application context
- Analyze their relationships and build a dependency graph
- Determine the correct insertion order using **topological sorting**
- Generate contextually appropriate data
- Persist entities while maintaining referential integrity

## ✨ Features

### 🚀 Zero-Configuration
Just add the dependency and it works out of the box. No XML files, no manual bean configuration.

### 🧠 Smart Entity Discovery
Automatically finds all `@Entity` annotated classes from the JPA metamodel using the `EntityManager`.

### 🔄 Dependency-Aware Seeding
Correctly orders entity creation based on relationships:
- `@ManyToOne` and `@OneToOne` dependencies are resolved first
- Handles cyclical dependencies gracefully with detection algorithms
- Uses topological sorting to ensure parents are created before children

### 🔗 Full Relationship Support
Populates all JPA relationship types:
- **@ManyToOne**: Links child entities to existing parents
- **@OneToOne**: Creates bidirectional one-to-one associations
- **@OneToMany**: Populates collections on the parent side
- **@ManyToMany**: Generates random cross-entity associations

### ✨ Two Data Providers

#### 1. **Faker Provider** (Realistic Data)
Integrates with the powerful [java-faker](https://github.com/DiUS/java-faker) library:
```java
firstName -> "John"
email -> "john.doe@example.com"
address -> "123 Main Street, Springfield"
phoneNumber -> "+1-555-0123"
```

#### 2. **Random Provider** (Simple Data)
Lightweight, dependency-free generator:
```java
firstName -> "random_string_a7b3"
email -> "random_a7b3@example.com"
age -> 42
```

### 🔧 Highly Configurable
Control behavior through standard Spring Boot properties:
- Enable/disable globally
- Choose data generation strategy
- Set data volume levels

### 📊 Configurable Data Volume
Three intuitive levels:
- **LOW**: 100 records per entity
- **MID**: 500 records per entity
- **HIGH**: 1000 records per entity

## 🚀 Quick Start

### 1. Add Dependencies

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- MockDB Auto Seeder -->
    <dependency>
        <groupId>com.autodb</groupId>
        <artifactId>autodb</artifactId>
        <version>1.0.0</version>
    </dependency>

```

### 2. Configure Properties

Create or update `src/main/resources/application.properties`:

```properties
# Enable the seeder (default: true)
mockdb.enabled=true

# Use Faker for realistic data (default: false)
mockdb.use-faker=true

# Set data volume: LOW (100), MID (500), HIGH (1000)
mockdb.level=LOW
```

### 3. Run Your Application

```bash
mvn spring-boot:run
```

That's it! Check your console for seeding output:

```
[mockdb] seeding mode=LOW count=100
[mockdb] discovered 5 entities
[mockdb] processing: Category
[mockdb] processing: Product
[mockdb] processing: User
[mockdb] processing: Order
[mockdb] seeding completed; persisted counts:
  Category: 100
  Product: 100
  User: 100
  Order: 100
```

## ⚙️ Configuration

### Configuration Properties

| Property | Description | Default | Options |
|----------|-------------|---------|---------|
| `mockdb.enabled` | Master switch to enable/disable the seeder | `true` | `true`, `false` |
| `mockdb.level` | Volume of data to generate per entity | `LOW` | `LOW`, `MID`, `HIGH` |
| `mockdb.use-faker` | Use Faker for realistic data vs simple random | `false` | `true`, `false` |

### YAML Configuration

If you prefer YAML (`application.yml`):

```yaml
mockdb:
  enabled: true
  level: MID      # 500 records per entity
  use-faker: true # Realistic data
```

### Environment-Specific Configuration

**Development** (`application-dev.properties`):
```properties
mockdb.enabled=true
mockdb.level=MID
mockdb.use-faker=true
```

**Production** (`application-prod.properties`):
```properties
# ALWAYS disable in production!
mockdb.enabled=false
```

**Testing** (`application-test.properties`):
```properties
mockdb.enabled=true
mockdb.level=LOW
mockdb.use-faker=false  # Faster without faker
```

Activate profiles:
```bash
# Development
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod
```

## 🔍 How It Works

Understanding the internals helps you appreciate the complexity MockDB handles for you.

### 1. Auto-Configuration

The library uses **Spring Boot's auto-configuration** mechanism:
```java
@ConditionalOnProperty(name = "mockdb.enabled", havingValue = "true")
public class MockDBAutoConfiguration {
    @Bean
    public ApplicationRunner databaseSeeder(EntityManager em, ...) {
        return args -> seedDatabase(em);
    }
}
```

When `mockdb.enabled=true`, Spring automatically registers an `ApplicationRunner` bean that executes after the application context is fully initialized.

### 2. Entity Discovery

Uses the **JPA Metamodel** to find all entities:
```java
Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
```

This approach is superior to classpath scanning because it:
- Only finds entities actually registered with JPA
- Respects your JPA configuration
- Works with all JPA providers (Hibernate, EclipseLink, etc.)

### 3. Dependency Graph Construction

Analyzes `@ManyToOne` and `@OneToOne` annotations to build a directed acyclic graph (DAG):

```
Graph:
  Product -> Category (ManyToOne)
  Order -> User (ManyToOne)
  Order -> Product (ManyToOne)
  
Dependency Resolution:
  1. Category (no dependencies)
  2. User (no dependencies)
  3. Product (depends on Category)
  4. Order (depends on User, Product)
```

### 4. Topological Sorting

Uses **Kahn's algorithm** or **depth-first search** to determine insertion order:
- Ensures parent entities are created before children
- Detects circular dependencies
- Groups circular entities for special handling

### 5. Instance Creation & Population

For each entity type:
```java
for (int i = 0; i < count; i++) {
    Object instance = entityClass.getDeclaredConstructor().newInstance();
    
    // Populate simple fields
    for (Field field : fields) {
        field.set(instance, valueProvider.generateValue(field));
    }
    
    // Link to existing entities for @ManyToOne
    for (ManyToOne relationship : manyToOneFields) {
        Object parent = randomlySelectFrom(existingParents);
        field.set(instance, parent);
    }
    
    entityManager.persist(instance);
}
```

### 6. Relationship Resolution

Second pass to populate collections and many-to-many:
```java
// @OneToMany
for (Order order : orders) {
    order.getCustomer().getOrders().add(order);
}

// @ManyToMany
for (Student student : students) {
    List<Course> randomCourses = randomSubset(courses);
    student.getCourses().addAll(randomCourses);
}
```

### 7. Transactional Persistence

All operations occur within a **single transaction** to ensure:
- Atomic data generation (all or nothing)
- Referential integrity
- Performance (batch inserts)

## 📚 Best Practices

### ✅ DO

- **Use profiles**: Enable for `dev`/`test`, disable for `prod`
- **Start with LOW**: Test with small datasets first
- **Use Faker in dev**: More realistic UI/UX testing
- **Commit your config**: Check `application-*.properties` into version control

### ❌ DON'T

- **Never enable in production**: Set `mockdb.enabled=false` for prod profiles
- **Don't rely on seeded data for tests**: Use `@Sql` scripts or test containers for deterministic test data
- **Don't seed sensitive entities**: Exclude entities like `User` credentials using custom configuration if needed

## 📄 License

This project is licensed under the MIT License - see below for details:

```
MIT License

Copyright (c) 2025 MockDB Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

If you encounter any issues or have questions, please file an issue on the GitHub repository.

---

**Made with ❤️ for the Spring Boot community**
