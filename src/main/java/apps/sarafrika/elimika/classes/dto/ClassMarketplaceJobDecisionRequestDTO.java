package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ClassMarketplaceJobDecisionRequest",
        description = "Organisation review notes when approving or rejecting an instructor application"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobDecisionRequestDTO(

        @JsonProperty("review_notes")
        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes
) {
}
