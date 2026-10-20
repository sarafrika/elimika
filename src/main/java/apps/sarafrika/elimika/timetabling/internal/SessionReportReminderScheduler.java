package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.timetabling.model.SessionReport;
import apps.sarafrika.elimika.timetabling.repository.SessionReportRepository;
import apps.sarafrika.elimika.timetabling.service.SessionReportService;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Keeps session reports mandatory: reminds at session end, flags overdue at 24h, locks after the grace
 * window. Each notice is claimed in the database first, so it publishes once however often this runs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionReportReminderScheduler {

    private static final Duration REMINDER_WINDOW = Duration.ofHours(24);

    private final SessionReportRepository reportRepository;
    private final SessionReportService sessionReportService;

    /** Ended sessions older than the reminder window are still tracked back to here, never further. */
    @Value("${app.session-reports.track-from:2026-10-01T00:00:00}")
    private LocalDateTime trackFrom;

    @Scheduled(cron = "${app.session-reports.reminder-cron:0 */5 * * * *}", zone = "UTC")
    public void run() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (UUID instanceUuid : reportRepository.findEndedSessionsAwaitingReminder(now.minus(REMINDER_WINDOW), now)) {
            safely("remind", instanceUuid, () -> sessionReportService.remindDue(instanceUuid));
        }
        for (UUID instanceUuid : reportRepository.findEndedSessionsWithoutReport(trackFrom, now.minus(REMINDER_WINDOW))) {
            safely("track", instanceUuid, () -> sessionReportService.trackMissedSession(instanceUuid));
        }
        for (SessionReport report : reportRepository.findByStatusAndDueAtBeforeAndOverdueNotifiedAtIsNull(SessionReportStatus.DRAFT, now)) {
            safely("flag overdue", report.getScheduledInstanceUuid(), () -> sessionReportService.noticeOverdue(report));
        }
        for (SessionReport report : reportRepository.findByStatusAndEditableUntilBefore(SessionReportStatus.SUBMITTED, now)) {
            safely("lock", report.getScheduledInstanceUuid(), () -> sessionReportService.lock(report));
        }
    }

    private static void safely(String step, UUID instanceUuid, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Could not {} the session report for instance {}", step, instanceUuid, e);
        }
    }
}
