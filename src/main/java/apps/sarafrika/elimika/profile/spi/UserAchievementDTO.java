package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** An award, milestone or recognition in the user's wallet. */
@Schema(name = "UserAchievement", description = "An achievement in the user-owned skills wallet")
public record UserAchievementDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank @Size(max = 255) @JsonProperty("title") String title,
        @NotNull @JsonProperty("achievement_type") AchievementType achievementType,
        @Size(max = 255) @JsonProperty("awarded_by") String awardedBy,
        @JsonProperty("awarded_on") LocalDate awardedOn,
        @JsonProperty("description") String description,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {
}
