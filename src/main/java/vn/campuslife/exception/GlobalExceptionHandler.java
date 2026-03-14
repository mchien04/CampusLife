package vn.campuslife.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.campuslife.model.Response;

import java.util.Comparator;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(InsufficientBudgetException.class)
    public ResponseEntity<Response> handleInsufficientBudget(InsufficientBudgetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.error(ex.getMessage()));
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.error("Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUnhandled(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error("Server error occurred"));
    }
}

