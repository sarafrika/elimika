package apps.sarafrika.elimika.config;

import apps.sarafrika.elimika.audit.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class ApplicationConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new ApplicationAuditAware();
    }
}
