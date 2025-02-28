package apps.sarafrika.elimika.common.event.user;

import java.util.UUID;

public record SuccessfulUserUpdateEvent(String keyCloakId, UUID userId) {
}
