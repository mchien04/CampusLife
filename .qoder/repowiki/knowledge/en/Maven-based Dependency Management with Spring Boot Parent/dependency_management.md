## System Overview
The repository uses **Apache Maven** as its primary dependency management system, leveraging the **Spring Boot Starter Parent** for standardized dependency versions and build configuration. The project targets **Java 21** and utilizes the **Maven Wrapper** (`mvnw`) to ensure consistent build environments across different machines.

## Key Configuration Files
- **`pom.xml`**: The central manifest defining all project dependencies, plugins, and build properties. It inherits from `org.springframework.boot:spring-boot-starter-parent:3.5.5`, which manages versions for most Spring-related libraries.
- **`.mvn/wrapper/maven-wrapper.properties`**: Configures the Maven Wrapper to use **Apache Maven 3.9.11**, ensuring that the exact same version of Maven is used for builds regardless of the local environment.
- **`Dockerfile`**: Implements a multi-stage build process. The build stage uses `maven:3.9.6-eclipse-temurin-21` to resolve dependencies (`mvn dependency:go-offline`) and package the application. This isolates dependency resolution from the runtime image (`eclipse-temurin:21-jre`).

## Dependency Strategy
1.  **Spring Boot BOM**: By extending `spring-boot-starter-parent`, the project avoids specifying versions for common starters (e.g., `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`). This reduces version conflicts and simplifies upgrades.
2.  **Explicit Versioning**: Third-party libraries not managed by Spring Boot are explicitly versioned in `pom.xml`:
    -   **JWT**: `io.jsonwebtoken:jjwt-*` (v0.11.5)
    -   **Excel/PDF**: `org.apache.poi:poi-ooxml` (v5.2.5), `com.github.librepdf:openpdf` (v1.3.39)
    -   **Firebase**: `com.google.firebase:firebase-admin` (v9.2.0)
    -   **Utilities**: `org.jsoup:jsoup` (v1.17.2), `org.apache.commons:commons-lang3` (v3.14.0)
    -   **Lombok**: `org.projectlombok:lombok` (v1.18.34), configured both as a dependency and an annotation processor path.
3.  **Repositories**: The project includes the **Spring Milestones** repository (`https://repo.spring.io/milestone`) in addition to the default Maven Central, allowing access to pre-release Spring artifacts if needed.
4.  **No Lockfile**: Maven does not use a lockfile (like `package-lock.json` or `go.sum`). Reproducibility is achieved through the Maven Wrapper and explicit version declarations in `pom.xml`.

## CI/CD Integration
-   **GitHub Actions (`.github/workflows/ci.yml`)**: Uses `actions/setup-java@v4` with `cache: maven` to cache Maven dependencies between runs, speeding up builds. It executes `./mvnw -B test` and `./mvnw -B -DskipTests clean package` using the wrapper script.
-   **Docker Build**: The `Dockerfile` optimizes layer caching by copying `pom.xml` first and running `mvn dependency:go-offline` before copying source code. This ensures that dependency downloads are cached unless `pom.xml` changes.

## Developer Conventions
-   **Use the Wrapper**: Always use `./mvnw` (or `mvnw.cmd` on Windows) instead of a globally installed `mvn` to guarantee build consistency.
-   **Dependency Updates**: To update dependencies, modify versions in `pom.xml`. For Spring-managed dependencies, consider upgrading the `spring-boot-starter-parent` version in the `<parent>` block.
-   **Offline Resolution**: When working in constrained network environments, run `./mvnw dependency:go-offline` to pre-fetch all required artifacts.