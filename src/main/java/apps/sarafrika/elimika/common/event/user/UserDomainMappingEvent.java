package apps.sarafrika.elimika.common.event.user;

import java.util.UUID;

public record UserDomainMappingEvent(
        UUID userUuid,
        String userDomain
) {
}
