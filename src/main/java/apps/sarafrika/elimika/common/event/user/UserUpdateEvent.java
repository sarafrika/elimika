package apps.sarafrika.elimika.common.event.user;

import java.util.UUID;

public record UserUpdateEvent(String username, String firstName, String lastName, String email,Boolean active, String realm, UUID sarafrikaCorrelationId, String keyCloakId) {
}
