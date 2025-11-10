package apps.sarafrika.elimika.student.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("guardianLinkSecurityService")
@RequiredArgsConstructor
@Slf4j
public class GuardianLinkSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final StudentGuardianLinkRepository guardianLinkRepository;

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
}
