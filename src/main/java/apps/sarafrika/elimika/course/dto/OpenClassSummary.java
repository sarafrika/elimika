package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One class a visitor can still join, as the public course page lists it.
 * <p>
 * Safe for anonymous readers by construction: there is no field for coordinates, a meeting link,
 * the instructor, the organisation or any revenue term. The fee is the class's sticker price.
 */
@Schema(name = "OpenClassSummary", description = "A class on a course that a visitor can still join.")
public record OpenClassSummary(

        @Schema(description = "Class definition UUID.")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Class title.", example = "Weekend cohort - Nairobi")
        @JsonProperty("title")
        String title,

        @Schema(description = "How the class is delivered.", example = "IN_PERSON")
        @JsonProperty("location_type")
        LocationType locationType,

        @Schema(description = "Group or one-to-one.", example = "GROUP")
        @JsonProperty("session_format")
        SessionFormat sessionFormat,

        @Schema(description = "The venue: the first comma-separated part of the class's location label. "
                + "Null when the class has no location label (typically online).",
                example = "Kenya School of Government")
        @JsonProperty("place_name")
        String placeName,

        @Schema(description = "The rest of the location label after the place name, with a trailing country "
                + "removed. Null when nothing remains.", example = "Lower Kabete Road, Nairobi")
        @JsonProperty("area")
        String area,

        @Schema(description = "The class fee a learner pays (the class sale price). Null when not set.",
                example = "2500.00")
        @JsonProperty("fee")
        BigDecimal fee,

        @Schema(description = "ISO 4217 currency of the fee.", example = "KES")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Seats offered. Null when unknown.", example = "20")
        @JsonProperty("max_participants")
        Integer maxParticipants,

        @Schema(description = "Seats offered minus live (not cancelled, not waitlisted) enrolments, never "
                + "below zero. Null when the class sets no seat cap.", example = "7")
        @JsonProperty("seats_left")
        Integer seatsLeft,

        @Schema(description = "First teaching day (the academic period start, else the first session's day). "
                + "Null when unknown.", example = "2026-10-12")
        @JsonProperty("starts_on")
        LocalDate startsOn,

        @Schema(description = "Last teaching day. Null when open-ended.", example = "2026-12-18")
        @JsonProperty("ends_on")
        LocalDate endsOn,

        @Schema(description = "Last day, inclusive, on which enrolments are accepted. Null when open-ended.",
                example = "2026-10-10")
        @JsonProperty("registration_closes_on")
        LocalDate registrationClosesOn,

        @Schema(description = "The training branch the class is delivered at. Null when none.",
                example = "Westlands branch")
        @JsonProperty("branch_name")
        String branchName
) {
}
