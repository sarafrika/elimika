package apps.sarafrika.elimika.commerce.config;

import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class CommerceIntegrationExceptionHandler {

    @ExceptionHandler(MedusaIntegrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMedusaIntegrationException(MedusaIntegrationException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Medusa integration error [errorId={}]", errorId, ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("Commerce service request failed", ex.getMessage()));
    }
}
