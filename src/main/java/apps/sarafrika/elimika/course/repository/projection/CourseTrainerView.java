package apps.sarafrika.elimika.course.repository.projection;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of a course's trainer directory as an unprivileged caller may know it: who was approved,
 * and when.
 * <p>
 * The point of the projection is what it leaves out. A training application also carries the rate
 * card the applicant negotiated with the course creator, and that is the creator's business alone.
 * Loading the entity and blanking the rates afterwards would still put every figure in the JVM, in
 * a managed entity, one careless {@code toDTO} away from the wire; selecting these three columns
 * means the rates are never read at all. The query behind this record names no rate column, so
 * there is nothing to leak even if a later change starts logging what it loaded.
 *
 * @param applicantType whether the trainer is an instructor or an organisation
 * @param applicantUuid the instructor or organisation identifier
 * @param approvedAt    when the application was approved; null on rows approved before the column
 *                      was populated
 */
public record CourseTrainerView(CourseTrainingApplicantType applicantType,
                                UUID applicantUuid,
                                LocalDateTime approvedAt) {
}
