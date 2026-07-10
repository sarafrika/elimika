package apps.sarafrika.elimika.commerce.payment.config;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection details for the generic mpesa-service Daraja gateway used to charge orders.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mpesa.service")
public class MpesaGatewayProperties {

    /**
     * Base URL of the mpesa-service, e.g. {@code https://mpesa.sarafrika.com}.
     */
    private String baseUrl;

    /**
     * UUID of the configured shortcode (till/paybill) to charge for elimika orders.
     */
    private UUID shortcodeUuid;

    /**
     * Keycloak client-credentials settings used to obtain a bearer token for the mpesa-service.
     */
    private OAuth oauth = new OAuth();

    /**
     * OAuth2 client-credentials configuration for authenticating to the mpesa-service.
     */
    @Getter
    @Setter
    public static class OAuth {

        /**
         * Keycloak token endpoint, e.g.
         * {@code https://auth.sarafrika.com/realms/elimika/protocol/openid-connect/token}.
         */
        private String tokenUri;

        /**
         * Client id of the Keycloak client-credentials client provisioned for elimika&rarr;mpesa.
         */
        private String clientId;

        /**
         * Client secret of the Keycloak client-credentials client.
         */
        private String clientSecret;
    }
}
