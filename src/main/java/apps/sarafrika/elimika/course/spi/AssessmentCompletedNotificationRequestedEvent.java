package apps.sarafrika.elimika.course.spi;

import java.util.UUID;

public record AssessmentCompletedNotificationRequestedEvent(
        UUID studentUuid,
        UUID courseUuid,
        UUID enrollmentUuid,
        UUID assessmentUuid,
        UUID assessmentSourceUuid,
        String assessmentTitle,
        String assessmentType
) {
}
