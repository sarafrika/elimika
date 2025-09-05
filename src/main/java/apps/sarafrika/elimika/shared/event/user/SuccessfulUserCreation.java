package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

public record SuccessfulUserCreation(UUID userId, String keycloakId) {
}
