package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.classes.repository.ClassQuizScheduleRepository;
import apps.sarafrika.elimika.shared.spi.analytics.ClassScheduleAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.ClassScheduleAnalyticsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleAnalyticsServiceImpl implements ClassScheduleAnalyticsService {

    private final ClassAssignmentScheduleRepository classAssignmentScheduleRepository;
    private final ClassQuizScheduleRepository classQuizScheduleRepository;
    private final AtomicReference<LocalDateTime> lastScheduleChange = new AtomicReference<>();

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleAnalyticsSnapshot captureSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAhead = now.plusDays(7);

        long totalAssignments = classAssignmentScheduleRepository.count();
        long totalQuizzes = classQuizScheduleRepository.count();
        long upcomingAssignments = classAssignmentScheduleRepository.countByDueAtBetween(now, sevenDaysAhead);
        long upcomingQuizzes = classQuizScheduleRepository.countByDueAtBetween(now, sevenDaysAhead);
        long overdueAssignments = classAssignmentScheduleRepository.countByDueAtBefore(now);
        long overdueQuizzes = classQuizScheduleRepository.countByDueAtBefore(now);

        return new ClassScheduleAnalyticsSnapshot(
                totalAssignments,
                totalQuizzes,
                upcomingAssignments,
                upcomingQuizzes,
                overdueAssignments,
                overdueQuizzes,
                lastScheduleChange.get()
        );
    }

    public void updateLastChange(LocalDateTime changedAt) {
        LocalDateTime effectiveTime = changedAt != null ? changedAt : LocalDateTime.now();
        lastScheduleChange.updateAndGet(existing ->
                existing == null || effectiveTime.isAfter(existing) ? effectiveTime : existing);
        log.debug("Class schedule analytics updated at {}", effectiveTime);
    }
}
