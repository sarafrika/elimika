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
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

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
        @JsonProperty("private_online_hourly_rate")
        BigDecimal privateOnlineHourlyRate,

        @Schema(description = "1:1 private session rate when delivered in person, per learner per hour.", example = "3600.0000")
        @NotNull(message = "Private in-person rate is required")
        @DecimalMin(value = "0.0000", message = "Private in-person rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Private in-person rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_hourly_rate")
        BigDecimal privateInpersonHourlyRate,

        @Schema(description = "Group session rate when delivered online, per learner per hour.", example = "2800.0000")
        @NotNull(message = "Group online rate is required")
        @DecimalMin(value = "0.0000", message = "Group online rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Group online rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_hourly_rate")
        BigDecimal groupOnlineHourlyRate,

        @Schema(description = "Group session rate when delivered in person, per learner per hour.", example = "3000.0000")
        @NotNull(message = "Group in-person rate is required")
        @DecimalMin(value = "0.0000", message = "Group in-person rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "Group in-person rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_hourly_rate")
        BigDecimal groupInpersonHourlyRate,

        @Schema(description = "1:1 private session rate when delivered online, per learner per session, whatever its length. Required for new and updated cards; null on cards created before per-session pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "private_online_session_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "private_online_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_session_rate")
        BigDecimal privateOnlineSessionRate,

        @Schema(description = "1:1 private session rate when delivered in person, per learner per session, whatever its length. Required for new and updated cards; null on cards created before per-session pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "private_inperson_session_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "private_inperson_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_session_rate")
        BigDecimal privateInpersonSessionRate,

        @Schema(description = "Group session rate when delivered online, per learner per session, whatever its length. Required for new and updated cards; null on cards created before per-session pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "group_online_session_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "group_online_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_session_rate")
        BigDecimal groupOnlineSessionRate,

        @Schema(description = "Group session rate when delivered in person, per learner per session, whatever its length. Required for new and updated cards; null on cards created before per-session pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "group_inperson_session_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "group_inperson_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_session_rate")
        BigDecimal groupInpersonSessionRate,

        @Schema(description = "1:1 private session rate when delivered online, per learner per calendar day, however many sessions fall in it. Required for new and updated cards; null on cards created before per-daily pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "private_online_daily_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "private_online_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_daily_rate")
        BigDecimal privateOnlineDailyRate,

        @Schema(description = "1:1 private session rate when delivered in person, per learner per calendar day, however many sessions fall in it. Required for new and updated cards; null on cards created before per-daily pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "private_inperson_daily_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "private_inperson_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_daily_rate")
        BigDecimal privateInpersonDailyRate,

        @Schema(description = "Group session rate when delivered online, per learner per calendar day, however many sessions fall in it. Required for new and updated cards; null on cards created before per-daily pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "group_online_daily_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "group_online_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_daily_rate")
        BigDecimal groupOnlineDailyRate,

        @Schema(description = "Group session rate when delivered in person, per learner per calendar day, however many sessions fall in it. Required for new and updated cards; null on cards created before per-daily pricing existed.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0.0000", message = "group_inperson_daily_rate cannot be negative")
        @Digits(integer = 8, fraction = 4, message = "group_inperson_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_daily_rate")
        BigDecimal groupInpersonDailyRate
) {

    public BigDecimal resolveRate(SessionFormat format, LocationType locationType) {
        return resolveRate(format, locationType, RateBasis.PER_HOUR);
    }

    /**
     * The rate for a delivery mode in the unit the job was contracted in.
     * <p>
     * Returns null when the instructor has not priced that basis, which is not the same as free —
     * callers must treat it as "cannot be matched to this job" rather than substituting an hourly
     * figure, because a per-day rate derived from an hourly one is a number nobody agreed to.
     */
    public BigDecimal resolveRate(SessionFormat format, LocationType locationType, RateBasis basis) {
        boolean online = LocationType.ONLINE.equals(locationType);
        boolean inPerson = LocationType.IN_PERSON.equals(locationType) || LocationType.HYBRID.equals(locationType);

        if (!online && !inPerson) {
            // default to online pricing when location type is unspecified
            online = true;
        }

        return switch (basis == null ? RateBasis.PER_HOUR : basis) {
            case PER_HOUR -> switch (format) {
                case INDIVIDUAL -> online ? privateOnlineHourlyRate : privateInpersonHourlyRate;
                case GROUP -> online ? groupOnlineHourlyRate : groupInpersonHourlyRate;
            };
            case PER_SESSION -> switch (format) {
                case INDIVIDUAL -> online ? privateOnlineSessionRate : privateInpersonSessionRate;
                case GROUP -> online ? groupOnlineSessionRate : groupInpersonSessionRate;
            };
            case PER_DAY -> switch (format) {
                case INDIVIDUAL -> online ? privateOnlineDailyRate : privateInpersonDailyRate;
                case GROUP -> online ? groupOnlineDailyRate : groupInpersonDailyRate;
            };
        };
    }
}
