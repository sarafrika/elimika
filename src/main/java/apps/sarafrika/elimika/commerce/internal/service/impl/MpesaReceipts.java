package apps.sarafrika.elimika.commerce.internal.service.impl;

import java.util.regex.Pattern;

/**
 * Only a real M-Pesa receipt (10 capitals and digits, e.g. UK5AB1C2D3). Until Safaricom's callback
 * arrives the gateway reports the STK CheckoutRequestID there, which must never reach a receipt.
 */
public final class MpesaReceipts {

    private static final Pattern RECEIPT = Pattern.compile("[A-Z0-9]{10}");

    private MpesaReceipts() {
    }

    public static String orNull(String value) {
        return value != null && RECEIPT.matcher(value.trim()).matches() ? value.trim() : null;
    }
}
