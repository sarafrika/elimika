package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.event.student.GuardianConsentRecordedEvent;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Establishes a guardian's link to a learner once that guardian has consented to the
 * learner joining an organisation.
 * <p>
 * The consent itself is recorded by the tenancy module, which owns the invitation but may
 * not depend on this one. The link is created here, where the learner profile lives.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class GuardianConsentListener {

    private final StudentRepository studentRepository;
    private final StudentGuardianLinkRepository guardianLinkRepository;
    private final UserLookupService userLookupService;

    @ApplicationModuleListener
    void onGuardianConsentRecorded(GuardianConsentRecordedEvent event) {
        // The learner profile is created by its own listener off the same commit, so
        // ordering between the two is not guaranteed. Create it here if it has not landed
        // yet rather than dropping the consent on the floor.
        Student student = studentRepository.findByUserUuid(event.studentUserUuid())
                .orElseGet(() -> createProfile(event.studentUserUuid()));

        guardianLinkRepository
                .findByStudentUuidAndGuardianUserUuidAndStatusIn(
                        student.getUuid(),
                        event.guardianUserUuid(),
                        List.of(GuardianLinkStatus.PENDING, GuardianLinkStatus.ACTIVE, GuardianLinkStatus.REVOKED))
                .ifPresentOrElse(
                        existing -> activate(existing, event),
                        () -> create(student.getUuid(), event));
    }

    private Student createProfile(UUID userUuid) {
        Student student = new Student();
        student.setUserUuid(userUuid);
        student.setFullName(userLookupService.getUserFullName(userUuid).orElse("Unknown"));
        return studentRepository.save(student);
    }

    private void activate(StudentGuardianLink link, GuardianConsentRecordedEvent event) {
        link.setStatus(GuardianLinkStatus.ACTIVE);
        link.setShareScope(shareScope(event.shareScope()));
        link.setRelationshipType(relationshipType(event.relationshipType()));
        link.setLinkedDate(LocalDateTime.now());
        link.setRevokedDate(null);
        link.setRevokedBy(null);
        guardianLinkRepository.save(link);

        log.info("Reactivated guardian link between guardian {} and student {}",
                event.guardianUserUuid(), link.getStudentUuid());
    }

    private void create(UUID studentUuid, GuardianConsentRecordedEvent event) {
        StudentGuardianLink link = new StudentGuardianLink();
        link.setStudentUuid(studentUuid);
        link.setGuardianUserUuid(event.guardianUserUuid());
        link.setRelationshipType(relationshipType(event.relationshipType()));
        link.setShareScope(shareScope(event.shareScope()));
        link.setStatus(GuardianLinkStatus.ACTIVE);
        link.setPrimaryGuardian(guardianLinkRepository
                .findByStudentUuidAndStatus(studentUuid, GuardianLinkStatus.ACTIVE).isEmpty());
        link.setLinkedDate(LocalDateTime.now());
        link.setNotes("Established when consenting to an organisation invitation");
        guardianLinkRepository.save(link);

        log.info("Created guardian link between guardian {} and student {} after organisation consent",
                event.guardianUserUuid(), studentUuid);
    }

    private static GuardianRelationshipType relationshipType(String value) {
        if (value == null || value.isBlank()) {
            return GuardianRelationshipType.GUARDIAN;
        }
        try {
            return GuardianRelationshipType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GuardianRelationshipType.GUARDIAN;
        }
    }

    private static GuardianShareScope shareScope(String value) {
        if (value == null || value.isBlank()) {
            return GuardianShareScope.FULL;
        }
        try {
            return GuardianShareScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GuardianShareScope.FULL;
        }
    }
}
