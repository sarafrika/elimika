package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * One instructor suggested for an organisation's job. A fit summary only: never the instructor's
 * rate, their diary, or the number or times of any clash.
 */
@Schema(name = "JobCandidate", description = "A verified, approved instructor suggested for a marketplace job")
public record JobCandidateDTO(
        @JsonProperty(value = "instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID instructorUuid,

        @JsonProperty(value = "display_name", access = JsonProperty.Access.READ_ONLY)
        String displayName,

        @JsonProperty(value = "location_name", access = JsonProperty.Access.READ_ONLY)
        String locationName,

        @JsonProperty(value = "admin_verified", access = JsonProperty.Access.READ_ONLY)
        boolean adminVerified,

        @JsonProperty(value = "match", access = JsonProperty.Access.READ_ONLY)
        Match match
) {

    @Schema(name = "JobCandidateMatch")
    public record Match(
            @Schema(description = "Fit score 0..1 (rules-v1)")
            @JsonProperty(value = "score", access = JsonProperty.Access.READ_ONLY)
            double score,

            @Schema(description = "Plain-language reasons; never mention rates or clashes")
            @JsonProperty(value = "reasons", access = JsonProperty.Access.READ_ONLY)
            List<String> reasons,

            @Schema(description = "Whether every session of the job is free in the instructor's schedule")
            @JsonProperty(value = "schedule_clear", access = JsonProperty.Access.READ_ONLY)
            boolean scheduleClear,

            @Schema(description = "Whether the job's pay covers the instructor's approved rate")
            @JsonProperty(value = "rate_within_budget", access = JsonProperty.Access.READ_ONLY)
            boolean rateWithinBudget
    ) {
    }

    @Schema(name = "JobCandidateList")
    public record Page(
            @Schema(description = "Send back as recommendation_id to POST /api/v1/discovery/events")
            @JsonProperty(value = "recommendation_id", access = JsonProperty.Access.READ_ONLY)
            UUID recommendationId,

            @JsonProperty(value = "model_version", access = JsonProperty.Access.READ_ONLY)
            String modelVersion,

            @JsonProperty(value = "job_uuid", access = JsonProperty.Access.READ_ONLY)
            UUID jobUuid,

            @JsonProperty(value = "items", access = JsonProperty.Access.READ_ONLY)
            List<JobCandidateDTO> items
    ) {
    }
}
