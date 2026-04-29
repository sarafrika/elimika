package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentStatusChangedEventDTO;
import apps.sarafrika.elimika.shared.spi.enrollment.StudentEnrolledEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseEnrollmentInheritanceListener {

    private final CourseEnrollmentSyncService courseEnrollmentSyncService;

    @EventListener
    public void handleStudentEnrolled(StudentEnrolledEventDTO event) {
        if (event == null) {
            return;
        }
        courseEnrollmentSyncService.syncFromClassDefinition(event.studentUuid(), event.classDefinitionUuid());
    }

    @EventListener
    public void handleEnrollmentStatusChanged(EnrollmentStatusChangedEventDTO event) {
        if (event == null) {
            return;
        }
        courseEnrollmentSyncService.syncFromClassDefinition(event.studentUuid(), event.classDefinitionUuid());
    }
}
