package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ClassMarketplaceJobApplicationRequest",
        description = "Application submitted by an instructor against a marketplace class job"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobApplicationRequestDTO(

        @Schema(description = "Optional note to support the instructor application.", nullable = true)
        @JsonProperty("application_note")
        @Size(max = 2000, message = "Application note must not exceed 2000 characters")
        String applicationNote
) {
}
