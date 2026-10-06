package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;

import java.util.UUID;

/** A course enrolment's pass/fail result changed; program results are recomputed from it. */
public record CourseResultDecidedEvent(
        UUID enrollmentUuid,
        UUID studentUuid,
        UUID courseUuid,
        CourseResultStatus result
) {
}
