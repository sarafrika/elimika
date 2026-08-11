package apps.sarafrika.elimika.tenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class AdminBootstrapProperties {

    private boolean enabled = false;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String phoneNumber;
}
