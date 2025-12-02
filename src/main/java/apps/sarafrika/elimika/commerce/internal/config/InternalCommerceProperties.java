package apps.sarafrika.elimika.commerce.internal.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag controlling the rollout of the internal commerce stack.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "commerce.internal")
public class InternalCommerceProperties {

    /**
     * Enables the internally managed catalog/cart/order implementation.
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * Default currency code used when provisioning catalog entries if none is provided upstream.
     */
    private String defaultCurrency = "USD";

    /**
     * Default region code used for commerce flows when client input is absent or ignored.
     */
    private String defaultRegion = "KE";

    /**
     * Enables IP-based geo lookups to derive region codes.
     */
    private Boolean geoipEnabled = Boolean.FALSE;

    /**
     * Endpoint template to resolve country/region from IP. Must contain {ip} placeholder.
     */
    private String geoipCountryEndpoint = "https://ipapi.co/{ip}/country/";
}
