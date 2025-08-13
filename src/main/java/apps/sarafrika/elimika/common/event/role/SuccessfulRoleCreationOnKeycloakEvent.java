package apps.sarafrika.elimika.common.event.role;

import java.util.UUID;

public record SuccessfulRoleCreationOnKeycloakEvent(UUID sarafrikaCorrelationId, UUID keycloakId) {
}
