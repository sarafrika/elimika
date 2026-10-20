package apps.sarafrika.elimika.student.security;

import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service("guardianLinkSecurityService")
@RequiredArgsConstructor
@Slf4j
public class GuardianLinkSecurityService {

    private static final List<GuardianShareScope> ACADEMIC_SCOPES = List.of(GuardianShareScope.FULL, GuardianShareScope.ACADEMICS);

    private final DomainSecurityService domainSecurityService;
    private final StudentGuardianLinkRepository guardianLinkRepository;
    private final LearnerProgressLookupService learnerProgressLookupService;

    public boolean canAccessStudent(UUID studentUuid) {
        UUID guardianUserUuid = domainSecurityService.getCurrentUserUuid();
        if (guardianUserUuid == null) {
            log.debug("GuardianLinkSecurityService: no authenticated user present");
            return false;
        }

        if (!domainSecurityService.hasAnyDomain(UserDomain.parent)) {
            log.debug("GuardianLinkSecurityService: user {} lacks parent domain", guardianUserUuid);
            return false;
        }

        boolean hasLink = guardianLinkRepository.existsByStudentUuidAndGuardianUserUuidAndStatus(
                studentUuid,
                guardianUserUuid,
                GuardianLinkStatus.ACTIVE
        );

        log.debug("GuardianLinkSecurityService: guardian {} access to {} => {}", guardianUserUuid, studentUuid, hasLink);
        return hasLink;
    }

    /** A parent may read a child's marks over an active link that shares academics, never attendance-only. */
    public boolean canViewEnrollmentAcademics(UUID courseEnrollmentUuid) {
        UUID guardianUserUuid = domainSecurityService.getCurrentUserUuid();
        if (guardianUserUuid == null || courseEnrollmentUuid == null || !domainSecurityService.hasAnyDomain(UserDomain.parent)) {
            return false;
        }
        return learnerProgressLookupService.findStudentUuidOfCourseEnrollment(courseEnrollmentUuid)
                .map(studentUuid -> guardianLinkRepository.existsByStudentUuidAndGuardianUserUuidAndStatusAndShareScopeIn(
                        studentUuid, guardianUserUuid, GuardianLinkStatus.ACTIVE, ACADEMIC_SCOPES))
                .orElse(false);
    }
}
