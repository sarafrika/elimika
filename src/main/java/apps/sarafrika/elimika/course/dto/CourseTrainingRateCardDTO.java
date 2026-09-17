package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

/**
 * Instructor pricing across session format, delivery modality and basis; a null cell is not offered, zero is never a price.
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

        @Schema(description = "Private (1:1) online rate per learner per hour. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_online_hourly_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_online_hourly_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_hourly_rate")
        BigDecimal privateOnlineHourlyRate,

        @Schema(description = "Private (1:1) in-person rate per learner per hour. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_inperson_hourly_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_inperson_hourly_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_hourly_rate")
        BigDecimal privateInpersonHourlyRate,

        @Schema(description = "Group online rate per learner per hour. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_online_hourly_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_online_hourly_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_hourly_rate")
        BigDecimal groupOnlineHourlyRate,

        @Schema(description = "Group in-person rate per learner per hour. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_inperson_hourly_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_inperson_hourly_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_hourly_rate")
        BigDecimal groupInpersonHourlyRate,

        @Schema(description = "Private (1:1) online rate per learner per session, whatever its length. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_online_session_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_online_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_session_rate")
        BigDecimal privateOnlineSessionRate,

        @Schema(description = "Private (1:1) in-person rate per learner per session, whatever its length. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_inperson_session_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_inperson_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_session_rate")
        BigDecimal privateInpersonSessionRate,

        @Schema(description = "Group online rate per learner per session, whatever its length. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_online_session_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_online_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_session_rate")
        BigDecimal groupOnlineSessionRate,

        @Schema(description = "Group in-person rate per learner per session, whatever its length. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_inperson_session_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_inperson_session_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_session_rate")
        BigDecimal groupInpersonSessionRate,

        @Schema(description = "Private (1:1) online rate per learner per calendar day, however many sessions fall in it. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_online_daily_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_online_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_online_daily_rate")
        BigDecimal privateOnlineDailyRate,

        @Schema(description = "Private (1:1) in-person rate per learner per calendar day, however many sessions fall in it. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "private_inperson_daily_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "private_inperson_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("private_inperson_daily_rate")
        BigDecimal privateInpersonDailyRate,

        @Schema(description = "Group online rate per learner per calendar day, however many sessions fall in it. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_online_daily_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_online_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_online_daily_rate")
        BigDecimal groupOnlineDailyRate,

        @Schema(description = "Group in-person rate per learner per calendar day, however many sessions fall in it. Null when this method is not offered; an offered method (format x location) prices all three bases, each above zero and at least the minimum training fee.", example = "3500.0000", nullable = true)
        @DecimalMin(value = "0", inclusive = false, message = "group_inperson_daily_rate must be greater than zero")
        @Digits(integer = 8, fraction = 4, message = "group_inperson_daily_rate must have at most 8 digits and 4 decimals")
        @JsonProperty("group_inperson_daily_rate")
        BigDecimal groupInpersonDailyRate
) {

    public BigDecimal resolveRate(SessionFormat format, LocationType locationType) {
        return resolveRate(format, locationType, RateBasis.PER_HOUR);
    }

    /** The rate in the unit the job was contracted in; null when not offered, which means "cannot be matched". */
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
