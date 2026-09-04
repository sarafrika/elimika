package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionAttachmentRepository extends JpaRepository<AssignmentSubmissionAttachment, Long> {
    Optional<AssignmentSubmissionAttachment> findByUuid(UUID uuid);

    List<AssignmentSubmissionAttachment> findBySubmissionUuid(UUID submissionUuid);

    /**
     * The attachments stored at a given storage key. Serving a submitted file by path is only
     * safe once the path has been resolved back to the submission that owns it, and uploads write
     * the same key into both columns, so either may carry it.
     */
    @Query("""
            SELECT a FROM AssignmentSubmissionAttachment a
            WHERE a.storedFilename = :path OR a.fileUrl = :path
            """)
    List<AssignmentSubmissionAttachment> findByStoragePath(@Param("path") String path);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
