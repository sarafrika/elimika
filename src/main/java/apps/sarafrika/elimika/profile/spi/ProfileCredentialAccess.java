package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/** The one credential-read rule, exposed as the bean {@code profileCredentialSecurityService}. */
public interface ProfileCredentialAccess {

    boolean canReadCredentials(UUID userUuid);
}
