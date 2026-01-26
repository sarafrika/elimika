package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    Optional<Course> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    long countByDifficultyUuid(UUID difficultyUuid);

    long countByStatus(ContentStatus status);

    List<Course> findByStatus(ContentStatus status);

    List<Course> findByUuidIn(List<UUID> uuids);

    @Query("select c.uuid from Course c where c.courseCreatorUuid = :courseCreatorUuid")
    List<UUID> findUuidsByCourseCreatorUuid(@Param("courseCreatorUuid") UUID courseCreatorUuid);
}
