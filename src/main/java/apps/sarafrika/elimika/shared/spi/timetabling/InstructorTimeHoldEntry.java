package apps.sarafrika.elimika.shared.spi.timetabling;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A marketplace job's claim on an instructor's diary, projected for modules that cannot see timetabling.
 * {@code firm} is true once the instructor was hired, which is the only state that blocks booking.
 */
public record InstructorTimeHoldEntry(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("job_uuid") UUID jobUuid,
        @JsonProperty("title") String title,
        @JsonProperty("organisation_uuid") UUID organisationUuid,
        @JsonProperty("organisation_name") String organisationName,
        @JsonProperty("start_time") LocalDateTime startTime,
        @JsonProperty("end_time") LocalDateTime endTime,
        @JsonProperty("firm") boolean firm
) { }
