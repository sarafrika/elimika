package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstructorEducationRepository extends JpaRepository<InstructorEducation,Long>,
        JpaSpecificationExecutor<InstructorEducation> {
}
