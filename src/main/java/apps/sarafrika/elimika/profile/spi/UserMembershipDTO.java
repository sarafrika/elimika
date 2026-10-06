package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A professional body membership on the user's professional profile. */
@Schema(name = "UserMembership", description = "A professional membership on the user-owned professional profile")
public record UserMembershipDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotBlank(message = "Organisation name is required")
        @Size(max = 255) @JsonProperty("organisation_name") String organizationName,
        @Size(max = 100) @JsonProperty("membership_number") String membershipNumber,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {
}
