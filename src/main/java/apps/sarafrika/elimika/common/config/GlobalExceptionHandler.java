package apps.sarafrika.elimika.common.config;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.exceptions.SmtpAuthenticationException;
import apps.sarafrika.elimika.course.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecordNotFoundException(RecordNotFoundException ex) {
        log.debug("Record not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Record not found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.debug("Validation failed", ex);
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(SmtpAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSmtpAuthenticationException(SmtpAuthenticationException ex) {
        log.debug("SMTP authentication failed", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("SMTP authentication failed", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Access denied", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", ex.getMessage()));
    }

    @ExceptionHandler({
            CourseNotFoundException.class,
            LessonNotFoundException.class,
            PrerequisiteTypeNotFoundException.class,
            PrerequisiteNotFoundException.class,
            PrerequisiteGroupNotFoundException.class,
            LessonResourceNotFoundException.class,
            CourseLearningObjectiveNotFoundException.class,
            CategoryNotFoundException.class,
            CourseCategoryNotFoundException.class,
            ContentTypeNotFoundException.class,
            LessonContentNotFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFoundExceptions(RuntimeException ex) {
        log.debug("Resource not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", ex.getMessage()));
    }

    @ExceptionHandler({
            PrerequisiteValidationException.class,
            ValidationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(RuntimeException ex) {
        log.debug("Validation error", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("Validation error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        final String message = "Unexpected error occurred";
        log.error(message, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, ex.getMessage()));
    }
}
