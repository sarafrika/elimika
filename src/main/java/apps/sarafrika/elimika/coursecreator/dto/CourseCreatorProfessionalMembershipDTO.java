package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Professional membership or affiliation captured for a course creator.
 */
@Schema(
        name = "CourseCreatorProfessionalMembership",
        description = "Membership information for industry bodies or associations that endorse the course creator",
        example = """
        {
            "uuid": "memb1234-5678-abcd-ef01-234567890abc",
            "course_creator_uuid": "c1r2e3a4-5t6o-7r89-0abc-defghijklmno",
            "organization_name": "International Society for Technology in Education",
            "membership_number": "ISTE-2024-991",
            "start_date": "2022-01-01",
            "end_date": null,
            "is_active": true
        }
        """
)
public record CourseCreatorProfessionalMembershipDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotBlank(message = "Organization name is required")
        @Size(max = 255, message = "Organization name must not exceed 255 characters")
        @JsonProperty("organization_name")
        String organizationName,

        @Size(max = 100, message = "Membership number must not exceed 100 characters")
        @JsonProperty("membership_number")
        String membershipNumber,

        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("end_date")
        LocalDate endDate,

        @JsonProperty("is_active")
        Boolean isActive,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

        @JsonProperty(value = "status_label", access = JsonProperty.Access.READ_ONLY)
        public String getStatusLabel() {
                if (Boolean.TRUE.equals(isActive)) {
                        return "Active";
                }
                return "Inactive";
        }
}
