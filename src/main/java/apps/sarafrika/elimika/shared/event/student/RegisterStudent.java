package apps.sarafrika.elimika.shared.event.student;

import java.util.UUID;

public record RegisterStudent(
        String fullName,
        UUID userUuid
) {
}
