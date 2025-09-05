package apps.sarafrika.elimika.shared.event.role;

import java.util.UUID;

public record SuccessfulRoleCreationOnKeycloakEvent(UUID sarafrikaCorrelationId, UUID keycloakId) {
}
