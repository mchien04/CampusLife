package vn.campuslife.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.campuslife.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FeatureNotEnabledException.class)
    public ResponseEntity<Response> handleFeatureNotEnabled(FeatureNotEnabledException ex) {
        return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Response> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Response> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBudgetException.class)
    public ResponseEntity<Response> handleInsufficientBudget(InsufficientBudgetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(OverBudgetException.class)
    public ResponseEntity<Response> handleOverBudget(OverBudgetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Response(false, ex.getMessage(), ex.getInfo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(Response.error(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> handleNotReadable(HttpMessageNotReadableException ex) {
        String detail = null;
        if (ex.getMostSpecificCause() != null) {
            detail = ex.getMostSpecificCause().getMessage();
        }
        if (detail == null || detail.isBlank()) {
            return ResponseEntity.badRequest().body(Response.error("Invalid request body"));
        }
        if (detail.length() > 200) {
            detail = detail.substring(0, 200);
        }
        return ResponseEntity.badRequest().body(Response.error("Invalid request body: " + detail));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (detail != null && detail.length() > 200) {
            detail = detail.substring(0, 200);
        }
        logger.error("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Response.error(detail != null ? detail : "Data conflict"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.error("Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUnhandled(Exception ex) {
        logger.error("Unhandled exception", ex);
        String detail = getRootMessage(ex);
        if (detail != null && detail.length() > 200) {
            detail = detail.substring(0, 200);
        }
        String message = "Server error occurred";
        if (detail != null && !detail.isBlank()) {
            message = message + ": " + detail;
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(message));
    }

    private String getRootMessage(Throwable t) {
        if (t == null) {
            return null;
        }
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage() != null ? cur.getMessage() : t.getMessage();
    }
}
