package apps.sarafrika.elimika.course.util;

import apps.sarafrika.elimika.course.model.Course;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Validates that a course splits revenue between its creator and instructor coherently.
 * <p>
 * Extracted from CourseServiceImpl so the draft-promotion path can apply the same rule to
 * the merged live course without depending on CourseService (which would be circular).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseRevenueShareValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static void validate(Course course) {
        BigDecimal creatorShare = course.getCreatorSharePercentage();
        BigDecimal instructorShare = course.getInstructorSharePercentage();

        if (creatorShare == null || instructorShare == null) {
            throw new IllegalArgumentException("Creator and instructor share percentages are required for a course");
        }

        if (creatorShare.compareTo(BigDecimal.ZERO) < 0 || instructorShare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Revenue share percentages cannot be negative");
        }

        if (creatorShare.compareTo(HUNDRED) > 0 || instructorShare.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Revenue share percentages cannot exceed 100%");
        }

        if (creatorShare.add(instructorShare).compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Creator and instructor share percentages must add up to 100%");
        }
    }
}
