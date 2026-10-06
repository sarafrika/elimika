package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** A framework competency with evidence, verified by a platform admin. */
@Schema(name = "UserCompetency", description = "A competency in the user-owned skills wallet")
public record UserCompetencyDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank @Size(max = 255) @JsonProperty("competency") String competency,
        @Size(max = 255) @JsonProperty("framework") String framework,
        @Min(1) @Max(5) @JsonProperty("level") Integer level,
        @JsonProperty("evidence") String evidence,
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
