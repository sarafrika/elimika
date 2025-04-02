package apps.sarafrika.elimika.common.event.student;

import java.util.UUID;

public record RegisterStudent(
        String fullName,
        UUID userUuid
) {
}
