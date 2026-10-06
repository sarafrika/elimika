package apps.sarafrika.elimika.tenancy.config;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;
import java.util.Set;

/** Self-registration settings; domains in {@code approvalRequiredDomains} stay pending until an admin approves. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    /** Domains a person may choose when registering. Admin is never self-registerable. */
    private Set<UserDomain> selfRegisterableDomains = EnumSet.of(
            UserDomain.student, UserDomain.instructor, UserDomain.course_creator,
            UserDomain.parent, UserDomain.organisation_user);

    /** Domains that stay pending until a platform admin approves them. */
    private Set<UserDomain> approvalRequiredDomains = EnumSet.of(
            UserDomain.student, UserDomain.instructor, UserDomain.course_creator,
            UserDomain.parent, UserDomain.organisation_user);

    /** Keycloak client the set-password link returns to. */
    private String clientId = "elimika-ui";

    /** Where Keycloak sends the person after they set their password. */
    private String redirectUri;

    /** How long the set-password link stays valid, in seconds. */
    private int actionsEmailLifespanSeconds = 72 * 60 * 60;

    /** Registration and resend attempts allowed per client IP per hour. */
    private int maxAttemptsPerIpPerHour = 10;

    /** Registration and resend attempts allowed per email address per hour. */
    private int maxAttemptsPerEmailPerHour = 3;

    private final Captcha captcha = new Captcha();

    public boolean requiresApproval(UserDomain domain) {
        return domain != null && approvalRequiredDomains.contains(domain);
    }

    @Getter
    @Setter
    public static class Captcha {

        /** When true every registration must carry a Turnstile token that verifies. */
        private boolean enabled = false;

        private String secret;

        private String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    }
}
