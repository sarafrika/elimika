package apps.sarafrika.elimika.tenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The default admin every new database starts with; it retires once a real admin signs in.
 * A blank password means one is generated and logged once; it is always temporary.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class AdminBootstrapProperties {

    private boolean enabled = true;
    private String email = "admin@elimika.local";
    private String firstName = "Elimika";
    private String middleName;
    private String lastName = "Administrator";
    private String phoneNumber;
    private String password;
}
