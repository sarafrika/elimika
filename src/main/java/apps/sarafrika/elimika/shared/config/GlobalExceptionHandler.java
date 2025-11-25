package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.AgeRestrictionException;
import apps.sarafrika.elimika.shared.exceptions.DatabaseAuditException;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.InvalidCsvFormatException;
import apps.sarafrika.elimika.shared.exceptions.KeycloakException;
import apps.sarafrika.elimika.shared.exceptions.PaymentRequiredException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.exceptions.SmtpAuthenticationException;
import apps.sarafrika.elimika.shared.exceptions.SmtpConnectionException;
import apps.sarafrika.elimika.shared.exceptions.SmtpMessagingException;
import apps.sarafrika.elimika.shared.exceptions.UserNotFoundException;
import apps.sarafrika.elimika.shared.utils.ValidationErrorUtil;
import apps.sarafrika.elimika.student.spi.StudentAgeGateException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecordNotFoundException(ResourceNotFoundException ex) {
        log.debug("Record not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Record not found", ex.getMessage()));
    }

    @ExceptionHandler(StudentAgeGateException.class)
    public ResponseEntity<ApiResponse<Void>> handleStudentAgeGateException(StudentAgeGateException ex) {
        log.debug("Student age gate blocked request", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Student age gate blocked the request", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.debug("Duplicate resource", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Duplicate resource", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, ValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(Exception ex) {
        log.debug("Invalid request payload", ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Request contains invalid data";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.debug("Operation cannot proceed in current state", ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Operation cannot be performed in the current state";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            message = status != null ? status.getReasonPhrase() : "Request could not be processed";
        }

        log.debug("Request rejected with status {}", ex.getStatusCode(), ex);
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.debug("Validation failed", ex);
        Map<String, String> errors = ValidationErrorUtil.buildValidationErrorMap(ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(SmtpAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSmtpAuthenticationException(SmtpAuthenticationException ex) {
        log.debug("SMTP authentication failed", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("SMTP authentication failed", ex.getMessage()));
    }

    /**
     * Handles duplicate key violations (e.g., unique constraint violations).
     * Logs full details but returns sanitized message to client.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Database duplicate key violation [errorId={}]", errorId, ex);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("A record with this information already exists"));
    }

    /**
     * Handles data integrity violations (e.g., foreign key, check constraints, not null violations).
     * Logs full details but returns sanitized message to client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Database integrity constraint violation [errorId={}]", errorId, ex);

        // Attempt to provide a more user-friendly message based on the type of constraint
        String message = "The operation cannot be completed due to a data constraint violation";

        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null) {
            String normalizedMessage = exceptionMessage.toLowerCase(Locale.ROOT);
            if (normalizedMessage.contains("foreign key")) {
                message = "Cannot complete operation: referenced record does not exist or is in use";
            } else if (normalizedMessage.contains("unique")) {
                message = "A record with this information already exists";
            } else if (normalizedMessage.contains("check constraint")) {
                message = "The provided data does not meet validation requirements";
            } else if (normalizedMessage.contains("not null")) {
                message = "Required information is missing";
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handles all other database access exceptions.
     * Logs full details but returns generic message to client.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Database access error [errorId={}]", errorId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("A database error occurred. Please try again or contact support if the problem persists"));
    }

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentRequiredException(PaymentRequiredException ex) {
        log.debug("Payment required", ex);
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error("Payment required", ex.getMessage()));
    }

    @ExceptionHandler(AgeRestrictionException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgeRestrictionException(AgeRestrictionException ex) {
        log.debug("Age restriction prevented operation", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        log.debug("User not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("User not found", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCsvFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCsvFormatException(InvalidCsvFormatException ex) {
        log.debug("Invalid CSV format", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid CSV content", ex.getMessage()));
    }

    @ExceptionHandler({SmtpConnectionException.class, SmtpMessagingException.class})
    public ResponseEntity<ApiResponse<Void>> handleSmtpExceptions(RuntimeException ex) {
        log.error("SMTP operation failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Email delivery is temporarily unavailable", ex.getMessage()));
    }

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloakException(KeycloakException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Identity provider error [errorId={}]", errorId, ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("Identity provider request failed", ex.getMessage()));
    }

    /**
     * Handles all other unexpected exceptions.
     * Logs full details but returns generic message to client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error occurred [errorId={}]", errorId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again or contact support"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Access denied", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", ex.getMessage()));
    }

    @ExceptionHandler(DatabaseAuditException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseAuditException(DatabaseAuditException ex) {
        log.debug("Database audit exception caught", ex);

        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (message.contains("already exists")) {
            status = HttpStatus.CONFLICT;
        } else if (message.contains("does not exist") || message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message.contains("Cannot delete") || message.contains("existing")) {
            status = HttpStatus.CONFLICT;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error("Operation failed", message));
    }
}
