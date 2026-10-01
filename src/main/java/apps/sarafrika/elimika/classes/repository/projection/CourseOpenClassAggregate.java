package apps.sarafrika.elimika.classes.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The open classes of one course, aggregated in the database.
 *
 * @param courseUuid the course
 * @param classCount how many of its classes are open
 * @param minFee     the lowest sale price among them, null when none has one
 */
public record CourseOpenClassAggregate(UUID courseUuid, long classCount, BigDecimal minFee) {
}
