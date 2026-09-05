package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The course statistics anybody signed in may read.
 * <p>
 * Everything here describes the course as a whole. Nothing here is per-trainer, and nothing here is
 * money: an approved trainer's takings and the creator's sales live in their own blocks, which the
 * server omits entirely from a response that is not entitled to them.
 */
@Schema(name = "CourseStatsPublic", description = "Course-wide performance figures, readable by any signed-in caller.")
public record CourseStatsPublicDTO(

        @Schema(description = "Distinct learners who have held a place on any of the course's classes.", example = "412")
        @JsonProperty("learners_trained")
        long learnersTrained,

        @Schema(description = "Active class definitions currently delivering the course.", example = "7")
        @JsonProperty("classes_running")
        long classesRunning,

        @Schema(description = """
                Mean seat fill across the running classes, as a percentage rounded to the nearest 5.

                The seat counts behind it are deliberately not published. Filled seats and total
                seats printed beside a course's price make gross revenue a multiplication, so the
                ratio leaves the server already coarsened and the operands never leave at all.
                """, example = "75")
        @JsonProperty("average_class_fill")
        int averageClassFill,

        @Schema(description = "Share of enrolments that reached completion, as a percentage.", example = "68.4")
        @JsonProperty("completion_rate")
        double completionRate,

        @Schema(description = "Mean learner rating out of 5, or 0 when the course has no reviews.", example = "4.6")
        @JsonProperty("average_rating")
        double averageRating,

        @Schema(description = "Number of learner reviews.", example = "37")
        @JsonProperty("total_reviews")
        long totalReviews,

        @Schema(description = "Instructors and organisations approved to deliver the course.", example = "5")
        @JsonProperty("approved_trainer_count")
        long approvedTrainerCount
) {
}
