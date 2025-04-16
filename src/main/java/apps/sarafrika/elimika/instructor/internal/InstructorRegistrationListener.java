package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.common.event.instructor.RegisterInstructor;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstructorRegistrationListener {
    private final InstructorRepository instructorRepository;

    @ApplicationModuleListener
    void onInstructorRegistration(RegisterInstructor event) {
        Instructor instructor = new Instructor();
        instructor.setUserUuid(event.userUuid());
        instructorRepository.save(instructor);
    }
}
