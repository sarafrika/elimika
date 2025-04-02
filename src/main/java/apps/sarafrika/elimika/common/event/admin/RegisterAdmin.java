package apps.sarafrika.elimika.common.event.admin;

import java.util.UUID;

public record RegisterAdmin(
        String fullName,
        UUID userUuid
) {
}
