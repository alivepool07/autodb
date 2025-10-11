# ⚙️ AutoDB — Smart Mock Data Generator for Java & Spring Boot

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-red?logo=openjdk" alt="Java Version" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/github/stars/alivepool07/autodb?style=social" alt="GitHub Stars" />
</p>

<p align="center">
  <strong>Automatically generate realistic mock data for your Spring Boot entities — no SQL or JSON seeders required.</strong><br>
  <em>Powered by DataFaker.</em>
</p>

---

## 📑 Table of Contents

1. [Overview](#-overview)
2. [Installation & Setup](#-installation--setup)
   - [Download the JAR](#1️⃣-download-the-jar)
   - [Add AutoDB to Your Project](#2️⃣-add-autodb-to-your-project)

3. [How It Works  (I swear i'll add this later)](#-how-it-works) 
7. [Contributors](#-contributors)
8. [Coming Soon — AutoDB 2.0](#-coming-soon--autodb-20)
9. [Support the Project](#-support-the-project)

---

## 💡 Overview

**AutoDB** is a lightweight tool that *automatically generates realistic mock data* for your database entities — perfect for testing APIs, repositories, and services when your DB is empty or missing data.

It uses **DataFaker (JavaFaker)** under the hood (when enabled) to produce authentic names, emails, locations, prices, and more — while also providing a fallback **RandomValueProvider** for fast, dependency-free generation.

> No more writing SQL inserts or JSON seeders — just plug AutoDB into your Spring Boot project, and your tables come to life with realistic test data. 🚀

---

## 📦 Installation & Setup

### 1️⃣ Download the JAR

You can either:

- **Build from source**
```bash
  mvn clean install -DskipTests
```
The JAR will be available under:

```bash
target/autodb-<version>.jar
```
Or download the prebuilt JAR from this repository.

### 2️⃣ Add AutoDB to Your Project
If you’re using Maven, install the JAR to your local repository:

```xml
<dependency>
  <groupId>com.autodb</groupId>
  <artifactId>autodb-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3️⃣ Configure AutoDB
In your project’s src/main/resources/system.properties, add:

```bash
# Enable AutoDB
mockdb.enabled=true

# Use DataFaker for more authentic fake data
mockdb.use-faker=true

# Default number of records to generate per entity
mockdb.level=LOW  #you can use LOW , MEDIUM and HIGH
```




AutoDB automatically detects your entities and configures the DB.


🤝 Contributors

@Sarthak 

@Sankalp

@Sagar

# 🔮 Coming Soon — AutoDB 2.0

We’re working on a more advanced version featuring:
Stay tuned — the next version will blur the line between mock and real data. 😉

### ⭐ Support the Project
If you find AutoDB helpful, please give it a star ⭐ on GitHub!

Your support motivates us to make it even smarter, faster, and more powerful.

<br>
<br>




---







