package apps.sarafrika.elimika.course.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** When the course or program creator first opened an application. */
public record TrainingApplicationFirstOpenedView(UUID applicationUuid, LocalDateTime openedAt) {
}
