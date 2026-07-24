package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single point in an organisation's monthly enrolment trend series.
 *
 * @param month the calendar month in {@code YYYY-MM} form
 * @param total the number of enrolments recorded in that month for classes owned
 *              by the organisation
 */
@Schema(description = "A single month in an organisation's enrolment trend series")
public record EnrolmentTrendPointDTO(
        @Schema(description = "Calendar month in YYYY-MM form", example = "2026-07")
        @JsonProperty("month") String month,

        @Schema(description = "Number of enrolments recorded in the month", example = "42")
        @JsonProperty("total") long total
) {
}
