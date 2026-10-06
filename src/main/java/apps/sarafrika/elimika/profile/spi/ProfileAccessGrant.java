package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/**
 * A relationship another module knows about that lets a caller read a user's credentials, such as
 * reviewing a training application that user lodged. Implementations must fail closed and never throw.
 */
public interface ProfileAccessGrant {

    boolean grantsCredentialRead(UUID subjectUserUuid, UUID callerUserUuid);
}
