package apps.sarafrika.elimika.commerce.internal.config;

import apps.sarafrika.elimika.commerce.internal.service.OrderNumbers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers configuration properties for the internal commerce mode.
 */
@Configuration
@EnableConfigurationProperties({
        InternalCommerceProperties.class,
        CommerceCaptureProperties.class,
        PaymentCallbackProperties.class,
        CommerceOrderNumberProperties.class})
public class InternalCommerceConfiguration {

    @Bean
    OrderNumbers orderNumbers(CommerceOrderNumberProperties properties) {
        return OrderNumbers.of(properties.getAlphabet());
    }
}
