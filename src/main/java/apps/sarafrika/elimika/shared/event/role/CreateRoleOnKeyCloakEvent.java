package apps.sarafrika.elimika.shared.event.role;

import java.util.UUID;

public record CreateRoleOnKeyCloakEvent(String roleName, String description, String realm, UUID sarafrikaCorrelationId) {
}
