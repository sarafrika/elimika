package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCreatorAchievementRepository extends JpaRepository<CourseCreatorAchievement, Long> {

    List<CourseCreatorAchievement> findByCourseCreatorUuidOrderByCreatedDateAsc(UUID courseCreatorUuid);

    Optional<CourseCreatorAchievement> findByUuidAndCourseCreatorUuid(UUID uuid, UUID courseCreatorUuid);

    long countByCourseCreatorUuid(UUID courseCreatorUuid);
}
