package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.TrainingRequirementAcquisition;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The applicant's answer to one of the course's training requirements. */
@Schema(name = "TrainingRequirementAnswerRequest", description = "Whether the applicant has a training requirement, and if not how they would obtain it")
public record TrainingRequirementAnswerRequest(

        @Schema(description = "**[REQUIRED]** A training requirement of the course (for programs, of one of its courses).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("requirement_uuid")
        @NotNull(message = "requirement_uuid is required")
        UUID requirementUuid,

        @Schema(description = "**[REQUIRED]** Whether the applicant already has it.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("has_it")
        @NotNull(message = "has_it is required")
        Boolean hasIt,

        @Schema(description = "How the applicant would obtain it: required when has_it is false, ignored when true.",
                allowableValues = {"lease", "hire"}, nullable = true)
        @JsonProperty("acquisition")
        TrainingRequirementAcquisition acquisition
) {
}
