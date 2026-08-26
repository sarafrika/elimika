package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(
        name = "ClassMarketplaceJobDecisionRequest",
        description = "Organisation review notes and optional interview scheduling details for an instructor application"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobDecisionRequestDTO(

        @JsonProperty("review_notes")
        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes,

        @JsonProperty("interview_at")
        @FutureOrPresent(message = "Interview date must be now or in the future")
        LocalDateTime interviewAt
) {
}
