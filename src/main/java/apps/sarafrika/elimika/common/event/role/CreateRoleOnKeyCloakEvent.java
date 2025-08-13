package apps.sarafrika.elimika.common.event.role;

import java.util.UUID;

public record CreateRoleOnKeyCloakEvent(String roleName, String description, String realm, UUID sarafrikaCorrelationId) {
}
