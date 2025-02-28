package apps.sarafrika.elimika.common.event.role;

import java.util.UUID;

public record AssignRoleToUserEvent(UUID userKeyCloakId, String roleName, String realm) {
}
