package apps.sarafrika.elimika.config;

import apps.sarafrika.elimika.shared.audit.ApplicationAuditAware;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
class ApplicationConfig {

    @Bean
    AuditorAware<String> auditorProvider() {
        return new ApplicationAuditAware();
    }

    @Bean
    ApplicationRunner initStorage(StorageService storageService) {
        return args -> {
            storageService.init();
        };
    }
}
