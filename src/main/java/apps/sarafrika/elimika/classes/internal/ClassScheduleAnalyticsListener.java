package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.service.impl.ClassScheduleAnalyticsServiceImpl;
import apps.sarafrika.elimika.shared.event.classes.ClassAssignmentScheduleChangedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassQuizScheduleChangedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Updates analytics when class assessment schedules change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleAnalyticsListener {

    private final ClassScheduleAnalyticsServiceImpl analyticsService;

    @ApplicationModuleListener
    @Async
    public void handleAssignmentScheduleEvent(ClassAssignmentScheduleChangedEventDTO event) {
        analyticsService.updateLastChange(event.changedAt());
        log.debug("Captured assignment schedule {} event for analytics", event.changeType());
    }

    @ApplicationModuleListener
    @Async
    public void handleQuizScheduleEvent(ClassQuizScheduleChangedEventDTO event) {
        analyticsService.updateLastChange(event.changedAt());
        log.debug("Captured quiz schedule {} event for analytics", event.changeType());
    }
}
