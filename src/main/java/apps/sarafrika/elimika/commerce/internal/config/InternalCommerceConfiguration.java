package apps.sarafrika.elimika.commerce.internal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers configuration properties for the internal commerce mode.
 */
@Configuration
@EnableConfigurationProperties(InternalCommerceProperties.class)
public class InternalCommerceConfiguration {
}
