package apps.sarafrika.elimika.tenancy.internal;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and verifies invitation tokens.
 * <p>
 * Only the SHA-256 hash of a token is ever persisted. The raw value exists exactly once,
 * in the link that is emailed to the recipient, so a database read cannot be turned into
 * the ability to accept somebody else's invitation.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Service
public class InvitationTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generates a new raw token. URL-safe, so it can be dropped straight into an invite link.
     */
    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }

    /**
     * Hashes a raw token for storage and lookup.
     */
    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
