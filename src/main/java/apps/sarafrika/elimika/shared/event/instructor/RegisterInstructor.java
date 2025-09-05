package apps.sarafrika.elimika.shared.event.instructor;

import java.util.UUID;

public record RegisterInstructor(
        String fullName,
        UUID userUuid
) {
}
