package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CoursePendingEdit;
import apps.sarafrika.elimika.course.util.enums.PendingEditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoursePendingEditRepository extends JpaRepository<CoursePendingEdit, Long> {

    /**
     * The open edit for a course, if any. At most one can exist — enforced by the partial
     * unique index uq_cpe_one_pending_per_course.
     */
    Optional<CoursePendingEdit> findByCourseUuidAndStatus(UUID courseUuid, PendingEditStatus status);

    Optional<CoursePendingEdit> findByUuid(UUID uuid);

    Optional<CoursePendingEdit> findByDraftCourseUuid(UUID draftCourseUuid);

    /** Drives the admin review queue. */
    Page<CoursePendingEdit> findByStatusOrderBySubmittedAtDesc(PendingEditStatus status, Pageable pageable);

    Page<CoursePendingEdit> findByCourseUuidOrderBySubmittedAtDesc(UUID courseUuid, Pageable pageable);

    boolean existsByCourseUuidAndStatus(UUID courseUuid, PendingEditStatus status);
}
