package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.TrainingRequirementAcquisition;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** A stored answer to a training requirement, with the requirement's current name. */
@Schema(name = "TrainingRequirementAnswer", description = "The applicant's answer to one training requirement")
public record TrainingRequirementAnswerDTO(

        @Schema(description = "**[READ-ONLY]** The training requirement answered.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "requirement_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID requirementUuid,

        @Schema(description = "**[READ-ONLY]** The requirement's name; null if it has since been removed from the course.",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "requirement_name", access = JsonProperty.Access.READ_ONLY)
        String requirementName,

        @Schema(description = "**[READ-ONLY]** Whether the applicant has it.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "has_it", access = JsonProperty.Access.READ_ONLY)
        boolean hasIt,

        @Schema(description = "**[READ-ONLY]** How it would be obtained; null when has_it is true.",
                allowableValues = {"lease", "hire"}, nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "acquisition", access = JsonProperty.Access.READ_ONLY)
        TrainingRequirementAcquisition acquisition
) {
}
