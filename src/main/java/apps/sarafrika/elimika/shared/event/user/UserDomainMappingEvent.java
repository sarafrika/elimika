package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

public record UserDomainMappingEvent(
        UUID userUuid,
        String userDomain
) {
}
