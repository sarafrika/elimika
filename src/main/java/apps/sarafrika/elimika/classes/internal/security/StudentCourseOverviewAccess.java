package apps.sarafrika.elimika.classes.internal.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Who may read a learner's course overview: the learner, a guardian whose share covers academics,
 * or a platform admin. It carries platform-wide course progress, so sharing a class is not enough.
 */
@Component("studentCourseOverviewAccess")
@RequiredArgsConstructor
public class StudentCourseOverviewAccess {

    private final DomainSecurityService domainSecurityService;
    private final LearnerProfileLookupService learnerProfileLookupService;

    public boolean canRead(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        return domainSecurityService.isStudentWithUuid(studentUuid)
                || domainSecurityService.isPlatformAdmin()
                || learnerProfileLookupService.guardianCanViewAcademics(
                        domainSecurityService.getCurrentUserUuid(), studentUuid);
    }
}
