package apps.sarafrika.elimika.commerce.internal.config;

import apps.sarafrika.elimika.commerce.internal.service.OrderNumbers;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The alphabet order numbers are encoded with. It must match the one the Flyway backfill read
 * ({@code COMMERCE_ORDER_NUMBER_ALPHABET}) and must never change once orders exist.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "commerce.order-number")
public class CommerceOrderNumberProperties {

    private String alphabet = OrderNumbers.DEFAULT_ALPHABET;
}
