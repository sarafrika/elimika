package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

public record SuccessfulUserUpdateEvent(String keyCloakId, UUID userId) {
}
