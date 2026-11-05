package apps.sarafrika.elimika.notifications.spi;

import java.util.Map;
import java.util.Optional;

/** Renders the branded A4 payment receipt; the same PDF the receipt email carries. */
public interface ReceiptDocumentService {

    /** {@code variables} are the receipt email's template variables; empty if rendering fails. */
    Optional<byte[]> renderOrderReceipt(String recipientName, String recipientEmail, Map<String, Object> variables);
}
