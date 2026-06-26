# Getting Started

<cite>
**Referenced Files in This Document**
- [pom.xml](file://pom.xml)
- [application.properties](file://src/main/resources/application.properties)
- [Dockerfile](file://Dockerfile)
- [CampusLifeApplication.java](file://src/main/java/vn/campuslife/CampusLifeApplication.java)
- [SecurityConfig.java](file://src/main/java/vn/campuslife/config/SecurityConfig.java)
- [FirebaseConfig.java](file://src/main/java/vn/campuslife/config/FirebaseConfig.java)
- [AuthController.java](file://src/main/java/vn/campuslife/controller/auth/AuthController.java)
- [TestController.java](file://src/main/java/vn/campuslife/controller/internal/TestController.java)
- [OVERVIEW_APPLICATION.md](file://OVERVIEW_APPLICATION.md)
- [V999__activity_datetime_and_flags.sql](file://db/migration/V999__activity_datetime_and_flags.sql)
- [V1000__unique_activity_registration.sql](file://db/migration/V1000__unique_activity_registration.sql)
- [V1025__create_reminder_schedule_table.sql](file://db/migration/V1025__create_reminder_schedule_table.sql)
- [messages_vi.properties](file://src/main/resources/messages_vi.properties)
- [mvnw.cmd](file://mvnw.cmd)
- [.mvn wrapper properties](file://.mvn/wrapper/maven-wrapper.properties)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Database Configuration](#database-configuration)
5. [Application Properties Setup](#application-properties-setup)
6. [Initial Project Structure Walkthrough](#initial-project-structure-walkthrough)
7. [Build Process Using Maven](#build-process-using-maven)
8. [Running the Application Locally](#running-the-application-locally)
9. [Accessing API Endpoints](#accessing-api-endpoints)
10. [IDE Configuration](#ide-configuration)
11. [First-Time Deployment](#first-time-deployment)
12. [Troubleshooting Common Setup Issues](#troubleshooting-common-setup-issues)
13. [Verification Steps](#verification-steps)
14. [Conclusion](#conclusion)

## Introduction
This guide helps you set up the CampusLife backend application for development. It covers prerequisites, environment configuration, database setup, application properties, building with Maven, running locally, accessing endpoints, IDE setup, deployment, troubleshooting, and verification steps.

## Prerequisites
- Java 21 JDK
- Maven 3.9+ (comes bundled via Maven Wrapper)
- MySQL 8.0+ server
- Git
- IDE with Java 21 support (e.g., IntelliJ IDEA, VS Code)
- Node.js and npm/yarn (for frontend, if you plan to run it locally)

**Section sources**
- [pom.xml:30](file://pom.xml#L30)
- [OVERVIEW_APPLICATION.md:9](file://OVERVIEW_APPLICATION.md#L9)

## Environment Setup
1. Clone the repository and open it in your terminal.
2. Verify Java 21:
   - Run: java -version
3. Verify Maven:
   - Run: mvn -version
   - Alternatively, use the wrapper scripts:
     - Linux/macOS: ./mvnw test
     - Windows: .\mvnw.cmd test

**Section sources**
- [mvnw.cmd:1-150](file://mvnw.cmd#L1-L150)
- [.mvn wrapper properties:17-20](file://.mvn/wrapper/maven-wrapper.properties#L17-L20)

## Database Configuration
- Default local connection:
  - Host: localhost
  - Port: 3306
  - Database: campuslife_db
  - Username: root
  - Password: 123456
- Create the database manually if needed:
  - mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS campuslife_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
- Apply migrations:
  - The application uses Flyway-style SQL migrations located under db/migration/.
  - Example migrations included:
    - [V999__activity_datetime_and_flags.sql](file://db/migration/V999__activity_datetime_and_flags.sql)
    - [V1000__unique_activity_registration.sql](file://db/migration/V1000__unique_activity_registration.sql)
    - [V1025__create_reminder_schedule_table.sql](file://db/migration/V1025__create_reminder_schedule_table.sql)

Notes:
- The application sets timezone to Asia/Ho_Chi_Minh globally and in JPA/Hibernate.
- For production, configure DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD via environment variables.

**Section sources**
- [application.properties:9-12](file://src/main/resources/application.properties#L9-L12)
- [application.properties:20-21](file://src/main/resources/application.properties#L20-L21)
- [CampusLifeApplication.java:12](file://src/main/java/vn/campuslife/CampusLifeApplication.java#L12)
- [V999__activity_datetime_and_flags.sql:1-20](file://db/migration/V999__activity_datetime_and_flags.sql#L1-L20)
- [V1000__unique_activity_registration.sql:1-16](file://db/migration/V1000__unique_activity_registration.sql#L1-L16)
- [V1025__create_reminder_schedule_table.sql:1-22](file://db/migration/V1025__create_reminder_schedule_table.sql#L1-L22)

## Application Properties Setup
Key properties and their purpose:
- Server: port defaults to 8080 (override via PORT env var)
- Database: JDBC URL, username, password, driver
- JPA/Hibernate: ddl-auto, SQL logging, dialect, timezone
- Email: SMTP host/port/auth/starttls, credentials
- Base URLs: app.base-url, app.frontend-url
- File uploads: upload directory, public URL, path prefixes
- CORS: origins, methods, headers, credentials
- JWT: secret and expiration (use JWT_SECRET env var)
- Quartz scheduling: JDBC job store and initialization
- Reminders: before days/hours/minutes and grace hours

Important:
- Do not commit secrets to source control. Use environment variables for DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD.

**Section sources**
- [application.properties:1-86](file://src/main/resources/application.properties#L1-L86)

## Initial Project Structure Walkthrough
High-level layout:
- src/main/java/vn/campuslife/
  - controller/: REST endpoints
  - service/, service/impl/: business logic
  - repository/: Spring Data JPA repositories
  - entity/, enumeration/, model/: domain and DTOs
  - config/: security, CORS, Firebase, JPA auditing, web
  - filter/: JWT authentication filter
  - util/: helpers (JWT, email, Excel, URL)
  - exception/: global exception handler and custom exceptions
- src/main/resources/
  - application.properties, messages_vi.properties
  - db/migration/: SQL migrations
- db/migration/: Flyway-style SQL migration files
- Dockerfile: multi-stage build for Java 21
- pom.xml: Spring Boot 3.5.5, Java 21, dependencies

**Section sources**
- [OVERVIEW_APPLICATION.md:18-41](file://OVERVIEW_APPLICATION.md#L18-L41)
- [pom.xml:1-179](file://pom.xml#L1-L179)

## Build Process Using Maven
- Clean compile and package (skip tests):
  - Linux/macOS: ./mvnw -DskipTests clean package
  - Windows: .\mvnw.cmd -DskipTests clean package
- Run tests:
  - Linux/macOS: ./mvnw test
  - Windows: .\mvnw.cmd test
- Spring Boot plugin builds an executable JAR; packaging phase configured in pom.xml.

**Section sources**
- [pom.xml:144-176](file://pom.xml#L144-L176)
- [OVERVIEW_APPLICATION.md:112-125](file://OVERVIEW_APPLICATION.md#L112-L125)

## Running the Application Locally
- Ensure MySQL is running and database exists.
- Set environment variables (recommended):
  - DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD
- Start the app:
  - From IDE: run CampusLifeApplication.main()
  - From CLI: java -jar target/*.jar (after packaging)
- The app sets default timezone to Asia/Ho_Chi_Minh at startup.

**Section sources**
- [application.properties:4](file://src/main/resources/application.properties#L4)
- [application.properties:10](file://src/main/resources/application.properties#L10-L11)
- [application.properties:65](file://src/main/resources/application.properties#L65-L66)
- [CampusLifeApplication.java:11-16](file://src/main/java/vn/campuslife/CampusLifeApplication.java#L11-L16)

## Accessing API Endpoints
- Base URL: http://localhost:8080 (or PORT env var)
- Public endpoints (no auth required):
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/verify
  - POST /api/auth/forgot-password
  - POST /api/auth/reset-password
  - GET /api/articles/**
  - GET /api/departments/**
  - GET /api/activities/**
  - GET /api/registrations/search
- Test endpoints:
  - GET /api/test/token-info (extracts username, role, expiration from Authorization header)
  - POST /api/test/log-request (logs incoming request data)

Note: Some endpoints require authentication or specific roles. Review SecurityConfig for detailed rules.

**Section sources**
- [AuthController.java:24-69](file://src/main/java/vn/campuslife/controller/auth/AuthController.java#L24-L69)
- [TestController.java:20-62](file://src/main/java/vn/campuslife/controller/internal/TestController.java#L20-L62)
- [SecurityConfig.java:69-193](file://src/main/java/vn/campuslife/config/SecurityConfig.java#L69-L193)

## IDE Configuration
Recommended setup:
- Open the project in IntelliJ IDEA or VS Code with Java 21 support.
- Ensure Maven integration is enabled.
- Configure environment variables in the IDE run configuration:
  - DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD, PORT
- Enable Lombok support in your IDE if not auto-detected.

**Section sources**
- [pom.xml:30](file://pom.xml#L30)
- [pom.xml:73-78](file://pom.xml#L73-L78)

## First-Time Deployment
- Build the application:
  - ./mvnw -DskipTests clean package
- Prepare runtime directories:
  - Create uploads/activities, uploads/email-attachments, uploads/submissions
- Run with environment variables set (as above).
- Docker option:
  - Multi-stage Dockerfile uses Java 21 JRE and copies built JAR.
  - Creates uploads directories at container build time.
  - Exposes port 8080; Render sets PORT via environment variable.

**Section sources**
- [Dockerfile:1-39](file://Dockerfile#L1-L39)
- [application.properties:44-53](file://src/main/resources/application.properties#L44-L53)

## Troubleshooting Common Setup Issues
- Java version mismatch:
  - Ensure JAVA_HOME and PATH point to Java 21.
  - Confirm with java -version and mvn -version.
- Maven Wrapper issues:
  - On Windows, use .\mvnw.cmd; on Unix/macOS, use ./mvnw.
  - If wrapper fails, check .mvn/wrapper/maven-wrapper.properties for distributionUrl.
- Database connectivity:
  - Verify MySQL is running and reachable.
  - Confirm DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD.
  - Ensure campuslife_db exists and is accessible.
- Timezone mismatches:
  - Application sets default timezone to Asia/Ho_Chi_Minh.
  - Ensure OS and JDBC driver timezone align with configuration.
- CORS errors:
  - Adjust spring.web.cors.allowed-origins to match your frontend origin (default: http://localhost:3000).
- JWT secret:
  - Set JWT_SECRET environment variable to a strong random value.
- Email configuration:
  - Set MAIL_USERNAME and MAIL_PASSWORD for SMTP; ensure STARTTLS is enabled.
- File upload directory:
  - Ensure uploads directory exists and is writable; Dockerfile pre-creates subdirectories.

**Section sources**
- [.mvn wrapper properties:17-20](file://.mvn/wrapper/maven-wrapper.properties#L17-L20)
- [application.properties:4](file://src/main/resources/application.properties#L4)
- [application.properties:9-12](file://src/main/resources/application.properties#L9-L12)
- [application.properties:20-21](file://src/main/resources/application.properties#L20-L21)
- [application.properties:56-60](file://src/main/resources/application.properties#L56-L60)
- [application.properties:65](file://src/main/resources/application.properties#L65)
- [application.properties:28-33](file://src/main/resources/application.properties#L28-L33)
- [application.properties:44-53](file://src/main/resources/application.properties#L44-L53)
- [Dockerfile:20-25](file://Dockerfile#L20-L25)

## Verification Steps
- Build and test:
  - ./mvnw test
- Start the app and confirm logs show successful startup and timezone initialization.
- Test authentication endpoints:
  - POST /api/auth/register
  - POST /api/auth/login
- Validate JWT parsing:
  - GET /api/test/token-info with a valid Bearer token
- Check CORS:
  - From frontend origin, verify preflight OPTIONS requests succeed.
- Verify uploads:
  - POST /api/upload/** and access uploaded files via app.upload.public-url.
- Confirm Quartz scheduling:
  - Check reminder schedule table creation and indexes if reminders are used.

**Section sources**
- [OVERVIEW_APPLICATION.md:112-125](file://OVERVIEW_APPLICATION.md#L112-L125)
- [AuthController.java:24-69](file://src/main/java/vn/campuslife/controller/auth/AuthController.java#L24-L69)
- [TestController.java:20-37](file://src/main/java/vn/campuslife/controller/internal/TestController.java#L20-L37)
- [application.properties:56-60](file://src/main/resources/application.properties#L56-L60)
- [application.properties:44-53](file://src/main/resources/application.properties#L44-L53)
- [V1025__create_reminder_schedule_table.sql:1-22](file://db/migration/V1025__create_reminder_schedule_table.sql#L1-L22)

## Conclusion
You now have the fundamentals to develop, run, and deploy the CampusLife backend. Keep environment variables secure, apply migrations carefully, and leverage the provided endpoints and configurations for smooth development and deployment.