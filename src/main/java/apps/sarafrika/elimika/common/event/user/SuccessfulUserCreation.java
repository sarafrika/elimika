package apps.sarafrika.elimika.common.event.user;

import java.util.UUID;

public record SuccessfulUserCreation(UUID userId, String keycloakId) {
}
