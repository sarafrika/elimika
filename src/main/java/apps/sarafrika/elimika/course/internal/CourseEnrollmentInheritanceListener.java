package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.timetabling.dto.StudentEnrolledEventDTO;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEnrollmentInheritanceListener {

    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final CourseEnrollmentService courseEnrollmentService;

    @EventListener
    public void handleStudentEnrolled(StudentEnrolledEventDTO event) {
        if (event == null || event.studentUuid() == null || event.classDefinitionUuid() == null) {
            return;
        }

        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshotOpt =
                classDefinitionLookupService.findByUuid(event.classDefinitionUuid());
        if (snapshotOpt.isEmpty()) {
            log.warn("Class definition {} not found for enrollment {}", event.classDefinitionUuid(), event.enrollmentUuid());
            return;
        }

        UUID courseUuid = snapshotOpt.get().courseUuid();
        if (courseUuid == null) {
            log.debug("Class definition {} has no course; skipping course enrollment for student {}",
                    event.classDefinitionUuid(),
                    event.studentUuid());
            return;
        }

        if (courseEnrollmentService.existsByStudentUuidAndCourseUuid(event.studentUuid(), courseUuid)) {
            return;
        }

        CourseEnrollmentDTO courseEnrollment = new CourseEnrollmentDTO(
                null,
                event.studentUuid(),
                courseUuid,
                null,
                null,
                EnrollmentStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null
        );

        courseEnrollmentService.createCourseEnrollment(courseEnrollment);
    }
}
