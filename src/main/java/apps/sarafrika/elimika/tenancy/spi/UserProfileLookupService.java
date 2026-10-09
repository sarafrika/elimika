package apps.sarafrika.elimika.tenancy.spi;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;

import java.util.UUID;

/**
 * Hands other modules the same full user record {@code GET /api/v1/users/me} serves, so a
 * composite read such as the session bootstrap stays byte-for-byte identical to it.
 */
public interface UserProfileLookupService {

    /**
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException when no user matches
     */
    UserDTO getUserProfile(UUID userUuid);
}
