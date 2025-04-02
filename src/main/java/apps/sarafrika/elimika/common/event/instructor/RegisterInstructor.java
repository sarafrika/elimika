package apps.sarafrika.elimika.common.event.instructor;

import java.util.UUID;

public record RegisterInstructor(
        String fullName,
        UUID userUuid
) {
}
