package apps.sarafrika.elimika.shared.event.role;

import java.util.UUID;

public record AssignRoleToUserEvent(UUID userKeyCloakId, String roleName, String realm) {
}
