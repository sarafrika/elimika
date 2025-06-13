package apps.sarafrika.elimika.common.util;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class UserDomainSelector {
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;

    // TODO: Revisit
    public List<UserDomain> selectUserDomains(UUID userUUid){
        return null;
    }
}
