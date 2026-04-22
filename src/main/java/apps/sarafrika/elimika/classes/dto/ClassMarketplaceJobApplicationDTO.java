package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassMarketplaceJobApplication",
        description = "Instructor application to deliver a marketplace class job"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobApplicationDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty(value = "job_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID jobUuid,

        @JsonProperty(value = "instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID instructorUuid,

        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        ClassMarketplaceJobApplicationStatus status,

        @JsonProperty(value = "application_note", access = JsonProperty.Access.READ_ONLY)
        String applicationNote,

        @JsonProperty(value = "review_notes", access = JsonProperty.Access.READ_ONLY)
        String reviewNotes,

        @JsonProperty(value = "reviewed_by", access = JsonProperty.Access.READ_ONLY)
        String reviewedBy,

        @JsonProperty(value = "reviewed_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime reviewedAt,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
