package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.DatabaseAuditException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.exceptions.SmtpAuthenticationException;
import apps.sarafrika.elimika.shared.utils.ValidationErrorUtil;
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
            if (exceptionMessage.contains("foreign key")) {
                message = "Cannot complete operation: referenced record does not exist or is in use";
            } else if (exceptionMessage.contains("unique")) {
                message = "A record with this information already exists";
            } else if (exceptionMessage.contains("check constraint")) {
                message = "The provided data does not meet validation requirements";
            } else if (exceptionMessage.contains("not null") || exceptionMessage.contains("NOT NULL")) {
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