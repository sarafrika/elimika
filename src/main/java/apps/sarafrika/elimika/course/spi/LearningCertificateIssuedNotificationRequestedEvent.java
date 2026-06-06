package apps.sarafrika.elimika.course.spi;

import java.util.UUID;

public record LearningCertificateIssuedNotificationRequestedEvent(
        UUID studentUuid,
        UUID certificateUuid,
        String certificateNumber,
        UUID courseUuid,
        UUID programUuid,
        String learningTitle
) {
}
