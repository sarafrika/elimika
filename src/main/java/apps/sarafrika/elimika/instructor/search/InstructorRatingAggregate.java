package apps.sarafrika.elimika.instructor.search;

import java.util.UUID;

/**
 * Review metrics for one instructor, aggregated in the database for search documents.
 *
 * @param instructorUuid the reviewed instructor
 * @param average        the mean rating, null when there are no reviews
 * @param count          the number of reviews
 */
public record InstructorRatingAggregate(UUID instructorUuid, Double average, Long count) {
}
