package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseSkillRepository extends JpaRepository<CourseSkill, Long> {

    List<CourseSkill> findByCourseUuidOrderByWeightDescIdAsc(UUID courseUuid);

    List<CourseSkill> findByCourseUuidInOrderByWeightDescIdAsc(Collection<UUID> courseUuids);
}
