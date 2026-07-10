package apps.sarafrika.elimika.commerce.payment.client;

import apps.sarafrika.elimika.commerce.payment.config.MpesaGatewayProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Obtains and caches a Keycloak access token via the OAuth2 client-credentials grant, used to
 * authenticate elimika's calls to the mpesa-service gateway.
 * <p>
 * The token is cached in memory and refreshed shortly before it expires. Fetch failures are
 * logged and surfaced as an empty token so the caller stays resilient (the downstream call will
 * fail with a 401 that {@link MpesaGatewayClient} already wraps).
 */
@Component
@Slf4j
public class MpesaOAuthTokenProvider {

    /** Refresh the token this long before its advertised expiry to avoid edge-of-expiry races. */
    private static final Duration EXPIRY_LEEWAY = Duration.ofSeconds(30);

    private final MpesaGatewayProperties properties;
    private final RestClient tokenRestClient;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public MpesaOAuthTokenProvider(MpesaGatewayProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.tokenRestClient = builder.build();
    }

    /**
     * Returns a valid bearer token, fetching a new one if the cache is empty or expired. Returns
     * {@code null} when client credentials are not configured or the token request fails.
     */
    public synchronized String getAccessToken() {
        MpesaGatewayProperties.OAuth oauth = properties.getOauth();
        if (oauth == null || !StringUtils.hasText(oauth.getClientId())
                || !StringUtils.hasText(oauth.getTokenUri())) {
            return null;
        }

        CachedToken current = cache.get();
        if (current != null && current.isValid()) {
            return current.accessToken();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", oauth.getClientId());
            if (StringUtils.hasText(oauth.getClientSecret())) {
                form.add("client_secret", oauth.getClientSecret());
            }

            Map<String, Object> response = tokenRestClient.post()
                    .uri(oauth.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {
                    });

            if (response == null || !(response.get("access_token") instanceof String accessToken)
                    || !StringUtils.hasText(accessToken)) {
                log.error("Keycloak token endpoint returned no access_token for mpesa-service client {}",
                        oauth.getClientId());
                return null;
            }

            long expiresIn = response.get("expires_in") instanceof Number number ? number.longValue() : 60L;
            Instant expiresAt = Instant.now().plusSeconds(expiresIn).minus(EXPIRY_LEEWAY);
            cache.set(new CachedToken(accessToken, expiresAt));
            log.debug("Obtained mpesa-service access token, expiring in {}s", expiresIn);
            return accessToken;
        } catch (RestClientException ex) {
            log.error("Failed to obtain mpesa-service access token from {}: {}",
                    oauth.getTokenUri(), ex.getMessage());
            return null;
        }
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
