package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
import apps.sarafrika.elimika.shared.spi.enrollment.AttendanceMarkedEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CourseAttendanceGradeBookListener {

    private final CourseGradeBookService courseGradeBookService;

    @EventListener
    public void handleAttendanceMarked(AttendanceMarkedEventDTO event) {
        if (event == null || event.attendanceStatus() == null) {
            return;
        }

        CourseAttendanceStatus attendanceStatus = toCourseAttendanceStatus(event.attendanceStatus());
        if (attendanceStatus == null) {
            return;
        }

        courseGradeBookService.syncAttendanceMark(
                event.instanceUuid(),
                event.classDefinitionUuid(),
                event.studentUuid(),
                event.classTitle(),
                event.markedAt(),
                attendanceStatus
        );
    }

    private CourseAttendanceStatus toCourseAttendanceStatus(String enrollmentStatus) {
        return switch (enrollmentStatus.toUpperCase(Locale.ROOT)) {
            case "ATTENDED" -> CourseAttendanceStatus.ATTENDED;
            case "ABSENT" -> CourseAttendanceStatus.ABSENT;
            default -> null;
        };
    }
}
