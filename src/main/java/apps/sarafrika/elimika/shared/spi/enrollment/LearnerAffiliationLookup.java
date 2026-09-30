package apps.sarafrika.elimika.shared.spi.enrollment;

import java.util.UUID;

/**
 * Who a learner is linked to through their classes, for course recommendations: the organisations and
 * instructors that teach them and the courses their classes deliver.
 * <p>
 * Lives in {@code shared} because the course module needs the answer but cannot depend on timetabling,
 * which owns class enrolments and implements this.
 */
public interface LearnerAffiliationLookup {

    /**
     * The learner's affiliations. Cancelled and waitlisted class enrolments do not count; an organisation
     * membership the learner still holds does.
     *
     * @param studentUuid the student profile; {@code null} yields {@link LearnerAffiliations#none()}
     * @return never {@code null}
     */
    LearnerAffiliations findAffiliations(UUID studentUuid);
}
