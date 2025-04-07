package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.common.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
}
