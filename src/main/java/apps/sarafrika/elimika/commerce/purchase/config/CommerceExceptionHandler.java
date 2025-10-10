package apps.sarafrika.elimika.commerce.purchase.config;

import apps.sarafrika.elimika.commerce.purchase.exception.CommercePaymentRequiredException;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import java.time.LocalDateTime;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class CommerceExceptionHandler {

    @ExceptionHandler(CommercePaymentRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentRequired(CommercePaymentRequiredException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }
}
