package apps.sarafrika.elimika.tenancy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO for admin dashboard statistics
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-12-01
 */
@Schema(
    name = "AdminDashboardStats",
    description = "Comprehensive statistics for the admin dashboard"
)
public record AdminDashboardStatsDTO(

    @Schema(description = "Timestamp when statistics were generated")
    LocalDateTime timestamp,

    @Schema(description = "Overall system health status")
    String overallHealth,

    @Schema(description = "User-related metrics")
    UserMetrics userMetrics,

    @Schema(description = "Organization-related metrics")
    OrganizationMetrics organizationMetrics,

    @Schema(description = "Content-related metrics")
    ContentMetrics contentMetrics,

    @Schema(description = "System performance metrics")
    SystemPerformance systemPerformance,

    @Schema(description = "Admin-specific metrics")
    AdminMetrics adminMetrics

) {

    @Schema(description = "User metrics for dashboard")
    public record UserMetrics(
        long totalUsers,
        long activeUsers24h,
        long newRegistrations7d,
        long suspendedAccounts
    ) {}

    @Schema(description = "Organization metrics for dashboard")
    public record OrganizationMetrics(
        long totalOrganizations,
        long pendingApprovals,
        long activeOrganizations,
        long suspendedOrganizations
    ) {}

    @Schema(description = "Content metrics for dashboard")
    public record ContentMetrics(
        long totalCourses,
        long pendingModeration,
        long reportedContent,
        double averageQualityScore
    ) {}

    @Schema(description = "System performance metrics")
    public record SystemPerformance(
        String serverUptime,
        String averageResponseTime,
        String errorRate,
        String storageUsage
    ) {}

    @Schema(description = "Admin-specific metrics")
    public record AdminMetrics(
        long totalAdmins,
        long activeAdminSessions,
        long adminActionsToday,
        long systemAdmins,
        long organizationAdmins
    ) {}

    public AdminDashboardStatsDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (overallHealth == null) {
            overallHealth = "UNKNOWN";
        }
    }
}