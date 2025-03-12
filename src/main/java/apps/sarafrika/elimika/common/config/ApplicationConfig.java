package apps.sarafrika.elimika.common.config;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    ApplicationRunner initStorage(StorageService storageService) {
        return args -> {
            storageService.init();
        };
    }
}
