package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.shared.event.instructor.RegisterInstructor;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(propagation = Propagation.REQUIRED)
@RequiredArgsConstructor
public class InstructorRegistrationListener {
    private final InstructorRepository instructorRepository;

    @ApplicationModuleListener
    void onInstructorRegistration(RegisterInstructor event) {
        ensureInstructorProfileExists(event.userUuid(), event.fullName());
    }

    @ApplicationModuleListener
    void onUserDomainAssigned(UserDomainMappingEvent event) {
        if (!UserDomain.instructor.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        ensureInstructorProfileExists(event.userUuid(), "Unknown");
    }

    @ApplicationModuleListener
    void onUserDomainRemoved(UserDomainRemovedEvent event) {
        if (!UserDomain.instructor.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        instructorRepository.findByUserUuid(event.userUuid())
                .ifPresent(instructorRepository::delete);
    }

    private void ensureInstructorProfileExists(java.util.UUID userUuid, String fullName) {
        if (instructorRepository.existsByUserUuid(userUuid)) {
            return;
        }

        Instructor instructor = new Instructor();
        instructor.setUserUuid(userUuid);
        instructor.setFullName(fullName);
        instructor.setAdminVerified(false);
        instructorRepository.save(instructor);
    }
}
