package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.shared.event.instructor.RegisterInstructor;
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
        Instructor instructor = new Instructor();
        instructor.setUserUuid(event.userUuid());
        instructor.setFullName(event.fullName());
        if(!instructorRepository.existsByUserUuid(event.userUuid())) {
            instructorRepository.save(instructor);
        }
    }
}
