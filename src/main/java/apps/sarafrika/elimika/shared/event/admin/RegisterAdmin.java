package apps.sarafrika.elimika.shared.event.admin;

import java.util.UUID;

public record RegisterAdmin(
        String fullName,
        UUID userUuid
) {
}
