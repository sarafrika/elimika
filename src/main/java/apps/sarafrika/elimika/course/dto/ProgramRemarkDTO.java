package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.model.ProgramEnrollmentRemark;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "ProgramRemark", description = "An instructor's progress remark on a learner's programme; rich text (HTML)")
public record ProgramRemarkDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Size(max = 20000)
        @JsonProperty("strengths")
        String strengths,

        @Size(max = 20000)
        @JsonProperty("improvements")
        String improvements,

        @Size(max = 20000)
        @JsonProperty("recommendations")
        String recommendations,

        @JsonProperty("next_review_date")
        LocalDate nextReviewDate,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "author_user_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID authorUserUuid,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Display name of the remark's author")
        @JsonProperty(value = "author_name", access = JsonProperty.Access.READ_ONLY)
        String authorName,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
    public static ProgramRemarkDTO from(ProgramEnrollmentRemark remark, String authorName) {
        return new ProgramRemarkDTO(remark.getUuid(), remark.getStrengths(), remark.getImprovements(),
                remark.getRecommendations(), remark.getNextReviewDate(), remark.getAuthorUserUuid(), authorName,
                remark.getCreatedDate());
    }
}
