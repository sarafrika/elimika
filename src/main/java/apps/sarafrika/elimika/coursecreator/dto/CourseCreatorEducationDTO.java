package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

/**
 * Educational qualification record for a course creator.
 */
@Schema(
        name = "CourseCreatorEducation",
        description = "Academic credentials captured for course creators",
        example = """
        {
            "uuid": "edu12345-6789-abcd-ef01-234567890abc",
            "course_creator_uuid": "c1r2e3a4-5t6o-7r89-0abc-defghijklmno",
            "qualification": "Master of Education",
            "school_name": "Strathmore University",
            "year_completed": 2021,
            "certificate_number": "MEd-2021-0099"
        }
        """
)
public record CourseCreatorEducationDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotBlank(message = "Qualification is required")
        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        @JsonProperty("qualification")
        String qualification,

        @NotBlank(message = "School name is required")
        @Size(max = 255, message = "School name must not exceed 255 characters")
        @JsonProperty("school_name")
        String schoolName,

        @Min(value = 1950, message = "Year completed must be 1950 or later")
        @Max(value = 2100, message = "Year completed must be realistic")
        @JsonProperty("year_completed")
        Integer yearCompleted,

        @Size(max = 100, message = "Certificate number must not exceed 100 characters")
        @JsonProperty("certificate_number")
        String certificateNumber,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

        @JsonProperty(value = "is_recent_qualification", access = JsonProperty.Access.READ_ONLY)
        public boolean isRecentQualification() {
                if (yearCompleted == null) {
                        return false;
                }
                return Year.now().getValue() - yearCompleted <= 10;
        }

        @JsonProperty(value = "formatted_completion", access = JsonProperty.Access.READ_ONLY)
        public String getFormattedCompletion() {
                if (yearCompleted == null || schoolName == null) {
                        return qualification;
                }
                return yearCompleted + " - " + schoolName;
        }
}
