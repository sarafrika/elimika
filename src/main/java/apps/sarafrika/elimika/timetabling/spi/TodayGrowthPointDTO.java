package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single hour bucket in an organisation's "today's growth" series — the number
 * of enrolments recorded in that hour of the current day.
 *
 * @param hour       the hour bucket in {@code HH:00} (24h) form
 * @param enrolments the number of enrolments recorded during that hour
 */
@Schema(description = "An hourly bucket of today's enrolment activity for an organisation")
public record TodayGrowthPointDTO(
        @Schema(description = "Hour bucket in HH:00 (24h) form", example = "14:00")
        @JsonProperty("hour") String hour,

        @Schema(description = "Enrolments recorded during the hour", example = "5")
        @JsonProperty("enrolments") long enrolments
) {
}
