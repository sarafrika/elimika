package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>,
        JpaSpecificationExecutor<Assignment> {
    Optional<Assignment> findByUuid(UUID uuid);

    List<Assignment> findByLessonUuidInOrderByCreatedDateAsc(List<UUID> lessonUuids);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
