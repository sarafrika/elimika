package apps.sarafrika.elimika.course.internal;

import java.util.UUID;

/** A programme's course weights or mandatory flags changed, so every enrolment's mark must be recomputed. */
public record ProgramWeightsChangedEvent(UUID programUuid) {
}
