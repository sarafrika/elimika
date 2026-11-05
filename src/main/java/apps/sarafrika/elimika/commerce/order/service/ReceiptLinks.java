package apps.sarafrika.elimika.commerce.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Signed links that open a paid order's PDF receipt without signing in. Only a link we issued
 * verifies, so order numbers alone can't be walked to read other buyers' receipts.
 */
@Component
@Slf4j
public class ReceiptLinks {

    public static final String PATH = "/api/v1/public/receipts/";
    private static final int TOKEN_BYTES = 16;

    private final byte[] secret;
    private final String apiBaseUrl;

    public ReceiptLinks(@Value("${commerce.receipt-link.secret:}") String secret,
                        @Value("${commerce.receipt-link.api-base-url:}") String apiBaseUrl) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.apiBaseUrl = apiBaseUrl.replaceAll("/+$", "");
        if (!StringUtils.hasText(secret)) {
            log.warn("commerce.receipt-link.secret is not set; payment emails go out without a download link");
        }
    }

    /** The public download URL for an order, or empty when links are not configured. */
    public Optional<String> url(String orderId) {
        if (secret.length == 0 || !StringUtils.hasText(orderId) || !StringUtils.hasText(apiBaseUrl)) {
            return Optional.empty();
        }
        return Optional.of(apiBaseUrl + PATH + orderId + ".pdf?t=" + token(orderId));
    }

    public boolean verify(String orderId, String token) {
        if (secret.length == 0 || !StringUtils.hasText(orderId) || !StringUtils.hasText(token)) {
            return false;
        }
        return MessageDigest.isEqual(token(orderId).getBytes(StandardCharsets.US_ASCII), token.getBytes(StandardCharsets.US_ASCII));
    }

    private String token(String orderId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(("receipt:" + orderId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, TOKEN_BYTES));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 unavailable", ex);
        }
    }
}
