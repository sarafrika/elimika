package apps.sarafrika.elimika.notifications.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.template.ReceiptPdfRenderer;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The receipt attached to the payment email renders as a real one-page PDF with the payment on it. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@DisplayName("Receipt PDF")
class ReceiptPdfIntegrationTest {

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

    @Autowired private ReceiptPdfRenderer renderer;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void thePaymentReceiptRendersWithTheOrderAmountAndMpesaReceipt() throws Exception {
        NotificationEvent event = mock(NotificationEvent.class);
        when(event.getNotificationId()).thenReturn(UUID.randomUUID());
        when(event.getNotificationType()).thenReturn(NotificationType.ORDER_PAYMENT_RECEIPT);
        when(event.getRecipientName()).thenReturn("Wanjiku Njeri");
        when(event.getRecipientEmail()).thenReturn("wanjiku@example.com");
        when(event.getTemplateVariables()).thenReturn(Map.of(
                "orderDisplayId", "ELM-K7Q9PX",
                "amountLabel", "KES 10,500",
                "currencyCode", "KES",
                "mpesaReceipt", "UK42C120ZR",
                "payerMasked", "+254 7•• ••• 678",
                "paidAt", "4 Nov 2026, 22:43 EAT",
                "items", List.of(CartItemResponse.builder().id("l1").title("Flower Gardening - Sat cohort")
                        .quantity(1).total(new BigDecimal("10500.0000")).build())));

        ReceiptPdfRenderer.Attachment pdf = renderer.render(event).orElseThrow();

        assertThat(pdf.filename()).isEqualTo("Elimika-receipt-ELM-K7Q9PX.pdf");
        try (PDDocument document = PDDocument.load(pdf.content())) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("ELM-K7Q9PX", "KES 10,500", "UK42C120ZR", "Flower Gardening - Sat cohort");
        }
    }
}
