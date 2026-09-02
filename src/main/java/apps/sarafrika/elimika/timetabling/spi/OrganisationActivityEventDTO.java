package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One entry in an organisation's activity feed — a recent, human-meaningful thing that
 * happened: a student enrolled, a class was opened, or an instructor was paid.
 *
 * <p>The {@code subjectUuid} is left as an id so the caller can resolve the display name
 * through its batched lookups: for {@code ENROLMENT} it is the student, for {@code PAYOUT}
 * it is the instructor user, and for {@code CLASS_OPENED} it is absent.
 *
 * @param eventType    one of {@code ENROLMENT}, {@code CLASS_OPENED}, {@code PAYOUT}
 * @param occurredAt   when it happened
 * @param classTitle   the class involved, when there is one
 * @param subjectUuid  the person the event is about (student or instructor user), when there is one
 * @param amount       the money moved, for {@code PAYOUT} events
 * @param currencyCode the currency of {@code amount}, for {@code PAYOUT} events
 */
@Schema(description = "A recent activity event within an organisation")
public record OrganisationActivityEventDTO(
        @Schema(description = "Event type: ENROLMENT, CLASS_OPENED or PAYOUT", example = "ENROLMENT")
        @JsonProperty("event_type") String eventType,

        @Schema(description = "When the event occurred")
        @JsonProperty("occurred_at") LocalDateTime occurredAt,

        @Schema(description = "Class the event relates to, if any", nullable = true)
        @JsonProperty("class_title") String classTitle,

        @Schema(description = "Student (ENROLMENT) or instructor user (PAYOUT) the event is about",
                nullable = true)
        @JsonProperty("subject_uuid") UUID subjectUuid,

        @Schema(description = "Amount paid, for PAYOUT events", nullable = true)
        @JsonProperty("amount") BigDecimal amount,

        @Schema(description = "Currency of the amount, for PAYOUT events", nullable = true)
        @JsonProperty("currency_code") String currencyCode
) {
}
