package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstructorSkillRepository extends JpaRepository<InstructorSkill, Long>,
    JpaSpecificationExecutor<InstructorSkill>{
}
