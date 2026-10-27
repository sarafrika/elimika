package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Who may see a learner's programme report: the learner, a guardian sharing academics, and the
 * instructors of the learner's programme classes. Remarks are written by the default instructors only.
 */
@Component("programReportAccess")
@RequiredArgsConstructor
@Slf4j
public class ProgramReportAccess {

    private final DomainSecurityService domainSecurityService;
    private final LearnerProfileLookupService learnerProfiles;
    private final EnrollmentLookupService enrollmentLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final ProgramCourseRepository programCourseRepository;

    public boolean canRead(UUID programUuid, UUID studentUuid) {
        if (programUuid == null || studentUuid == null) {
            return false;
        }
        if (domainSecurityService.isStudentWithUuid(studentUuid)) {
            return true;
        }
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid != null && learnerProfiles.guardianCanViewAcademics(userUuid, studentUuid)) {
            return true;
        }
        UUID instructor = domainSecurityService.getCurrentInstructorUuid();
        return instructor != null && courses(programUuid).stream()
                .anyMatch(course -> enrollmentLookupService.isClassInstructorOfStudentForCourse(instructor, studentUuid, course));
    }

    public boolean canWriteRemarks(UUID programUuid, UUID studentUuid) {
        UUID instructor = domainSecurityService.getCurrentInstructorUuid();
        return instructor != null && studentUuid != null && courses(programUuid).stream()
                .anyMatch(course -> enrollmentLookupService.isDefaultInstructorOfStudentForCourse(instructor, studentUuid, course));
    }

    public boolean canReadClass(UUID classDefinitionUuid) {
        UUID instructor = domainSecurityService.getCurrentInstructorUuid();
        return instructor != null && classDefinitionUuid != null
                && classDefinitionLookupService.findClassDefinitionUuidsByInstructorUuid(instructor).contains(classDefinitionUuid);
    }

    private List<UUID> courses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid).stream()
                .map(ProgramCourse::getCourseUuid).toList();
    }
}
