package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "CourseRatingSummary",
        description = "Aggregate review metrics for a course, as shown on a course list item.",
        example = """
        {
          "average": 4.6,
          "count": 18
        }
        """
)
public record CourseRatingSummary(

        @Schema(
                description = "Average review rating (1-5), rounded to one decimal. Null when there are no reviews.",
                example = "4.6",
                nullable = true
        )
        @JsonProperty("average")
        Double average,

        @Schema(description = "Number of reviews for the course.", example = "18")
        @JsonProperty("count")
        long count
) {

    public static final CourseRatingSummary NONE = new CourseRatingSummary(null, 0);
}
