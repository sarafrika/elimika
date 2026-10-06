package apps.sarafrika.elimika.student.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues guardian invitation tokens and the links that carry them. Only the SHA-256 hash is
 * persisted, so a database read cannot be turned into someone else's guardian access.
 */
@Component
public class GuardianInvitationTokens {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final String frontendUrl;

    public GuardianInvitationTokens(@Value("${app.email.frontend.url:https://elimika.sarafrika.com}") String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public String invitationLink(String rawToken) {
        return frontendUrl + "/guardian-links/" + rawToken;
    }

    public String guardianDashboardLink() {
        return frontendUrl + "/dashboard/parent";
    }
}
