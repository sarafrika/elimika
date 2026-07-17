package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CoursePendingEditDTO;
import apps.sarafrika.elimika.course.dto.CourseEditDiffDTO;
import apps.sarafrika.elimika.course.dto.CourseVersionSnapshotDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Review workflow around a draft-over-live course edit.
 * <p>
 * Pairs with {@link CourseDraftService}, which owns the draft content itself: this service
 * owns who submitted an edit, who reviewed it, and how it was resolved.
 */
public interface CoursePendingEditService {

    /**
     * Records that the course has an edit awaiting review, or refreshes the existing one if
     * the creator is still working on it. Idempotent per course — at most one edit can be
     * pending, which the database enforces.
     */
    CoursePendingEditDTO submitOrRefresh(UUID courseUuid, UUID draftCourseUuid);

    Optional<CoursePendingEditDTO> findPending(UUID courseUuid);

    /** Every edit ever submitted for a course, newest first. */
    Page<CoursePendingEditDTO> getHistory(UUID courseUuid, Pageable pageable);

    /** The admin review queue: every course with an edit awaiting a decision. */
    Page<CoursePendingEditDTO> getPendingQueue(Pageable pageable);

    /**
     * Promotes the pending edit onto the live course and marks it approved.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if no edit is pending
     */
    CoursePendingEditDTO approve(UUID courseUuid, String reason);

    /**
     * Discards the pending edit and marks it rejected. The live course is not modified and
     * keeps its approval — the published content was never at fault.
     */
    CoursePendingEditDTO reject(UUID courseUuid, String reason);

    /** The creator abandons their own pending edit. */
    CoursePendingEditDTO withdraw(UUID courseUuid);

    /** What the pending edit would change if approved. */
    CourseEditDiffDTO diff(UUID courseUuid);

    /** Approved content history for a course. */
    Page<CourseVersionSnapshotDTO> getVersions(UUID courseUuid, Pageable pageable);

    /** Whether an edit to this course must go through review rather than apply live. */
    boolean requiresReview(UUID courseUuid);
}
