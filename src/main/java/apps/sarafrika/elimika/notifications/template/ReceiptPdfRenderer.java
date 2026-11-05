package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Renders the A4 payment receipt attached to the receipt email. Seller details come from
 * {@code app.billing.*}; a blank one is left off the page rather than printed as a placeholder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptPdfRenderer implements apps.sarafrika.elimika.notifications.spi.ReceiptDocumentService {

    static final String TEMPLATE = "pdf/order-receipt";
    /** The same artwork the emails embed, so the PDF is branded exactly like the message it rides on. */
    static final String ELIMIKA_LOGO = "static/logos/elimika/elimika-logo-email.png";
    static final String SARAFRIKA_LOGO = "static/logos/sarafrika/sarafrika-logo-email.png";

    private final EmailTemplateService templateService;

    @Value("${app.billing.legal-name:Sarafrika Productions Ltd}")
    private String legalName;
    @Value("${app.billing.address:}")
    private String address;
    @Value("${app.billing.kra-pin:}")
    private String kraPin;
    @Value("${app.billing.vat-note:}")
    private String vatNote;

    /** The receipt PDF for a payment-receipt email, or empty for any other notification. */
    public Optional<Attachment> render(NotificationEvent event) {
        if (event.getNotificationType() != NotificationType.ORDER_PAYMENT_RECEIPT) {
            return Optional.empty();
        }
        Map<String, Object> vars = event.getTemplateVariables() == null ? Map.of() : event.getTemplateVariables();
        return renderOrderReceipt(event.getRecipientName(), event.getRecipientEmail(), vars)
                .map(pdf -> new Attachment(filename(vars), pdf));
    }

    @Override
    public Optional<byte[]> renderOrderReceipt(String recipientName, String recipientEmail, Map<String, Object> variables) {
        try {
            Map<String, Object> seller = new HashMap<>(variables);
            seller.put("sellerName", legalName);
            seller.put("sellerAddress", StringUtils.hasText(address) ? address : null);
            seller.put("sellerKraPin", StringUtils.hasText(kraPin) ? kraPin : null);
            seller.put("vatNote", StringUtils.hasText(vatNote) ? vatNote : null);
            seller.put("elimikaLogo", dataUri(ELIMIKA_LOGO));
            seller.put("sarafrikaLogo", dataUri(SARAFRIKA_LOGO));
            String html = templateService.renderDocument(TEMPLATE, recipientName, recipientEmail, seller);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();

            return Optional.of(out.toByteArray());
        } catch (Exception ex) {
            // The email still goes out without the PDF rather than not at all.
            log.error("Could not render the receipt PDF for order {}: {}", variables.get("orderDisplayId"), ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    public static String filename(Map<String, Object> variables) {
        Object order = variables.get("orderDisplayId");
        return "Elimika-receipt-" + (order == null ? "order" : order) + ".pdf";
    }

    private static String dataUri(String path) {
        try (var in = new org.springframework.core.io.ClassPathResource(path).getInputStream()) {
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (java.io.IOException ex) {
            log.warn("Logo {} could not be read for the receipt PDF: {}", path, ex.getMessage());
            return null;
        }
    }

    public record Attachment(String filename, byte[] content) {
    }
}
