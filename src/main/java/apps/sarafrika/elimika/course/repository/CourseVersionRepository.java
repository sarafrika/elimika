package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseVersionRepository extends JpaRepository<CourseVersion, Long> {

    Optional<CourseVersion> findByUuid(UUID uuid);

    Optional<CourseVersion> findByUuidAndCourseUuid(UUID uuid, UUID courseUuid);

    List<CourseVersion> findByCourseUuidOrderByVersionNumberDesc(UUID courseUuid);

    Optional<CourseVersion> findTopByCourseUuidOrderByVersionNumberDesc(UUID courseUuid);

    boolean existsByCourseUuidAndSnapshotHash(UUID courseUuid, String snapshotHash);
}
