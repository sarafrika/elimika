package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * One trainer's own delivery of a course.
 * <p>
 * Present only for an instructor, or a member of an organisation, holding an <em>approved</em>
 * training application for the course. A pending application is self-service — anybody may lodge
 * one — so it unlocks nothing. The figures cover the caller's own classes and no one else's; there
 * is no shape of this block that reports another trainer's learners or takings.
 */
@Schema(name = "CourseStatsScoped", description = "The calling trainer's own delivery of the course. Absent unless they are approved to train it.")
public record CourseStatsScopedDTO(

        @Schema(description = "Distinct learners the caller has taught on this course.", example = "48")
        @JsonProperty("your_learners")
        long yourLearners,

        @Schema(description = "The caller's active classes for this course.", example = "2")
        @JsonProperty("your_classes")
        long yourClasses,

        @Schema(description = "Credited to the caller for captured sales of their own classes.", example = "126500.00")
        @JsonProperty("your_earnings")
        BigDecimal yourEarnings
) {
}
