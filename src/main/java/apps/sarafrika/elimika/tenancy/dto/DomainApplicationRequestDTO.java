package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "DomainApplicationRequest", description = "A signed-in account asking for another domain")
public record DomainApplicationRequestDTO(

        @NotBlank
        @Schema(example = "instructor",
                allowableValues = {"student", "instructor", "course_creator", "organisation_user"})
        @JsonProperty("domain")
        String domain
) {
}
