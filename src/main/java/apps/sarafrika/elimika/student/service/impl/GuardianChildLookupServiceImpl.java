package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.spi.GuardianChildLookupService;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianChildLookupServiceImpl implements GuardianChildLookupService {

    private final StudentRepository studentRepository;
    private final StudentGuardianLinkRepository guardianLinkRepository;
    private final UserLookupService userLookupService;

    @Override
    public Optional<UUID> findChildUserUuid(UUID guardianUserUuid, String firstName, String lastName, LocalDate dob) {
        List<UUID> studentUuids = guardianLinkRepository.findByGuardianUserUuidAndStatusIn(guardianUserUuid,
                        EnumSet.of(GuardianLinkStatus.ACTIVE, GuardianLinkStatus.PENDING)).stream()
                .map(StudentGuardianLink::getStudentUuid)
                .toList();
        if (studentUuids.isEmpty()) {
            return Optional.empty();
        }
        String name = normaliseName(firstName + " " + lastName);
        return studentRepository.findByUuidIn(studentUuids).stream()
                .map(Student::getUserUuid)
                .filter(Objects::nonNull)
                .filter(userUuid -> dob.equals(userLookupService.getUserDateOfBirth(userUuid).orElse(null))
                        && userLookupService.getUserFullName(userUuid).map(GuardianChildLookupServiceImpl::normaliseName)
                        .filter(name::equals).isPresent())
                .findFirst();
    }

    private static String normaliseName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
