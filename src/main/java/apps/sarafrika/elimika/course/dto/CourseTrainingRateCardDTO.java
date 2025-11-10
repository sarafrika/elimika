package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
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
 * Represents a rate card that captures instructor pricing across private/public and individual/group sessions.
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

        @Schema(description = "Private 1:1 session rate per learner per hour.", example = "3500.0000")
        @NotNull(message = "Private individual rate is required")
        @DecimalMin(value = "0.0000", message = "Private individual rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Private individual rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_individual_rate")
        BigDecimal privateIndividualRate,

        @Schema(description = "Private group session rate per learner per hour.", example = "2800.0000")
        @NotNull(message = "Private group rate is required")
        @DecimalMin(value = "0.0000", message = "Private group rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Private group rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_group_rate")
        BigDecimal privateGroupRate,

        @Schema(description = "Public individual rate per learner per hour.", example = "3000.0000")
        @NotNull(message = "Public individual rate is required")
        @DecimalMin(value = "0.0000", message = "Public individual rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Public individual rate must have at most 8 digits and 4 decimals")
        @JsonProperty("public_individual_rate")
        BigDecimal publicIndividualRate,

        @Schema(description = "Public group rate per learner per hour.", example = "2400.0000")
        @NotNull(message = "Public group rate is required")
        @DecimalMin(value = "0.0000", message = "Public group rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Public group rate must have at most 8 digits and 4 decimals")
        @JsonProperty("public_group_rate")
        BigDecimal publicGroupRate
) {

    public BigDecimal resolveRate(ClassVisibility visibility, SessionFormat format) {
        return switch (visibility) {
            case PRIVATE -> format == SessionFormat.INDIVIDUAL ? privateIndividualRate : privateGroupRate;
            case PUBLIC -> format == SessionFormat.INDIVIDUAL ? publicIndividualRate : publicGroupRate;
        };
    }
}
