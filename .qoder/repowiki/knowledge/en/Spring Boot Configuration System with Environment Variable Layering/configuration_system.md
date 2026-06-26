# Configuration System Overview

This is a **Spring Boot 3.5.5** application using a conventional Spring Boot configuration approach with environment variable layering, `application.properties`, and Java-based `@Configuration` classes.

## Core Configuration Approach

### Primary Configuration File
- **`src/main/resources/application.properties`**: Single monolithic properties file containing all runtime configuration
- Uses `${ENV_VAR:default}` syntax for environment variable injection with sensible defaults for local development
- No YAML files or profile-specific property files (e.g., no `application-dev.yml`, `application-prod.yml`)

### Test Configuration
- **`src/test/resources/application-test.properties`**: Separate test configuration using H2 in-memory database instead of MySQL
- Disables Firebase (`firebase.enabled=false`) and Quartz auto-startup for isolated testing
- Uses `create-drop` DDL strategy vs `update` in production

## Configuration Categories

### 1. Infrastructure & Database
- **Database URL**: Configured via `DATABASE_URL` env var with MySQL connection string fallback to localhost
- **Timezone**: Hardcoded to `Asia/Ho_Chi_Minh` across JPA, Jackson serialization, and JDBC
- **Flyway migrations**: Located in `db/migration/` with versioned SQL scripts (V1000-V1027+)

### 2. Security & Authentication
- **JWT**: Secret and expiration injected via `JWT_SECRET` and `JWT_EXPIRATION` env vars
- **Security rules**: Defined programmatically in `SecurityConfig.java` with role-based access control (STUDENT, ADMIN, MANAGER)
- **CORS**: Configurable origins via `CORS_ALLOWED_ORIGINS` env var

### 3. External Services
- **Firebase Admin SDK**: Initialized from `firebase-admin.json` classpath resource; can be disabled via `firebase.enabled` flag
- **Email (SMTP)**: Gmail SMTP configured via `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- **Gemini AI API**: API key and model configured via `gemini.api-key` and `gemini.model` properties

### 4. File Uploads
- **Structured upload paths**: Managed via `UploadProperties` (@ConfigurationProperties with prefix `app.upload`)
- Subdirectories: `activities`, `submissions`, `email-attachments`
- Public URL prefix configurable via `APP_BASE_URL`

### 5. Scheduling & Reminders
- **Quartz Scheduler**: JDBC-backed job store with MySQL delegate
- **Reminder timing**: Granular configuration for event/task reminders (days, hours, minutes) via `app.reminder.*` properties
- Custom `SchedulerFactoryBeanCustomizer` enables Spring bean autowiring in Quartz jobs

## Key Configuration Classes

| File | Purpose |
|------|---------|
| `config/SecurityConfig.java` | JWT authentication, role-based authorization rules, CORS integration |
| `config/CorsConfig.java` | Dual CORS setup (WebMvcConfigurer + CorsConfigurationSource bean) |
| `config/FirebaseConfig.java` | Firebase Admin SDK initialization from classpath JSON credential |
| `config/UploadProperties.java` | Type-safe upload directory configuration via @ConfigurationProperties |
| `config/ReminderSchedulingConfig.java` | Quartz scheduler customization for Spring bean injection |
| `config/JpaConfig.java` | JPA/Hibernate additional configuration |
| `config/SchedulingConfig.java` | General scheduling enablement |
| `config/RestTemplateConfig.java` | HTTP client bean configuration |
| `config/WebConfig.java` | Web MVC configuration |

## Environment Variable Convention

All sensitive or environment-specific values follow the pattern:
```
${ENV_VAR_NAME:development_default}
```

Key environment variables expected in production:
- `PORT` — Server port (Render/cloud platform convention)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET` — Must be a strong random string in production
- `MAIL_PASSWORD` — SMTP credentials
- `CORS_ALLOWED_ORIGINS` — Frontend origin(s)
- `APP_BASE_URL`, `FRONTEND_URL` — Application and frontend URLs
- `UPLOAD_DIR`, `UPLOAD_ACTIVITY_PHOTOS_DIR`, `UPLOAD_SUBMISSIONS_DIR` — File storage paths
- `SHOW_SQL`, `LOG_SQL`, `LOG_SQL_BINDER` — Debug logging toggles
- `SPRING_QUARTZ_JDBC_INITIALIZE_SCHEMA` — Quartz schema initialization mode

## Deployment Configuration

### Docker
- Multi-stage build: Maven build stage → JRE runtime stage
- Pre-creates upload directories (`uploads/activities`, `uploads/email-attachments`, `uploads/submissions`)
- Expects `PORT` environment variable at runtime (Render convention)
- No volume mount by default (commented out VOLUME instruction for Render Disk)

### CI/CD (GitHub Actions)
- Runs `mvn test` and `mvn package` on PR/push to `main` and `develop`
- No environment-specific configuration in CI — relies on test profile defaults

## Developer Conventions

1. **Add new config properties** to `application.properties` with `${ENV_VAR:default}` syntax
2. **Use @Value** for simple property injection in services/utilities
3. **Use @ConfigurationProperties** (like `UploadProperties`) for grouped, type-safe configuration
4. **Sensitive values** (secrets, API keys) must use environment variables — never hardcode
5. **Test overrides** go in `application-test.properties`
6. **No YAML** — stick to `.properties` format for consistency
7. **Timezone is hardcoded** to `Asia/Ho_Chi_Minh` — changing requires updates across multiple config locations
8. **Firebase credentials** stored as `firebase-admin.json` in classpath — ensure this file is in `.gitignore` for security
