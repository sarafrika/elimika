package apps.sarafrika.elimika.notifications.config;

import apps.sarafrika.elimika.notifications.preferences.spi.exceptions.PreferencesException;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class NotificationsExceptionHandler {

    @ExceptionHandler(PreferencesException.class)
    public ResponseEntity<ApiResponse<Void>> handlePreferencesException(PreferencesException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Notification preferences error [errorId={}]", errorId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unable to load notification preferences", ex.getMessage()));
    }
}
