package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Exposes scheduling and attendance analytics derived from the timetabling module.
 */
public interface TimetablingAnalyticsService {

    /**
     * Collects a snapshot of scheduling metrics for administrative dashboards.
     *
     * @return immutable timetabling analytics snapshot
     */
    TimetablingAnalyticsSnapshot captureSnapshot();
}