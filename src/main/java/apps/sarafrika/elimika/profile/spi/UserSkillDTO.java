package apps.sarafrika.elimika.profile.spi;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A skill in the user's wallet; its verification holds for every domain the user has. */
@Schema(name = "UserSkill", description = "A skill in the user-owned skills wallet")
public record UserSkillDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank(message = "Skill name is required")
        @Size(max = 255, message = "Skill name must not exceed 255 characters")
        @JsonProperty("skill_name") String skillName,
        @JsonProperty(value = "skill_uuid", access = JsonProperty.Access.READ_ONLY) UUID skillUuid,
        @JsonProperty("proficiency_level") ProficiencyLevel proficiencyLevel,
        @JsonProperty("evidence") String evidence,
        @JsonProperty("last_assessed_on") LocalDate lastAssessedOn,
        @JsonProperty(value = "verification_status", access = JsonProperty.Access.READ_ONLY)
        WalletVerificationStatus verificationStatus,
        @JsonProperty(value = "verified_at", access = JsonProperty.Access.READ_ONLY) LocalDateTime verifiedAt,
        @JsonProperty(value = "verification_notes", access = JsonProperty.Access.READ_ONLY) String verificationNotes,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {

    /** A claim to write: only the fields an owner may set. */
    public static UserSkillDTO claim(String skillName, ProficiencyLevel proficiencyLevel, String evidence,
                                     LocalDate lastAssessedOn) {
        return new UserSkillDTO(null, null, skillName, null, proficiencyLevel, evidence, lastAssessedOn,
                null, null, null, null, null, null, null);
    }
}
