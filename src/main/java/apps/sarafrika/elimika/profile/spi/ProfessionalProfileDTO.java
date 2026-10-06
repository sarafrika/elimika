package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Professional basics one user shares across every domain they hold. */
@Schema(name = "ProfessionalProfile", description = "User-owned professional basics shared by every domain")
public record ProfessionalProfileDTO(
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID userUuid,

        @Size(max = 10000, message = "Bio must not exceed 10000 characters")
        @JsonProperty("bio")
        String bio,

        @Size(max = 500, message = "Professional headline must not exceed 500 characters")
        @JsonProperty("professional_headline")
        String professionalHeadline,

        @Size(max = 500, message = "Website must not exceed 500 characters")
        @JsonProperty("website")
        String website,

        @Size(max = 255, message = "Location name must not exceed 255 characters")
        @JsonProperty("location_name")
        String locationName,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        @JsonProperty("latitude")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        @JsonProperty("longitude")
        BigDecimal longitude,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate
) {

    public static ProfessionalProfileDTO empty(UUID userUuid) {
        return new ProfessionalProfileDTO(userUuid, null, null, null, null, null, null, null);
    }

    /** These values, with every null taken from {@code fallback}. */
    public ProfessionalProfileDTO orElse(ProfessionalProfileDTO fallback) {
        if (fallback == null) {
            return this;
        }
        return new ProfessionalProfileDTO(
                userUuid != null ? userUuid : fallback.userUuid(),
                bio != null ? bio : fallback.bio(),
                professionalHeadline != null ? professionalHeadline : fallback.professionalHeadline(),
                website != null ? website : fallback.website(),
                locationName != null ? locationName : fallback.locationName(),
                latitude != null ? latitude : fallback.latitude(),
                longitude != null ? longitude : fallback.longitude(),
                updatedDate != null ? updatedDate : fallback.updatedDate());
    }

    /** Bio and headline both filled: what profile-completion reminders look for. */
    @JsonProperty(value = "basics_complete", access = JsonProperty.Access.READ_ONLY)
    public boolean isBasicsComplete() {
        return bio != null && !bio.isBlank() && professionalHeadline != null && !professionalHeadline.isBlank();
    }
}
