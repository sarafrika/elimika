package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Represents a rate card that captures instructor pricing across session format and delivery modality.
 */
@Schema(name = "CourseTrainingRateCard")
public record CourseTrainingRateCardDTO(

        @Schema(
                description = "**[OPTIONAL]** ISO currency applied to every rate entry in the card. Defaults to the platform currency when omitted.",
                example = "KES",
                maxLength = 3,
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Rate currency must be a 3-letter ISO code")
        @JsonProperty("currency")
        String currency,

        @Schema(description = "1:1 private session rate when delivered online, per learner per hour.", example = "3500.0000")
        @NotNull(message = "Private online rate is required")
        @DecimalMin(value = "0.0000", message = "Private online rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Private online rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_rate")
        BigDecimal privateOnlineRate,

        @Schema(description = "1:1 private session rate when delivered in person, per learner per hour.", example = "3600.0000")
        @NotNull(message = "Private in-person rate is required")
        @DecimalMin(value = "0.0000", message = "Private in-person rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Private in-person rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_rate")
        BigDecimal privateInpersonRate,

        @Schema(description = "Group session rate when delivered online, per learner per hour.", example = "2800.0000")
        @NotNull(message = "Group online rate is required")
        @DecimalMin(value = "0.0000", message = "Group online rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Group online rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_rate")
        BigDecimal groupOnlineRate,

        @Schema(description = "Group session rate when delivered in person, per learner per hour.", example = "3000.0000")
        @NotNull(message = "Group in-person rate is required")
        @DecimalMin(value = "0.0000", message = "Group in-person rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Group in-person rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_rate")
        BigDecimal groupInpersonRate
) {

    public BigDecimal resolveRate(SessionFormat format, LocationType locationType) {
        boolean online = LocationType.ONLINE.equals(locationType);
        boolean inPerson = LocationType.IN_PERSON.equals(locationType) || LocationType.HYBRID.equals(locationType);

        if (!online && !inPerson) {
            // default to online pricing when location type is unspecified
            online = true;
        }

        return switch (format) {
            case INDIVIDUAL -> online ? privateOnlineRate : privateInpersonRate;
            case GROUP -> online ? groupOnlineRate : groupInpersonRate;
        };
    }
}
