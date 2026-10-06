package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A certificate, badge, award or external credential in the user's wallet. */
@Schema(name = "UserCertification", description = "A credential in the user-owned skills wallet")
public record UserCertificationDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank(message = "Certification name is required")
        @Size(max = 255) @JsonProperty("certification_name") String certificationName,
        @NotBlank(message = "Issuing organisation is required")
        @Size(max = 255) @JsonProperty("issuing_organization") String issuingOrganization,
        @JsonProperty("issued_date") LocalDate issuedDate,
        @JsonProperty("expiry_date") LocalDate expiryDate,
        @Size(max = 120) @JsonProperty("credential_id") String credentialId,
        @Size(max = 500) @JsonProperty("credential_url") String credentialUrl,
        @JsonProperty("description") String description,
        @JsonProperty("credential_type") CredentialType credentialType,
        @JsonProperty(value = "verification_status", access = JsonProperty.Access.READ_ONLY)
        WalletVerificationStatus verificationStatus,
        @JsonProperty(value = "verified_at", access = JsonProperty.Access.READ_ONLY) LocalDateTime verifiedAt,
        @JsonProperty(value = "verification_notes", access = JsonProperty.Access.READ_ONLY) String verificationNotes,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {
}
