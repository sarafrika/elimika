package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * One job matched to the current instructor: the job exactly as the listing shows it, plus
 * {@code match}. Only ever returned to the instructor it was matched for.
 */
@Schema(name = "JobMatch", description = "A marketplace job matched to the current instructor, with its fit")
public record JobMatchDTO(
        @JsonUnwrapped
        ClassMarketplaceJobDTO job,

        @Schema(description = "Why and how well the job fits")
        @JsonProperty(value = "match", access = JsonProperty.Access.READ_ONLY)
        Match match
) {

    @Schema(name = "JobMatchDetails")
    public record Match(
            @Schema(description = "Fit score 0..1 (rules-v1)")
            @JsonProperty(value = "score", access = JsonProperty.Access.READ_ONLY)
            double score,

            @Schema(description = "Required skills the instructor holds at or above the minimum proficiency")
            @JsonProperty(value = "matched_skills", access = JsonProperty.Access.READ_ONLY)
            List<Skill> matchedSkills,

            @Schema(description = "The job's effective required skills (its own tags, or its course's)")
            @JsonProperty(value = "required_skills", access = JsonProperty.Access.READ_ONLY)
            List<Skill> requiredSkills,

            @Schema(description = "Plain-language reasons, strongest first")
            @JsonProperty(value = "reasons", access = JsonProperty.Access.READ_ONLY)
            List<String> reasons,

            @Schema(description = "The same answer as GET /api/v1/classes/jobs/{jobUuid}/eligibility; ineligible jobs are listed last")
            @JsonProperty(value = "eligibility", access = JsonProperty.Access.READ_ONLY)
            ClassMarketplaceJobEligibilityDTO eligibility
    ) {
    }

    @Schema(name = "JobMatchSkill")
    public record Skill(
            @JsonProperty(value = "skill_uuid", access = JsonProperty.Access.READ_ONLY)
            UUID skillUuid,

            @JsonProperty(value = "skill_name", access = JsonProperty.Access.READ_ONLY)
            String skillName,

            @JsonProperty(value = "min_proficiency", access = JsonProperty.Access.READ_ONLY)
            ProficiencyLevel minProficiency,

            @JsonProperty(value = "is_mandatory", access = JsonProperty.Access.READ_ONLY)
            boolean mandatory
    ) {
    }

    /** A page of matches with the id the client reports clicks and dismissals against. */
    @Schema(name = "JobMatchList")
    public record Page(
            @Schema(description = "Send back as recommendation_id to POST /api/v1/discovery/events")
            @JsonProperty(value = "recommendation_id", access = JsonProperty.Access.READ_ONLY)
            UUID recommendationId,

            @JsonProperty(value = "model_version", access = JsonProperty.Access.READ_ONLY)
            String modelVersion,

            @JsonProperty(value = "items", access = JsonProperty.Access.READ_ONLY)
            List<JobMatchDTO> items
    ) {
    }
}
