package apps.sarafrika.elimika.shared.spi.enrollment;

import java.util.Set;
import java.util.UUID;

/**
 * A learner's links to organisations, instructors and courses, as seen from their class enrolments and
 * organisation memberships.
 *
 * @param organisationUuids organisations whose classes the learner attends or that they belong to
 * @param instructorUuids   instructors who teach the learner's class sessions or own their classes
 * @param courseUuids       courses delivered by the learner's classes
 */
public record LearnerAffiliations(Set<UUID> organisationUuids, Set<UUID> instructorUuids, Set<UUID> courseUuids) {

    public LearnerAffiliations {
        organisationUuids = organisationUuids == null ? Set.of() : Set.copyOf(organisationUuids);
        instructorUuids = instructorUuids == null ? Set.of() : Set.copyOf(instructorUuids);
        courseUuids = courseUuids == null ? Set.of() : Set.copyOf(courseUuids);
    }

    public static LearnerAffiliations none() {
        return new LearnerAffiliations(Set.of(), Set.of(), Set.of());
    }

    public boolean isEmpty() {
        return organisationUuids.isEmpty() && instructorUuids.isEmpty() && courseUuids.isEmpty();
    }
}
