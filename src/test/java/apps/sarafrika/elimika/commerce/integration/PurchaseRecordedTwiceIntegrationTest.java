package apps.sarafrika.elimika.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.commerce.purchase.spi.CommercePurchaseService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * An M-Pesa order is recorded at checkout, unpaid, and again when the payment is captured. The
 * second recording used to clear and re-add the lines, which broke the unique line key and rolled
 * back the whole capture, so no M-Pesa payment was ever recorded.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@DisplayName("A purchase recorded at checkout and again at capture")
class PurchaseRecordedTwiceIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private CommercePurchaseService purchaseService;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void theCaptureUpdatesTheCheckoutRecordInsteadOfFailing() {
        String orderId = UUID.randomUUID().toString();
        String lineId = UUID.randomUUID().toString();

        purchaseService.recordOrder(order(orderId, lineId, "AWAITING_PAYMENT"), null);
        purchaseService.recordOrder(order(orderId, lineId, "CAPTURED"), null);

        assertThat(jdbc.queryForObject(
                "SELECT payment_status FROM commerce_purchase WHERE order_id = ?", String.class, orderId))
                .isEqualTo("CAPTURED");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM commerce_purchase_item WHERE line_item_id = ?", Integer.class, lineId))
                .isEqualTo(1);
    }

    private OrderResponse order(String orderId, String lineId, String paymentStatus) {
        CartItemResponse line = CartItemResponse.builder()
                .id(lineId)
                .title("Biology")
                .quantity(1)
                .unitPrice(new BigDecimal("300.00"))
                .subtotal(new BigDecimal("300.00"))
                .total(new BigDecimal("300.00"))
                .metadata(Map.of("variant_code", "SKU-TEST"))
                .build();
        return OrderResponse.builder()
                .id(orderId)
                .displayId("ELM-68QKTX")
                .paymentStatus(paymentStatus)
                .currencyCode("KES")
                .subtotal(new BigDecimal("300.00"))
                .total(new BigDecimal("300.00"))
                .items(List.of(line))
                .build();
    }
}
