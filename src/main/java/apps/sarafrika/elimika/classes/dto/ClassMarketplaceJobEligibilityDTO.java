package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ClassMarketplaceJobEligibility",
        description = "Current instructor's eligibility to apply for a marketplace class job"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobEligibilityDTO(

        @Schema(description = "Whether the current instructor can apply for this job")
        @JsonProperty(value = "eligible", access = JsonProperty.Access.READ_ONLY)
        boolean eligible,

        @Schema(description = "Whether the instructor profile has been verified by an administrator")
        @JsonProperty(value = "instructor_verified", access = JsonProperty.Access.READ_ONLY)
        boolean instructorVerified,

        @Schema(description = "Whether the instructor is approved to deliver the job's course or training program")
        @JsonProperty(value = "training_approved", access = JsonProperty.Access.READ_ONLY)
        boolean trainingApproved,

        @Schema(description = "Whether the instructor already has an application for this job")
        @JsonProperty(value = "already_applied", access = JsonProperty.Access.READ_ONLY)
        boolean alreadyApplied,

        @Schema(description = "Whether the instructor's existing schedule is free for every session of this job")
        @JsonProperty(value = "schedule_clear", access = JsonProperty.Access.READ_ONLY)
        boolean scheduleClear,

        @Schema(description = "Job session occurrences that clash with the instructor's existing schedule, with reasons", nullable = true)
        @JsonProperty(value = "schedule_conflicts", access = JsonProperty.Access.READ_ONLY)
        java.util.List<ClassSchedulingConflictDTO> scheduleConflicts,

        @Schema(description = "Human-readable explanation when the instructor is not eligible", nullable = true)
        @JsonProperty(value = "reason", access = JsonProperty.Access.READ_ONLY)
        String reason
) {
}
