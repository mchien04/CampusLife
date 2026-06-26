The CampusLife backend employs a centralized, annotation-driven error handling strategy typical of Spring Boot applications, utilizing a `@RestControllerAdvice` to intercept and transform exceptions into consistent HTTP responses.

### 1. Core Architecture: Global Exception Handler
The primary mechanism is the `GlobalExceptionHandler` class, which acts as a central interceptor for all uncaught exceptions thrown within the controller layer. It maps specific exception types to appropriate HTTP status codes and wraps them in a standardized `Response` object.

- **Consistent Response Format**: All errors are returned using the `vn.campuslife.model.Response` class, which contains three fields:
  - `status` (boolean): Always `false` for errors.
  - `message` (String): A human-readable description of the error.
  - `body` (Object): Optional additional data (used primarily by `OverBudgetException`).

- **HTTP Status Mapping**:
  - `400 Bad Request`: Used for `BadRequestException`, `FeatureNotEnabledException`, validation failures (`MethodArgumentNotValidException`), and malformed JSON (`HttpMessageNotReadableException`).
  - `403 Forbidden`: Used for `ForbiddenException` and Spring Security's `AccessDeniedException`.
  - `404 Not Found`: Used for `ResourceNotFoundException`.
  - `409 Conflict`: Used for `InsufficientBudgetException`, `OverBudgetException`, and database integrity violations (`DataIntegrityViolationException`).
  - `500 Internal Server Error`: Catch-all for any other `Exception`, ensuring no raw stack traces are exposed to the client.

### 2. Custom Exception Hierarchy
The application defines a set of custom unchecked exceptions (extending `RuntimeException`) in the `vn.campuslife.exception` package. These are thrown by service layers to signal specific business logic failures.

- **General Purpose**:
  - `BadRequestException`: For invalid input or business rule violations not covered by bean validation.
  - `ResourceNotFoundException`: When a requested entity (e.g., Activity, Article) does not exist.
  - `ForbiddenException`: When a user lacks permission for an action despite being authenticated.
  - `FeatureNotEnabledException`: Used to gate features that may be disabled for specific activities or globally.

- **Domain-Specific (Preparation/Finance)**:
  - `InsufficientBudgetException`: Thrown when a budget operation cannot proceed due to lack of funds.
  - `OverBudgetException`: A richer exception that includes an `OverBudgetInfoDto` in its payload. This DTO provides contextual data such as `requiredAdditionalAmount`, `currentAllocatedAmount`, and `suggestedSources`, allowing the frontend to offer remediation options (e.g., requesting more budget).

### 3. Validation and Data Integrity
- **Bean Validation**: The handler intercepts `MethodArgumentNotValidException` to extract the first field error message, providing concise feedback to the client rather than a full list of violations.
- **Database Constraints**: `DataIntegrityViolationException` is caught to prevent raw SQL errors from leaking. The handler logs the full exception server-side but returns a sanitized message to the client.
- **Malformed Input**: `HttpMessageNotReadableException` handles cases where the request body cannot be deserialized (e.g., wrong JSON structure), returning a clear "Invalid request body" message.

### 4. Developer Conventions
- **Throw Early, Fail Fast**: Services should throw specific custom exceptions (e.g., `new ResourceNotFoundException("Activity not found")`) rather than returning null or generic error codes.
- **No Raw Exceptions in Controllers**: Controllers should not catch these exceptions locally; they must propagate to the `GlobalExceptionHandler` to ensure consistent formatting.
- **Logging**: The global handler logs unexpected exceptions (`Exception.class`) and data integrity violations at the ERROR level with full stack traces for debugging, while business exceptions are typically not logged at the global level (assumed to be handled or logged upstream if necessary).
- **Sanitization**: Error messages from low-level exceptions (like SQL errors) are truncated to 200 characters to prevent excessive payload sizes and potential information leakage.