package apps.sarafrika.elimika.skills.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SkillLevelRepository extends JpaRepository<SkillLevel, Long> {

    List<SkillLevel> findAllByOrderByMinPercentageAsc();
}
