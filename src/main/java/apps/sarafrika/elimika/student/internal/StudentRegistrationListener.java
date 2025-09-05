package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.event.student.RegisterStudent;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
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

    @ApplicationModuleListener
    void onStudentRegistration(RegisterStudent event) {
        log.info("Processing student registration event: name={}, userUuid={}", event.fullName(), event.userUuid());
        Student student = new Student();
        student.setUserUuid(event.userUuid());
        student.setFullName(event.fullName());

        if(!studentRepository.existsByUserUuid(event.userUuid())) {
            studentRepository.save(student);
        }

        log.info("Successfully processed student registration event: name={}, userUuid={}", event.fullName(), event.userUuid());
    }
}
