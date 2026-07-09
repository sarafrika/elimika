package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Statistics scoped to a single organisation for its own reporting dashboard.
 * <p>
 * Unlike {@code AdminDashboardStatsDTO}, every figure here is limited to the
 * requested organisation — never platform-wide.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-10
 */
@Schema(
        name = "OrganisationDashboardStats",
        description = "Statistics scoped strictly to a single organisation"
)
public record OrganisationDashboardStatsDTO(

        @Schema(description = "Timestamp when statistics were generated")
        @JsonProperty("timestamp")
        LocalDateTime timestamp,

        @Schema(description = "UUID of the organisation these statistics belong to")
        @JsonProperty("organisation_uuid")
        java.util.UUID organisationUuid,

        @Schema(description = "Total members affiliated with the organisation")
        @JsonProperty("total_members")
        long totalMembers,

        @Schema(description = "Total members with the student domain")
        @JsonProperty("total_students")
        long totalStudents,

        @Schema(description = "Total members with the instructor domain")
        @JsonProperty("total_instructors")
        long totalInstructors,

        @Schema(description = "Total organisation administrators")
        @JsonProperty("total_admins")
        long totalAdmins,

        @Schema(description = "Total active training branches")
        @JsonProperty("total_branches")
        long totalBranches

) {
    public OrganisationDashboardStatsDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
