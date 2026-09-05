package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Who is approved to deliver a course.
 * <p>
 * {@code pending_count} is the course creator's own operational figure — how many applications are
 * still waiting on them — so, like the rate cards inside {@code trainers}, it is absent for every
 * other caller rather than zero. An absent count means "not yours to see"; it never means "none".
 */
@Schema(
        name = "CourseTrainerDirectory",
        description = "The approved delivery list for a course, plus the creator's pending queue"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseTrainerDirectoryDTO(

        @Schema(description = "Trainers approved to deliver this course.")
        @JsonProperty("trainers")
        List<CourseTrainerSummaryDTO> trainers,

        @Schema(description = "**[COURSE OWNER AND PLATFORM ADMIN ONLY]** Applications still awaiting a decision. Absent for every other caller.",
                example = "2", nullable = true)
        @JsonProperty("pending_count")
        Long pendingCount
) {
}
