package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.SessionReportLockLookup;
import apps.sarafrika.elimika.timetabling.model.SessionReport;
import apps.sarafrika.elimika.timetabling.repository.SessionReportRepository;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** A report locks at editable_until; until the lock job runs, a lapsed window counts as locked too. */
@Component
@RequiredArgsConstructor
public class SessionReportLocks implements SessionReportLockLookup {

    public static final String LOCKED_MESSAGE = "This session's report is locked; its records can no longer be changed.";

    private final SessionReportRepository repository;

    public boolean isSessionLocked(UUID instanceUuid) {
        return instanceUuid != null && repository.findByScheduledInstanceUuid(instanceUuid)
                .map(SessionReportLocks::isLocked)
                .orElse(false);
    }

    public void requireSessionOpen(UUID instanceUuid) {
        if (isSessionLocked(instanceUuid)) {
            throw new IllegalStateException(LOCKED_MESSAGE);
        }
    }

    @Override
    public boolean isLessonLocked(UUID studentUuid, UUID courseUuid, UUID lessonUuid) {
        if (studentUuid == null || courseUuid == null || lessonUuid == null) {
            return false;
        }
        return repository.existsLockedForStudentLesson(studentUuid, courseUuid, lessonUuid, LocalDateTime.now(ZoneOffset.UTC));
    }

    public static boolean isLocked(SessionReport report) {
        return report.getStatus() == SessionReportStatus.LOCKED
                || (report.getStatus() == SessionReportStatus.SUBMITTED && report.getEditableUntil() != null
                && report.getEditableUntil().isBefore(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
