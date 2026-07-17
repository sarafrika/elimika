package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseVersionSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseVersionSnapshotRepository extends JpaRepository<CourseVersionSnapshot, Long> {

    Page<CourseVersionSnapshot> findByCourseUuidOrderByVersionNumberDesc(UUID courseUuid, Pageable pageable);

    /** Source of the next version number for a course. */
    Optional<CourseVersionSnapshot> findTopByCourseUuidOrderByVersionNumberDesc(UUID courseUuid);

    Optional<CourseVersionSnapshot> findByUuid(UUID uuid);
}
