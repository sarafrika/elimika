package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One approved trainer on a course's delivery list.
 * <p>
 * {@code rate_card} is <em>absent</em>, not null and not empty, for anyone but the course creator
 * and platform administrators — the whole record is {@code NON_NULL}, so a viewer who may not see
 * what a trainer charges receives JSON with no {@code rate_card} key at all. The figures are not
 * even loaded on that path: see
 * {@link apps.sarafrika.elimika.course.repository.projection.CourseTrainerView}.
 * <p>
 * {@code location} is a place in words — a town, optionally qualified — and never coordinates.
 */
@Schema(
        name = "CourseTrainerSummary",
        description = "An instructor or organisation approved to deliver a course",
        example = """
        {
            "applicant_type": "organisation",
            "applicant_uuid": "b7f3c1de-1f4a-4a24-9d55-9c1c2e5c0f11",
            "display_name": "Westlands Training Institute",
            "location": "Westlands",
            "approved_at": "2026-02-14T09:30:00",
            "active_class_count": 3
        }
        """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseTrainerSummaryDTO(

        @Schema(description = "Whether the trainer is an instructor or an organisation.", example = "organisation")
        @JsonProperty("applicant_type")
        CourseTrainingApplicantType applicantType,

        @Schema(description = "Identifier of the approved instructor or organisation.",
                example = "b7f3c1de-1f4a-4a24-9d55-9c1c2e5c0f11")
        @JsonProperty("applicant_uuid")
        UUID applicantUuid,

        @Schema(description = "The trainer's name as it should be shown.", example = "Westlands Training Institute")
        @JsonProperty("display_name")
        String displayName,

        @Schema(description = "Where the trainer operates, in words. Absent when they have not said.",
                example = "Westlands", nullable = true)
        @JsonProperty("location")
        String location,

        @Schema(description = "When the training application was approved. Absent on records approved before this was captured.",
                format = "date-time", nullable = true)
        @JsonProperty("approved_at")
        LocalDateTime approvedAt,

        @Schema(description = "How many active classes the trainer currently runs on this course.", example = "3")
        @JsonProperty("active_class_count")
        long activeClassCount,

        @Schema(description = "**[COURSE OWNER AND PLATFORM ADMIN ONLY]** What the trainer charges. Absent for every other caller.",
                nullable = true)
        @JsonProperty("rate_card")
        CourseTrainingRateCardDTO rateCard
) {
}
