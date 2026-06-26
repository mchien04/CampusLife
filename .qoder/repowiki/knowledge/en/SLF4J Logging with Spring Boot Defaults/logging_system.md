## Overview

The CampusLife application uses **SLF4J** (Simple Logging Facade for Java) as its logging abstraction, backed by **Logback** (the default implementation provided by `spring-boot-starter-web`). No custom log configuration files (`logback.xml`, `logback-spring.xml`) exist — the application relies entirely on Spring Boot's default logging setup configured through `application.properties`.

## Framework and Dependencies

- **Logging API**: SLF4J (`org.slf4j.Logger`, `org.slf4j.LoggerFactory`)
- **Implementation**: Logback (transitively included via `spring-boot-starter-web` in Spring Boot 3.5.5)
- **No explicit logging dependency** declared in `pom.xml` — inherited from the Spring Boot parent BOM

## Configuration

Logging levels are configured in `src/main/resources/application.properties`:

```properties
# Hibernate SQL debugging (development-focused)
logging.level.org.hibernate.SQL=${LOG_SQL:DEBUG}
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=${LOG_SQL_BINDER:TRACE}
```

Key observations:
- SQL statement logging is enabled at `DEBUG` level by default (controlled via `LOG_SQL` env var)
- SQL parameter binding is logged at `TRACE` level (controlled via `LOG_SQL_BINDER` env var)
- No file-based log output is configured — all logs go to **console/stdout**
- No log rotation, retention, or structured JSON output is configured
- No environment-specific log profiles (e.g., separate dev/prod configs)

## Logger Initialization Pattern

Across the codebase, loggers follow a consistent initialization pattern:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SomeClass {
    private static final Logger logger = LoggerFactory.getLogger(SomeClass.class);
}
```

This pattern appears in:
- **Controllers**: `ActivityController`, `ActivityRegistrationController`, `EmailController`, `StatisticsController`, `MiniGameController`, `ActivitySeriesController`, `ActivityPhotoController`
- **Filters**: `JwtAuthenticationFilter`
- **Exception handlers**: `GlobalExceptionHandler`
- **Service implementations**: `ActivityServiceImpl`, and likely others (though grep did not return service-layer matches, manual inspection confirms the pattern)

## Log Level Usage Conventions

The codebase demonstrates a clear hierarchy of log level usage:

| Level | Usage Pattern | Example |
|-------|--------------|---------|
| `logger.info()` | High-level operational events, request tracing blocks, significant state changes | `"=== CREATE ACTIVITY REQUEST ==="`, authentication success messages |
| `logger.debug()` | Detailed diagnostic information, internal state inspection, auto-generated values | Check-in code generation, score rule persistence, user detail loading |
| `logger.warn()` | Recoverable issues, validation failures, expected-but-notable conditions | Token extraction failures, user not found, invalid requests |
| `logger.error()` | Exception handling, unexpected failures, always includes exception stack trace | All catch blocks in controllers and global handler |

### Notable Patterns

1. **Request tracing blocks**: Controllers use decorative `info` logs with separator lines to mark request boundaries:
   ```java
   logger.info("=== CREATE ACTIVITY REQUEST ===");
   logger.info("Name: {}", request.getName());
   logger.info("Type: {}", request.getType());
   logger.info("===============================");
   ```

2. **Error logging always includes stack traces**: The third argument pattern `logger.error("message: {}", e.getMessage(), e)` ensures full exception context is captured.

3. **Security-sensitive filtering**: `JwtAuthenticationFilter` uses extensive `debug` logging for token extraction, validation, and authentication flow — appropriate for security troubleshooting without exposing sensitive data in production logs.

4. **Global exception handler**: `GlobalExceptionHandler` logs only two categories at error level:
   - `DataIntegrityViolationException` — database constraint violations
   - Generic `Exception` — unhandled exceptions (catch-all)
   Custom business exceptions (`BadRequestException`, `ResourceNotFoundException`, etc.) are handled without logging, assuming they are already logged upstream or are expected control flow.

## Architecture Observations

- **No structured logging**: Log messages use simple string interpolation with `{}` placeholders. No MDC (Mapped Diagnostic Context), no correlation IDs, no JSON formatting.
- **No centralized logging utility**: Each class declares its own logger instance. No shared logging helper or aspect-based logging.
- **No audit logging via the logging framework**: The `AuditLog` entity exists for persistent audit trails, but this is separate from the logging system.
- **Development-oriented defaults**: SQL logging at DEBUG/TRACE level is suitable for local development but should be disabled or reduced in production.

## Rules for Developers

1. **Always use SLF4J**: Import `org.slf4j.Logger` and `org.slf4j.LoggerFactory`. Never use `System.out.println` or `java.util.logging` directly.

2. **Logger declaration**: Use `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);` at the class level.

3. **Use parameterized messages**: Always use `{}` placeholders instead of string concatenation:
   ```java
   // Correct
   logger.error("Failed to process activity {}: {}", id, e.getMessage(), e);
   // Incorrect
   logger.error("Failed to process activity " + id + ": " + e.getMessage());
   ```

4. **Include exceptions in error logs**: When logging caught exceptions, pass the throwable as the last argument to preserve stack traces.

5. **Respect log level semantics**:
   - `error`: Unexpected failures requiring investigation
   - `warn`: Recoverable issues or degraded behavior
   - `info`: Significant business events (use sparingly to avoid noise)
   - `debug`: Detailed diagnostic info (safe to leave in production if level is disabled)

6. **Avoid logging sensitive data**: Do not log passwords, tokens, PII, or other sensitive information. The `JwtAuthenticationFilter` demonstrates this by logging usernames but not token values.

7. **Use environment variables for log configuration**: Follow the existing pattern of `${ENV_VAR:default}` in `application.properties` for any new logging settings.

## Gaps and Recommendations

- No production-ready log configuration (no file output, no rotation, no structured format)
- No correlation ID or request ID tracking for distributed tracing
- No log level differentiation between environments (dev vs. prod)
- Consider adding `logback-spring.xml` for environment-specific configurations
- Consider integrating a structured logging encoder (e.g., Logstash encoder) for better log aggregation compatibility