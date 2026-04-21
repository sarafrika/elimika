package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.event.student.RegisterStudent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class StudentRegistrationListener {
    private final StudentRepository studentRepository;
    private final UserLookupService userLookupService;

    @ApplicationModuleListener
    void onStudentRegistration(RegisterStudent event) {
        log.info("Processing student registration event: name={}, userUuid={}", event.fullName(), event.userUuid());
        ensureStudentProfileExists(event.userUuid(), event.fullName());
        log.info("Successfully processed student registration event: name={}, userUuid={}", event.fullName(), event.userUuid());
    }

    @ApplicationModuleListener
    void onUserDomainAssigned(UserDomainMappingEvent event) {
        if (!UserDomain.student.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        String fullName = userLookupService.getUserFullName(event.userUuid()).orElse("Unknown");
        log.info("Ensuring student profile exists for userUuid={} after student domain assignment", event.userUuid());
        ensureStudentProfileExists(event.userUuid(), fullName);
    }

    @ApplicationModuleListener
    void onUserDomainRemoved(UserDomainRemovedEvent event) {
        if (!UserDomain.student.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        studentRepository.findByUserUuid(event.userUuid())
                .ifPresent(studentRepository::delete);
    }

    private void ensureStudentProfileExists(java.util.UUID userUuid, String fullName) {
        if (studentRepository.existsByUserUuid(userUuid)) {
            return;
        }

        Student student = new Student();
        student.setUserUuid(userUuid);
        student.setFullName(fullName);
        studentRepository.save(student);
    }
}
