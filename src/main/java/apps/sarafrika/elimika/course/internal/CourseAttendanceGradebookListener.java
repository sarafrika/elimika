package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.service.CourseGradebookService;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
import apps.sarafrika.elimika.timetabling.dto.AttendanceMarkedEventDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAttendanceGradebookListener {

    private final CourseGradebookService courseGradebookService;

    @EventListener
    public void handleAttendanceMarked(AttendanceMarkedEventDTO event) {
        if (event == null || event.attendanceStatus() == null) {
            return;
        }

        CourseAttendanceStatus attendanceStatus = toCourseAttendanceStatus(event.attendanceStatus());
        if (attendanceStatus == null) {
            return;
        }

        courseGradebookService.syncAttendanceMark(
                event.instanceUuid(),
                event.classDefinitionUuid(),
                event.studentUuid(),
                event.classTitle(),
                event.markedAt(),
                attendanceStatus
        );
    }

    private CourseAttendanceStatus toCourseAttendanceStatus(EnrollmentStatus enrollmentStatus) {
        return switch (enrollmentStatus) {
            case ATTENDED -> CourseAttendanceStatus.ATTENDED;
            case ABSENT -> CourseAttendanceStatus.ABSENT;
            default -> null;
        };
    }
}
