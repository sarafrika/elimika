package apps.sarafrika.elimika.common.repository;

import apps.sarafrika.elimika.common.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
