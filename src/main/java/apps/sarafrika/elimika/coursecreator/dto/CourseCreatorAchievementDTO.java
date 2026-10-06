package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.profile.spi.AchievementType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CourseCreatorAchievement", description = "An award, milestone or recognition in the skills wallet")
public record CourseCreatorAchievementDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty(value = "course_creator_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseCreatorUuid,

        @NotBlank
        @Size(max = 255)
        @JsonProperty("title")
        String title,

        @NotNull
        @JsonProperty("achievement_type")
        AchievementType achievementType,

        @Size(max = 255)
        @JsonProperty("awarded_by")
        String awardedBy,

        @JsonProperty("awarded_on")
        LocalDate awardedOn,

        @JsonProperty("description")
        String description,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
