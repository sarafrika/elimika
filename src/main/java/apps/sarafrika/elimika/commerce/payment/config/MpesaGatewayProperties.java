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
     * HTTP Basic auth username for the mpesa-service.
     */
    private String username;

    /**
     * HTTP Basic auth password for the mpesa-service.
     */
    private String password;
}
