package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.shared.spi.analytics.TimetablingAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.TimetablingAnalyticsSnapshot;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetablingAnalyticsServiceImpl implements TimetablingAnalyticsService {

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public TimetablingAnalyticsSnapshot captureSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAhead = now.plusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        long sessionsNext7Days = scheduledInstanceRepository.countByStartTimeBetween(now, sevenDaysAhead);
        long sessionsLast30Days = scheduledInstanceRepository.countByStartTimeBetween(thirtyDaysAgo, now);
        long sessionsCompletedLast30Days = scheduledInstanceRepository
                .countByStatusAndEndTimeBetween(SchedulingStatus.COMPLETED, thirtyDaysAgo, now);
        long sessionsCancelledLast30Days = scheduledInstanceRepository
                .countByStatusAndStartTimeBetween(SchedulingStatus.CANCELLED, thirtyDaysAgo, now);

        long attendedEnrollmentsLast30Days = enrollmentRepository
                .countByStatusAndAttendanceMarkedAtBetween(EnrollmentStatus.ATTENDED, thirtyDaysAgo, now);
        long absentEnrollmentsLast30Days = enrollmentRepository
                .countByStatusAndAttendanceMarkedAtBetween(EnrollmentStatus.ABSENT, thirtyDaysAgo, now);

        return new TimetablingAnalyticsSnapshot(
                sessionsNext7Days,
                sessionsLast30Days,
                sessionsCompletedLast30Days,
                sessionsCancelledLast30Days,
                attendedEnrollmentsLast30Days,
                absentEnrollmentsLast30Days
        );
    }
}
