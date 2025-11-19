package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

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

    ,

    @Schema(description = "Keycloak admin event telemetry")
    @JsonProperty("keycloak_admin_events")
    KeycloakAdminEventMetrics keycloakAdminEvents,

    @Schema(description = "Learning performance metrics")
    @JsonProperty("learning_metrics")
    LearningMetrics learningMetrics,

    @Schema(description = "Timetabling utilisation metrics")
    @JsonProperty("timetabling_metrics")
    TimetablingMetrics timetablingMetrics,

    @Schema(description = "Commerce performance metrics")
    @JsonProperty("commerce_metrics")
    CommerceMetrics commerceMetrics,

    @Schema(description = "Notification delivery metrics")
    @JsonProperty("communication_metrics")
    CommunicationMetrics communicationMetrics,

    @Schema(description = "Compliance and verification metrics")
    @JsonProperty("compliance_metrics")
    ComplianceMetrics complianceMetrics

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

    @Schema(description = "Keycloak admin event activity")
    public record KeycloakAdminEventMetrics(
        @JsonProperty("events_last_24h")
        long eventsLast24Hours,
        @JsonProperty("events_last_7d")
        long eventsLast7Days,
        @JsonProperty("operations_last_24h")
        Map<String, Long> operationsLast24Hours,
        @JsonProperty("resource_types_last_24h")
        Map<String, Long> resourceTypesLast24Hours
    ) {}

    @Schema(description = "Detailed learning analytics")
    public record LearningMetrics(
        @JsonProperty("total_courses")
        long totalCourses,
        @JsonProperty("published_courses")
        long publishedCourses,
        @JsonProperty("in_review_courses")
        long inReviewCourses,
        @JsonProperty("draft_courses")
        long draftCourses,
        @JsonProperty("archived_courses")
        long archivedCourses,
        @JsonProperty("total_course_enrollments")
        long totalCourseEnrollments,
        @JsonProperty("active_course_enrollments")
        long activeCourseEnrollments,
        @JsonProperty("new_course_enrollments_7d")
        long newCourseEnrollments7d,
        @JsonProperty("completed_course_enrollments_30d")
        long completedCourseEnrollments30d,
        @JsonProperty("average_course_progress")
        double averageCourseProgress,
        @JsonProperty("total_training_programs")
        long totalTrainingPrograms,
        @JsonProperty("published_training_programs")
        long publishedTrainingPrograms,
        @JsonProperty("active_training_programs")
        long activeTrainingPrograms,
        @JsonProperty("program_enrollments")
        long programEnrollments,
        @JsonProperty("completed_program_enrollments_30d")
        long completedProgramEnrollments30d
    ) {}

    @Schema(description = "Timetabling and attendance analytics")
    public record TimetablingMetrics(
        @JsonProperty("sessions_next_7d")
        long sessionsNext7Days,
        @JsonProperty("sessions_last_30d")
        long sessionsLast30Days,
        @JsonProperty("sessions_completed_last_30d")
        long sessionsCompletedLast30Days,
        @JsonProperty("sessions_cancelled_last_30d")
        long sessionsCancelledLast30Days,
        @JsonProperty("attended_enrollments_last_30d")
        long attendedEnrollmentsLast30Days,
        @JsonProperty("absent_enrollments_last_30d")
        long absentEnrollmentsLast30Days
    ) {}

    @Schema(description = "Commerce analytics")
    public record CommerceMetrics(
        @JsonProperty("total_orders")
        long totalOrders,
        @JsonProperty("orders_last_30d")
        long ordersLast30Days,
        @JsonProperty("captured_orders")
        long capturedOrders,
        @JsonProperty("unique_customers")
        long uniqueCustomers,
        @JsonProperty("new_customers_last_30d")
        long newCustomersLast30Days,
        @JsonProperty("course_purchases_last_30d")
        long coursePurchasesLast30Days,
        @JsonProperty("class_purchases_last_30d")
        long classPurchasesLast30Days
    ) {}

    @Schema(description = "Notification delivery analytics")
    public record CommunicationMetrics(
        @JsonProperty("notifications_created_7d")
        long notificationsCreated7d,
        @JsonProperty("notifications_delivered_7d")
        long notificationsDelivered7d,
        @JsonProperty("notifications_failed_7d")
        long notificationsFailed7d,
        @JsonProperty("pending_notifications")
        long pendingNotifications
    ) {}

    @Schema(description = "Compliance and verification analytics")
    public record ComplianceMetrics(
        @JsonProperty("verified_instructors")
        long verifiedInstructors,
        @JsonProperty("pending_instructor_verifications")
        long pendingInstructorVerifications,
        @JsonProperty("pending_instructor_documents")
        long pendingInstructorDocuments,
        @JsonProperty("expiring_instructor_documents_30d")
        long expiringInstructorDocuments30d,
        @JsonProperty("total_course_creators")
        long totalCourseCreators,
        @JsonProperty("verified_course_creators")
        long verifiedCourseCreators,
        @JsonProperty("pending_course_creator_verifications")
        long pendingCourseCreatorVerifications
    ) {}

    public AdminDashboardStatsDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (overallHealth == null) {
            overallHealth = "UNKNOWN";
        }
        if (keycloakAdminEvents == null) {
            keycloakAdminEvents = new KeycloakAdminEventMetrics(0, 0, Map.of(), Map.of());
        }
    }
}
