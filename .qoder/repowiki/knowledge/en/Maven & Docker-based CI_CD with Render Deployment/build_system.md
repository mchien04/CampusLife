## Build System Overview

The CampusLife platform uses a standard **Java 21 / Spring Boot 3.5.5** stack managed by **Apache Maven**. The build and deployment pipeline is automated via **GitHub Actions** and targets **Render** for hosting, using a multi-stage **Docker** build.

### 1. Core Build Tool: Apache Maven
- **Management**: Uses the **Maven Wrapper** (`mvnw`) to ensure consistent build environments across different machines and CI runners. The wrapper is configured to use Maven 3.9.11.
- **Configuration**: Defined in `pom.xml`.
  - **Parent**: `spring-boot-starter-parent:3.5.5`.
  - **Compiler**: Configured for Java 21 release with Lombok annotation processing.
  - **Packaging**: Produces an executable JAR via `spring-boot-maven-plugin`.
  - **Dependencies**: Includes Spring Data JPA, Security, Web, Validation, MySQL Connector, H2 (test), Lombok, JWT (jjwt), Apache POI, OpenPDF, Firebase Admin, JSoup, and Quartz.

### 2. Containerization: Docker
- **Strategy**: Multi-stage build to optimize image size.
  - **Build Stage**: Uses `maven:3.9.6-eclipse-temurin-21` to resolve dependencies and package the application (`mvn -DskipTests=true clean package`).
  - **Run Stage**: Uses `eclipse-temurin:21-jre` (JRE only) to run the resulting JAR.
- **File Handling**: The `Dockerfile` pre-creates directories for uploads (`uploads/activities`, `uploads/email-attachments`, `uploads/submissions`) to ensure runtime readiness.
- **Port**: Exposes port `8080`.

### 3. CI/CD Pipeline: GitHub Actions
The project employs two distinct workflows located in `.github/workflows/`:

#### Continuous Integration (`ci.yml`)
- **Triggers**: Push or Pull Request to `main` or `develop` branches.
- **Steps**:
  1. Checkout code.
  2. Setup Java 21 (Temurin) with Maven caching.
  3. **Test**: Runs `./mvnw -B test` to execute unit tests.
  4. **Package**: Runs `./mvnw -B -DskipTests clean package` to verify build success.

#### Continuous Deployment (`cd.yml`)
- **Triggers**: Push to `main` branch or manual dispatch.
- **Steps**:
  1. **Test**: Re-runs tests to ensure stability before deployment.
  2. **Deploy**: Triggers a Render deployment hook via `curl` using the `RENDER_DEPLOY_HOOK_URL` secret.
  - **Note**: Render is configured to build the Docker image from the repository's `Dockerfile` upon receiving the hook.

### 4. Database Migrations
- **Tool**: Flyway (implied by `db/migration` directory structure and `V*__*.sql` naming convention).
- **Location**: `src/main/resources/db/migration` and `db/migration`.
- **Convention**: Versioned scripts (e.g., `V1000__unique_activity_registration.sql`) are applied automatically on startup.

### 5. Developer Rules & Conventions
- **Build Command**: Always use `./mvnw` instead of system-installed `mvn` to guarantee version consistency.
- **Testing**: Tests are mandatory in CI. Local development should ensure `./mvnw test` passes before pushing.
- **Deployment**: Deployment is triggered automatically on merge to `main`. Do not manually deploy to Render unless necessary; rely on the CD pipeline.
- **Secrets Management**: Sensitive configuration (DB credentials, JWT secrets, Render hooks) must be stored in GitHub Secrets or Render Environment Variables, never in code.
- **Docker Hygiene**: The `Dockerfile` skips tests during the image build (`-DskipTests=true`) because testing is handled in the CI stage prior to deployment triggering.