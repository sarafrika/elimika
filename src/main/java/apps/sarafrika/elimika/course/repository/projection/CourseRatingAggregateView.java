package apps.sarafrika.elimika.course.repository.projection;

import java.util.UUID;

/** Average rating and review count for one course, from a single grouped aggregate over a page. */
public record CourseRatingAggregateView(UUID courseUuid, Double averageRating, long reviewCount) {
}
