package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Course Creator Skill DTO mirrors instructor skill representation so qualification data stays consistent.
 */
@Schema(
        name = "CourseCreatorSkill",
        description = "Technical or creative competency declared by a course creator with proficiency metadata",
        example = """
        {
            "uuid": "skill123-4567-89ab-cdef-0123456789ab",
            "course_creator_uuid": "c1r2e3a4-5t6o-7r89-0abc-defghijklmno",
            "skill_name": "Instructional Design",
            "proficiency_level": "EXPERT",
            "created_date": "2024-06-15T14:30:22",
            "created_by": "creator@example.com",
            "updated_date": "2024-06-16T09:15:00",
            "updated_by": "creator@example.com"
        }
        """
)
public record CourseCreatorSkillDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotBlank(message = "Skill name is required")
        @Size(max = 100, message = "Skill name must not exceed 100 characters")
        @JsonProperty("skill_name")
        String skillName,

        @NotNull(message = "Proficiency level is required")
        @JsonProperty("proficiency_level")
        ProficiencyLevel proficiencyLevel,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

        @JsonProperty(value = "display_name", access = JsonProperty.Access.READ_ONLY)
        public String getDisplayName() {
                if (skillName == null) {
                        return null;
                }
                if (proficiencyLevel == null) {
                        return skillName;
                }
                return skillName + " (" + proficiencyLevel.name().toLowerCase().replace("_", " ") + ")";
        }

        @JsonProperty(value = "proficiency_description", access = JsonProperty.Access.READ_ONLY)
        public String getProficiencyDescription() {
                if (proficiencyLevel == null) {
                        return "Proficiency not specified";
                }
                return switch (proficiencyLevel) {
                        case BEGINNER -> "Emerging skill level with foundational exposure";
                        case INTERMEDIATE -> "Working proficiency with hands-on delivery experience";
                        case ADVANCED -> "Advanced practitioner capable of leading complex initiatives";
                        case EXPERT -> "Expert who can mentor others and set quality standards";
                };
        }
}
