package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.spi.GuardianChildLookupService;
import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import apps.sarafrika.elimika.student.dto.GuardianChildRequestDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkRequest;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.GuardianAccessService;
import apps.sarafrika.elimika.student.service.GuardianChildService;
import apps.sarafrika.elimika.student.service.StudentService;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.ManagedLearnerAccountService;
import apps.sarafrika.elimika.tenancy.spi.ManagedLearnerAccountService.ManagedLearnerAccount;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianChildServiceImpl implements GuardianChildService {

    private static final Set<GuardianRelationshipType> CHILD_RELATIONSHIPS =
            EnumSet.of(GuardianRelationshipType.PARENT, GuardianRelationshipType.GUARDIAN);

    private final ManagedLearnerAccountService managedLearnerAccountService;
    private final StudentService studentService;
    private final GuardianAccessService guardianAccessService;
    private final StudentRepository studentRepository;
    private final UserLookupService userLookupService;
    private final GuardianChildLookupService guardianChildLookupService;

    @Override
    @Transactional
    public GuardianStudentLinkDTO addChild(UUID guardianUserUuid, GuardianChildRequestDTO request) {
        if (!CHILD_RELATIONSHIPS.contains(request.relationshipType())) {
            throw new IllegalArgumentException("Relationship must be PARENT or GUARDIAN");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (ageOn(request.dob(), today) >= MinorLearnerLookupService.AGE_OF_MAJORITY) {
            throw new IllegalArgumentException("Learners aged %d or over need their own account. Ask them to invite you as a guardian."
                    .formatted(MinorLearnerLookupService.AGE_OF_MAJORITY));
        }
        // An unknown date of birth counts as an adult, as it does for learners.
        userLookupService.getUserDateOfBirth(guardianUserUuid)
                .filter(dob -> ageOn(dob, today) < MinorLearnerLookupService.AGE_OF_MAJORITY)
                .ifPresent(dob -> {
                    throw new IllegalArgumentException("A guardian must be at least %d years old."
                            .formatted(MinorLearnerLookupService.AGE_OF_MAJORITY));
                });
        requireNotAlreadyAdded(guardianUserUuid, request);

        UUID childUserUuid = managedLearnerAccountService.createManagedLearnerAccount(new ManagedLearnerAccount(
                request.firstName(), request.middleName(), request.lastName(), request.dob()));

        // createStudent applies the learner age gate and grants the child the student domain.
        StudentDTO student = studentService.createStudent(new StudentDTO(null, childUserUuid, null, null,
                null, null, null, null, null, null, null, null, null, null));

        // The database trigger derives full_name on insert, but this transaction still holds the entity without it.
        studentRepository.findByUuid(student.uuid())
                .ifPresent(saved -> saved.setFullName(fullName(request)));

        GuardianStudentLinkDTO link = guardianAccessService.createOrUpdateLink(new GuardianStudentLinkRequest(
                student.uuid(), guardianUserUuid, request.relationshipType(), GuardianShareScope.FULL, true,
                null), guardianUserUuid);
        log.info("Guardian {} added managed learner {}", guardianUserUuid, student.uuid());
        return link;
    }

    /** Guards against a double submit: the same name and date of birth among the guardian's linked learners. */
    private void requireNotAlreadyAdded(UUID guardianUserUuid, GuardianChildRequestDTO request) {
        if (guardianChildLookupService.findChildUserUuid(guardianUserUuid, request.firstName(), request.lastName(),
                request.dob()).isPresent()) {
            throw new DuplicateResourceException("You have already added this child.");
        }
    }

    /** Same shape as {@code generate_student_full_name_from_user}. */
    private static String fullName(GuardianChildRequestDTO request) {
        String middle = request.middleName() == null || request.middleName().isBlank()
                ? "" : " " + request.middleName().trim();
        return request.firstName().trim() + middle + " " + request.lastName().trim();
    }

    private static int ageOn(LocalDate dob, LocalDate date) {
        return Period.between(dob, date).getYears();
    }
}
