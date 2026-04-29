package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentGuardianLookupServiceImpl implements StudentGuardianLookupService {

    private final StudentGuardianLinkRepository guardianLinkRepository;

    @Override
    public List<GuardianStudentAccess> findActiveGuardianStudents(UUID guardianUserUuid) {
        if (guardianUserUuid == null) {
            return List.of();
        }
        return guardianLinkRepository.findByGuardianUserUuidAndStatus(guardianUserUuid, GuardianLinkStatus.ACTIVE)
                .stream()
                .map(link -> new GuardianStudentAccess(link.getStudentUuid(), link.getShareScope()))
                .toList();
    }

    @Override
    public List<UUID> findActiveGuardianStudentUuidsWithFullAccess(UUID guardianUserUuid) {
        if (guardianUserUuid == null) {
            return List.of();
        }
        return guardianLinkRepository.findByGuardianUserUuidAndStatus(guardianUserUuid, GuardianLinkStatus.ACTIVE)
                .stream()
                .filter(link -> GuardianShareScope.FULL.equals(link.getShareScope()))
                .map(link -> link.getStudentUuid())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
