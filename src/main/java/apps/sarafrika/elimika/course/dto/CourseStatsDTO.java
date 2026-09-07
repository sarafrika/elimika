package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A course's statistics, in three blocks the caller earns separately.
 * <p>
 * {@code public} is always there. {@code scoped} appears only for a trainer with an approved
 * application to deliver the course, {@code owner} only for the creator and platform admins. A block
 * the caller may not read is <strong>absent from the JSON</strong> rather than null or zeroed — the
 * type is {@code NON_NULL} for exactly that reason, the way {@link OrganisationCourseContentDTO}
 * already omits lesson bodies from an unapproved organisation's payload. Not approved means not
 * transmitted, so there is nothing on the wire for a client to filter or a proxy to log.
 */
@Schema(name = "CourseStats", description = "Course statistics. The scoped and owner blocks are omitted entirely unless the caller is entitled to them.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseStatsDTO(

        @Schema(description = "Course-wide figures, present for every signed-in caller.")
        @JsonProperty("public")
        CourseStatsPublicDTO publicStats,

        @Schema(description = "The calling trainer's own delivery. Absent unless they are approved to train the course.")
        @JsonProperty("scoped")
        CourseStatsScopedDTO scoped,

        @Schema(description = "Commercial totals. Absent unless the caller is the course creator or a platform admin.")
        @JsonProperty("owner")
        CourseStatsOwnerDTO owner
) {
}
