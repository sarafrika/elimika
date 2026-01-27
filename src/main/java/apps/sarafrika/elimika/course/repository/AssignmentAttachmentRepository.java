package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {
    Optional<AssignmentAttachment> findByUuid(UUID uuid);

    List<AssignmentAttachment> findByAssignmentUuid(UUID assignmentUuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
