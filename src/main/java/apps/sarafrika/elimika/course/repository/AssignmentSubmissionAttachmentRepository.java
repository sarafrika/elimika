package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionAttachmentRepository extends JpaRepository<AssignmentSubmissionAttachment, Long> {
    Optional<AssignmentSubmissionAttachment> findByUuid(UUID uuid);

    List<AssignmentSubmissionAttachment> findBySubmissionUuid(UUID submissionUuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
