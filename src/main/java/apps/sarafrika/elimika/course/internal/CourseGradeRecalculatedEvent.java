package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;

import java.util.UUID;

/** A course enrolment's grade was recalculated; programs containing the course recompute theirs. */
public record CourseGradeRecalculatedEvent(
        UUID enrollmentUuid,
        UUID studentUuid,
        UUID courseUuid,
        CourseResultStatus result
) {
}
