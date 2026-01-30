package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService.ClassEnrollmentStatusSnapshot;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseEnrollmentSyncService {

    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final EnrollmentLookupService enrollmentLookupService;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseEnrollmentService courseEnrollmentService;

    public void syncFromClassDefinition(UUID studentUuid, UUID classDefinitionUuid) {
        if (studentUuid == null || classDefinitionUuid == null) {
            return;
        }

        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshotOpt =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
        if (snapshotOpt.isEmpty()) {
            log.warn("Class definition {} not found while syncing course enrollment for student {}",
                    classDefinitionUuid,
                    studentUuid);
            return;
        }

        UUID courseUuid = snapshotOpt.get().courseUuid();
        if (courseUuid == null) {
            return;
        }

        syncFromClassEnrollments(studentUuid, courseUuid);
    }

    public void syncFromClassEnrollments(UUID studentUuid, UUID courseUuid) {
        if (studentUuid == null || courseUuid == null) {
            return;
        }

        Optional<EnrollmentStatus> targetStatus = resolveStatusFromClassEnrollments(studentUuid, courseUuid);
        if (targetStatus.isEmpty()) {
            return;
        }

        upsertCourseEnrollment(studentUuid, courseUuid, targetStatus.get());
    }

    public void syncFromProgramEnrollment(UUID studentUuid, UUID programUuid, EnrollmentStatus programStatus) {
        if (studentUuid == null || programUuid == null) {
            return;
        }

        List<ProgramCourse> programCourses = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid);
        if (programCourses.isEmpty()) {
            return;
        }

        for (ProgramCourse programCourse : programCourses) {
            UUID courseUuid = programCourse.getCourseUuid();
            if (courseUuid == null) {
                continue;
            }

            EnrollmentStatus desiredStatus = resolveProgramPreferredStatus(studentUuid, courseUuid, programStatus);
            upsertCourseEnrollment(studentUuid, courseUuid, desiredStatus);
        }
    }

    private EnrollmentStatus resolveProgramPreferredStatus(UUID studentUuid, UUID courseUuid, EnrollmentStatus programStatus) {
        if (programStatus == null) {
            programStatus = EnrollmentStatus.ACTIVE;
        }

        Optional<ClassEnrollmentStatusSnapshot> activeEnrollment =
                enrollmentLookupService.findMostRecentActiveEnrollmentForCourse(studentUuid, courseUuid);
        if (activeEnrollment.isPresent()) {
            return EnrollmentStatus.ACTIVE;
        }

        return programStatus;
    }

    private Optional<EnrollmentStatus> resolveStatusFromClassEnrollments(UUID studentUuid, UUID courseUuid) {
        Optional<ClassEnrollmentStatusSnapshot> activeEnrollment =
                enrollmentLookupService.findMostRecentActiveEnrollmentForCourse(studentUuid, courseUuid);
        if (activeEnrollment.isPresent()) {
            return Optional.of(EnrollmentStatus.ACTIVE);
        }

        Optional<ClassEnrollmentStatusSnapshot> latestEnrollment =
                enrollmentLookupService.findMostRecentEnrollmentForCourse(studentUuid, courseUuid);
        if (latestEnrollment.isEmpty()) {
            return Optional.empty();
        }

        EnrollmentStatus mappedStatus = mapClassEnrollmentStatus(latestEnrollment.get().status());
        return Optional.ofNullable(mappedStatus);
    }

    private EnrollmentStatus mapClassEnrollmentStatus(String status) {
        if (status == null) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ENROLLED" -> EnrollmentStatus.ACTIVE;
            case "WAITLISTED" -> EnrollmentStatus.SUSPENDED;
            case "ATTENDED" -> EnrollmentStatus.COMPLETED;
            case "ABSENT" -> EnrollmentStatus.DROPPED;
            case "CANCELLED" -> EnrollmentStatus.DROPPED;
            default -> {
                log.warn("Unhandled class enrollment status '{}' while syncing course enrollment", status);
                yield null;
            }
        };
    }

    private void upsertCourseEnrollment(UUID studentUuid, UUID courseUuid, EnrollmentStatus status) {
        if (status == null) {
            return;
        }

        Optional<CourseEnrollment> existingOpt = courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid);
        if (existingOpt.isPresent()) {
            CourseEnrollment existing = existingOpt.get();
            if (status.equals(existing.getStatus())) {
                return;
            }
            existing.setStatus(status);
            courseEnrollmentRepository.save(existing);
            return;
        }

        CourseEnrollmentDTO dto = new CourseEnrollmentDTO(
                null,
                studentUuid,
                courseUuid,
                null,
                null,
                status,
                null,
                null,
                null,
                null,
                null,
                null
        );

        courseEnrollmentService.createCourseEnrollment(dto);
    }
}
