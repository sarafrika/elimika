package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single ISO-week bucket in an organisation's weekly-growth series — the number
 * of distinct students-per-course enrolled during that week for classes owned by
 * the organisation.
 *
 * @param week       the ISO week bucket in {@code IYYY-"W"IW} form, e.g. {@code 2026-W07}
 * @param enrolments the number of distinct student/course enrolments recorded during the week
 */
@Schema(description = "A weekly bucket of enrolment activity for an organisation")
public record WeeklyGrowthPointDTO(
        @Schema(description = "ISO week bucket in IYYY-WIW form", example = "2026-W07")
        @JsonProperty("week") String week,

        @Schema(description = "Distinct student/course enrolments recorded during the week", example = "12")
        @JsonProperty("enrolments") long enrolments
) {
}
