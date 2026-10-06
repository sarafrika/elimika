package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** An education record on the user's professional profile. */
@Schema(name = "UserEducation", description = "An education record on the user-owned professional profile")
public record UserEducationDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank(message = "Qualification is required")
        @Size(max = 255) @JsonProperty("qualification") String qualification,
        @Size(max = 255) @JsonProperty("field_of_study") String fieldOfStudy,
        @NotBlank(message = "School name is required")
        @Size(max = 255) @JsonProperty("school_name") String schoolName,
        @Min(1950) @Max(2100) @JsonProperty("start_year") Integer startYear,
        @Min(1950) @Max(2100) @JsonProperty("year_completed") Integer yearCompleted,
        @Size(max = 100) @JsonProperty("certificate_number") String certificateNumber,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {
}
