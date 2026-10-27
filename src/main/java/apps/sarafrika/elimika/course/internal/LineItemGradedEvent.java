package apps.sarafrika.elimika.course.internal;

import java.time.LocalDateTime;
import java.util.UUID;

/** A learner's gradebook cell was scored or rescored. */
public record LineItemGradedEvent(UUID enrollmentUuid, UUID lineItemUuid, UUID courseAssessmentUuid, LocalDateTime gradedAt) {
}
