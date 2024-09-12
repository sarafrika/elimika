package apps.sarafrika.elimika.config;

import apps.sarafrika.elimika.shared.audit.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
class ApplicationConfig {

    @Bean
    AuditorAware<String> auditorProvider() {
        return new ApplicationAuditAware();
    }
}
