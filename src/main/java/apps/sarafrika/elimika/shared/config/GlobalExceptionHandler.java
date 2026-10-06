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
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.utils.ValidationErrorUtil;
import apps.sarafrika.elimika.student.spi.StudentAgeGateException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLTransientConnectionException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Error code a client checks to show the pending-approval screen instead of a generic denial. */
    public static final String DOMAIN_PENDING_APPROVAL = "DOMAIN_PENDING_APPROVAL";

    // Optional so the handler can still be constructed directly in standalone MockMvc tests.
    @Autowired(required = false)
    private DomainSecurityService domainSecurityService;

    /** Seconds a client should wait before retrying when no database connection could be acquired. */
    private static final String DATABASE_BUSY_RETRY_AFTER_SECONDS = "2";

    /**
     * No pooled database connection became free within the Hikari acquire timeout. This is overload, not a
     * server fault: answer 503 with Retry-After so clients and load balancers back off, and keep the request
     * thread free instead of letting it fail as a 500.
     */
    @ExceptionHandler({CannotGetJdbcConnectionException.class, CannotCreateTransactionException.class,
            SQLTransientConnectionException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConnectionUnavailable(Exception ex) {
        return databaseBusy(ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecordNotFoundException(ResourceNotFoundException ex) {
        log.debug("Record not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Record not found", ex.getMessage()));
    }

    /**
     * Free text ({@code q}) is served only by the search engine. When search is off, the index's reads
     * are not enabled, or the engine fails, the request cannot be answered and there is no database
     * fallback. The engine's message is not echoed or logged at warn level: it can quote filter values.
     */
    @ExceptionHandler(SearchUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearchUnavailable(SearchUnavailableException ex) {
        log.warn("Search unavailable; answering 503");
        log.debug("Search unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Search is unavailable",
                        "Text search (q) is disabled or temporarily unavailable; try again later"));
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request payload", ex);

        String message = "Request body contains invalid or malformed JSON";
        Object error = null;

        if (containsCauseMessage(ex, "LocationType")) {
            message = "Validation failed";
            error = Map.of("location_type", "location_type must be one of ONLINE, IN_PERSON, HYBRID");
        }

        ApiResponse<Void> response = error == null
                ? ApiResponse.error(message)
                : ApiResponse.error(message, error);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
            } else if (normalizedMessage.contains("not null") || normalizedMessage.contains("not-null")) {
                // PostgreSQL writes "violates not-null constraint" — hyphenated. Matching only the
                // spaced form let every not-null violation fall through to the generic message.
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
        if (isConnectionUnavailable(ex)) {
            return databaseBusy(ex);
        }
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
     * Handles requests for paths that do not match any endpoint.
     * Without this, unknown routes fall into the generic handler and surface
     * as 500 "unexpected error" instead of 404, hiding client-side URL bugs.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("No endpoint for path: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", "No endpoint: " + ex.getResourcePath()));
    }

    /**
     * Handles request parameters/path variables that cannot be converted to
     * the declared type (e.g. a datetime sent where a date is expected, or a
     * non-UUID value in a UUID path segment). These are client errors and
     * must surface as 400 with an actionable message, not a generic 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "the expected type";
        String message = String.format("Parameter '%s' has invalid value '%s' (expected %s)",
                ex.getName(), ex.getValue(), requiredType);
        log.debug("Request parameter type mismatch: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request parameter", message));
    }

    /**
     * Handles all other unexpected exceptions.
     * Logs full details but returns generic message to client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        if (isConnectionUnavailable(ex)) {
            return databaseBusy(ex);
        }
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error occurred [errorId={}]", errorId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again or contact support"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Access denied", ex);
        if (domainSecurityService != null && domainSecurityService.isAwaitingDomainApproval()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account is awaiting approval by an Elimika administrator",
                            Map.of("code", DOMAIN_PENDING_APPROVAL)));
        }
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

    private boolean containsCauseMessage(Throwable throwable, String value) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(value)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ApiResponse<Void>> databaseBusy(Exception ex) {
        log.warn("No database connection available; answering 503: {}", ex.getMessage());
        log.debug("Database connection unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, DATABASE_BUSY_RETRY_AFTER_SECONDS)
                .body(ApiResponse.error("Service temporarily unavailable",
                        "The server is busy; please retry shortly"));
    }

    /**
     * True when the failure, at any depth, is a connection-pool acquire timeout. Hibernate and Spring wrap
     * Hikari's {@link SQLTransientConnectionException} differently depending on where it surfaced (transaction
     * begin, repository call, lazy load), so the cause chain is the reliable signal.
     */
    private boolean isConnectionUnavailable(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 16) {
            if (current instanceof SQLTransientConnectionException
                    || current instanceof CannotGetJdbcConnectionException
                    || current instanceof CannotCreateTransactionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
