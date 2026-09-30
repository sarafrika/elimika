package apps.sarafrika.elimika.student.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Who may see or change a learner's skill goals: the learner reads and writes them; a platform admin
 * and a guardian whose share covers academics (FULL or ACADEMICS) may read them.
 */
@Service("learnerSkillGoalSecurityService")
@RequiredArgsConstructor
public class LearnerSkillGoalSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final LearnerProfileLookupService learnerProfileLookupService;

    public boolean canRead(UUID studentUuid) {
        return domainSecurityService.isStudentWithUuid(studentUuid)
                || domainSecurityService.isPlatformAdmin()
                || learnerProfileLookupService.guardianCanViewAcademics(domainSecurityService.getCurrentUserUuid(), studentUuid);
    }

    public boolean canWrite(UUID studentUuid) {
        return domainSecurityService.isStudentWithUuid(studentUuid);
    }
}
