package apps.sarafrika.elimika.instructor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstructorExperienceRepository extends JpaRepository<InstructorExperienceRepository, Long>,
    JpaSpecificationExecutor<InstructorExperienceRepository>{
}
