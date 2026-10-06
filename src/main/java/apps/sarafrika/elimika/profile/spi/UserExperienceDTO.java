package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A work, training, volunteering or project experience on the user's professional profile. */
@Schema(name = "UserExperience", description = "An experience record on the user-owned professional profile")
public record UserExperienceDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank(message = "Position is required")
        @Size(max = 255) @JsonProperty("position") String position,
        @NotBlank(message = "Organisation name is required")
        @Size(max = 255) @JsonProperty("organisation_name") String organizationName,
        @JsonProperty("responsibilities") String responsibilities,
        @DecimalMin("0.0") @DecimalMax("60.0") @JsonProperty("years_of_experience") BigDecimal yearsOfExperience,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("is_current_position") Boolean isCurrentPosition,
        @JsonProperty("experience_type") ExperienceType experienceType,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {
}
