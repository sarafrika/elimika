package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.common.event.student.RegisterStudent;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentRegistrationListener {
    private final StudentRepository studentRepository;

    @ApplicationModuleListener
    void onStudentRegistration(RegisterStudent event) {
        Student student = new Student();
        student.setFullName(event.fullName());
        student.setUserUuid(event.userUuid());

        studentRepository.save(student);
    }
}
