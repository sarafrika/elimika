package apps.sarafrika.elimika.shared.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * The location for storing files.
     */
    private String location;

}
