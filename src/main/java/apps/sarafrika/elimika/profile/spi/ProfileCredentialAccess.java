package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/**
 * The one rule for reading a user's credential material (education, memberships, certifications,
 * documents): the owner, a platform admin, staff of an organisation the user belongs to, or a party a
 * {@link ProfileAccessGrant} vouches for. Exposed as the bean {@code profileCredentialSecurityService}.
 */
public interface ProfileCredentialAccess {

    boolean canReadCredentials(UUID userUuid);
}
