package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** For the course or program owner: which rate card cells are priced below the minimum training fee. */
@Schema(
        name = "TrainingRateFloorFlags",
        description = "Per cell, true when the rate is set and below the minimum training fee (legacy cards can be). Owner only."
)
public record TrainingRateFloorFlagsDTO(

        @Schema(description = "The minimum training fee the card is compared against: the course's, or the highest across a program's courses.")
        @JsonProperty("minimum_training_fee")
        BigDecimal minimumTrainingFee,

        @JsonProperty("private_online_hourly_rate")
        boolean privateOnlineHourlyRate,

        @JsonProperty("private_inperson_hourly_rate")
        boolean privateInpersonHourlyRate,

        @JsonProperty("group_online_hourly_rate")
        boolean groupOnlineHourlyRate,

        @JsonProperty("group_inperson_hourly_rate")
        boolean groupInpersonHourlyRate,

        @JsonProperty("private_online_session_rate")
        boolean privateOnlineSessionRate,

        @JsonProperty("private_inperson_session_rate")
        boolean privateInpersonSessionRate,

        @JsonProperty("group_online_session_rate")
        boolean groupOnlineSessionRate,

        @JsonProperty("group_inperson_session_rate")
        boolean groupInpersonSessionRate,

        @JsonProperty("private_online_daily_rate")
        boolean privateOnlineDailyRate,

        @JsonProperty("private_inperson_daily_rate")
        boolean privateInpersonDailyRate,

        @JsonProperty("group_online_daily_rate")
        boolean groupOnlineDailyRate,

        @JsonProperty("group_inperson_daily_rate")
        boolean groupInpersonDailyRate
) {
}
