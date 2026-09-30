package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.student.internal.LearnerSkillGoalStore;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Learner facts for course recommendations. The age is computed by tenancy, which owns the date of
 * birth; only the number crosses into this module and on to course.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearnerProfileLookupServiceImpl implements LearnerProfileLookupService {

    /** Share scopes that include a learner's academic record. */
    private static final Set<GuardianShareScope> ACADEMIC_SCOPES = Set.of(GuardianShareScope.FULL, GuardianShareScope.ACADEMICS);

    private final StudentRepository studentRepository;
    private final UserLookupService userLookupService;
    private final StudentGuardianLookupService studentGuardianLookupService;
    private final LearnerSkillGoalStore skillGoalStore;

    @Override
    public Optional<UUID> findStudentUuidByUserUuid(UUID userUuid) {
        if (userUuid == null) {
            return Optional.empty();
        }
        return studentRepository.findByUserUuid(userUuid).map(Student::getUuid);
    }

    @Override
    public OptionalInt findLearnerAge(UUID studentUuid, LocalDate asOf) {
        if (studentUuid == null || asOf == null) {
            return OptionalInt.empty();
        }
        return studentRepository.findByUuid(studentUuid)
                .map(Student::getUserUuid)
                .map(userUuid -> userLookupService.findUserAgeInYears(userUuid, asOf))
                .orElse(OptionalInt.empty());
    }

    @Override
    public List<UUID> findSkillGoalUuids(UUID studentUuid) {
        return studentUuid == null ? List.of() : skillGoalStore.findSkillUuids(studentUuid);
    }

    @Override
    public boolean guardianCanViewAcademics(UUID guardianUserUuid, UUID studentUuid) {
        if (guardianUserUuid == null || studentUuid == null) {
            return false;
        }
        return studentGuardianLookupService.findActiveGuardianStudents(guardianUserUuid).stream()
                .anyMatch(access -> studentUuid.equals(access.studentUuid())
                        && ACADEMIC_SCOPES.contains(access.shareScope()));
    }
}
