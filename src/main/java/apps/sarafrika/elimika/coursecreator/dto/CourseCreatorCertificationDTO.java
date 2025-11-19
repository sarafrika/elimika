package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.shared.utils.validation.ValidUrl;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Certification or accreditation record for a course creator.
 */
@Schema(
        name = "CourseCreatorCertification",
        description = "Professional certification or accreditation evidence associated with a course creator",
        example = """
        {
            "uuid": "cert1234-5678-abcd-ef01-234567890abc",
            "course_creator_uuid": "c1r2e3a4-5t6o-7r89-0abc-defghijklmno",
            "certification_name": "Adobe Captivate Specialist",
            "issuing_organization": "Adobe",
            "issued_date": "2023-04-01",
            "expiry_date": "2025-04-01",
            "credential_id": "ADCAP-2023-8891",
            "credential_url": "https://verify.example.com/ADCAP-2023-8891",
            "description": "Validated expertise in creating immersive digital learning content.",
            "is_verified": true
        }
        """
)
public record CourseCreatorCertificationDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotBlank(message = "Certification name is required")
        @Size(max = 255, message = "Certification name must not exceed 255 characters")
        @JsonProperty("certification_name")
        String certificationName,

        @NotBlank(message = "Issuing organization is required")
        @Size(max = 255, message = "Issuing organization must not exceed 255 characters")
        @JsonProperty("issuing_organization")
        String issuingOrganization,

        @JsonProperty("issued_date")
        LocalDate issuedDate,

        @JsonProperty("expiry_date")
        LocalDate expiryDate,

        @Size(max = 120, message = "Credential ID must not exceed 120 characters")
        @JsonProperty("credential_id")
        String credentialId,

        @Size(max = 500, message = "Credential URL must not exceed 500 characters")
        @ValidUrl
        @JsonProperty("credential_url")
        String credentialUrl,

        @JsonProperty("description")
        String description,

        @JsonProperty("is_verified")
        Boolean isVerified,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

        @JsonProperty(value = "is_expired", access = JsonProperty.Access.READ_ONLY)
        public boolean isExpired() {
                return expiryDate != null && expiryDate.isBefore(LocalDate.now());
        }
}
