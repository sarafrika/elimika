package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of scheduling and attendance analytics for the timetabling module.
 *
 * @param sessionsNext7Days sessions scheduled in the next 7 days
 * @param sessionsLast30Days sessions that started in the last 30 days
 * @param sessionsCompletedLast30Days sessions completed in the last 30 days
 * @param sessionsCancelledLast30Days sessions cancelled in the last 30 days
 * @param attendedEnrollmentsLast30Days enrollments marked as attended in the last 30 days
 * @param absentEnrollmentsLast30Days enrollments marked as absent in the last 30 days
 */
public record TimetablingAnalyticsSnapshot(
        long sessionsNext7Days,
        long sessionsLast30Days,
        long sessionsCompletedLast30Days,
        long sessionsCancelledLast30Days,
        long attendedEnrollmentsLast30Days,
        long absentEnrollmentsLast30Days
) {
}