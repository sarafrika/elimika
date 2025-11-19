package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Professional experience history for a course creator.
 */
@Schema(
        name = "CourseCreatorExperience",
        description = "Work history and practical delivery background for course creators",
        example = """
        {
            "uuid": "exp12345-6789-abcd-ef01-234567890abc",
            "course_creator_uuid": "c1r2e3a4-5t6o-7r89-0abc-defghijklmno",
            "position": "Lead Content Strategist",
            "organization_name": "Digital Learning Labs",
            "responsibilities": "Designed blended learning experiences for enterprise teams.",
            "years_of_experience": 5.5,
            "start_date": "2019-01-01",
            "end_date": null,
            "is_current_position": true
        }
        """
)
public record CourseCreatorExperienceDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotBlank(message = "Position is required")
        @Size(max = 255, message = "Position must not exceed 255 characters")
        @JsonProperty("position")
        String position,

        @NotBlank(message = "Organization name is required")
        @Size(max = 255, message = "Organization name must not exceed 255 characters")
        @JsonProperty("organization_name")
        String organizationName,

        @JsonProperty("responsibilities")
        String responsibilities,

        @DecimalMin(value = "0.0", message = "Years of experience must be positive")
        @DecimalMax(value = "60.0", message = "Years of experience must be realistic")
        @JsonProperty("years_of_experience")
        BigDecimal yearsOfExperience,

        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("end_date")
        LocalDate endDate,

        @JsonProperty("is_current_position")
        Boolean isCurrentPosition,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

        @JsonProperty(value = "tenure_label", access = JsonProperty.Access.READ_ONLY)
        public String getTenureLabel() {
                if (startDate == null) {
                        return null;
                }
                String end = Boolean.TRUE.equals(isCurrentPosition) || endDate == null
                        ? "Present"
                        : endDate.toString();
                return startDate + " - " + end;
        }
}
