package apps.sarafrika.elimika.instructor.dto;

import apps.sarafrika.elimika.tenancy.dto.ProfessionalBodyDTO;
import apps.sarafrika.elimika.tenancy.dto.TrainingExperienceDTO;
import apps.sarafrika.elimika.tenancy.dto.UserCertificationDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing an Instructor's information.
 * This record encapsulates all details related to an instructor including personal info,
 * location, professional background, training experience, and certifications.
 */
@Schema(description = "Data Transfer Object for Instructor information")
public record InstructorDTO(
        @Schema(description = "Unique identifier for the instructor")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Reference to the associated user account")
        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(description = "Full name of the instructor", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("full_name")
        String fullName,

        @Schema(description = "Latitude coordinate of instructor's location")
        @JsonProperty("latitude")
        BigDecimal latitude,

        @Schema(description = "Longitude coordinate of instructor's location")
        @JsonProperty("longitude")
        BigDecimal longitude,

        @Schema(description = "Website or portfolio URL of the instructor")
        @JsonProperty("website")
        String website,

        @Schema(description = "Biography or personal description of the instructor")
        @JsonProperty("bio")
        String bio,

        @Schema(description = "Professional headline or title of the instructor")
        @JsonProperty("professional_headline")
        String professionalHeadline,

        @Schema(description = "Professional body affiliations of the instructor")
        @JsonProperty("professional_bodies")
        @Valid
        List<ProfessionalBodyDTO> professionalBodies,

        @Schema(description = "Training experience details of the instructor")
        @JsonProperty("training_experiences")
        @Valid
        List<TrainingExperienceDTO> trainingExperiences,

        @Schema(description = "Certifications held by the instructor")
        @JsonProperty("certifications")
        @Valid
        List<UserCertificationDTO> certifications,

        @Schema(description = "Timestamp when the record was created", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "User who created the record", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(description = "Timestamp when the record was last updated", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(description = "User who last updated the record", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}