package apps.sarafrika.elimika.profile.spi;

import java.util.UUID;

/** A relationship (e.g. reviewing the user's application) that lets a caller read their credentials; fails closed. */
public interface ProfileAccessGrant {

    boolean grantsCredentialRead(UUID subjectUserUuid, UUID callerUserUuid);
}
