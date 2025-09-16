package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("timestamp")
    LocalDateTime timestamp,

    @Schema(description = "Overall system health status")
    @JsonProperty("overall_health")
    String overallHealth,

    @Schema(description = "User-related metrics")
    @JsonProperty("user_metrics")
    UserMetrics userMetrics,

    @Schema(description = "Organization-related metrics")
    @JsonProperty("organization_metrics")
    OrganizationMetrics organizationMetrics,

    @Schema(description = "Content-related metrics")
    @JsonProperty("content_metrics")
    ContentMetrics contentMetrics,

    @Schema(description = "System performance metrics")
    @JsonProperty("system_performance")
    SystemPerformance systemPerformance,

    @Schema(description = "Admin-specific metrics")
    @JsonProperty("admin_metrics")
    AdminMetrics adminMetrics

) {

    @Schema(description = "User metrics for dashboard")
    public record UserMetrics(
        @JsonProperty("total_users")
        long totalUsers,
        @JsonProperty("active_users_24h")
        long activeUsers24h,
        @JsonProperty("new_registrations_7d")
        long newRegistrations7d,
        @JsonProperty("suspended_accounts")
        long suspendedAccounts
    ) {}

    @Schema(description = "Organization metrics for dashboard")
    public record OrganizationMetrics(
        @JsonProperty("total_organizations")
        long totalOrganizations,
        @JsonProperty("pending_approvals")
        long pendingApprovals,
        @JsonProperty("active_organizations")
        long activeOrganizations,
        @JsonProperty("suspended_organizations")
        long suspendedOrganizations
    ) {}

    @Schema(description = "Content metrics for dashboard")
    public record ContentMetrics(
        @JsonProperty("total_courses")
        long totalCourses,
        @JsonProperty("pending_moderation")
        long pendingModeration,
        @JsonProperty("reported_content")
        long reportedContent,
        @JsonProperty("average_quality_score")
        double averageQualityScore
    ) {}

    @Schema(description = "System performance metrics")
    public record SystemPerformance(
        @JsonProperty("server_uptime")
        String serverUptime,
        @JsonProperty("average_response_time")
        String averageResponseTime,
        @JsonProperty("error_rate")
        String errorRate,
        @JsonProperty("storage_usage")
        String storageUsage
    ) {}

    @Schema(description = "Admin-specific metrics")
    public record AdminMetrics(
        @JsonProperty("total_admins")
        long totalAdmins,
        @JsonProperty("active_admin_sessions")
        long activeAdminSessions,
        @JsonProperty("admin_actions_today")
        long adminActionsToday,
        @JsonProperty("system_admins")
        long systemAdmins,
        @JsonProperty("organization_admins")
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