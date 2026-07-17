package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseEditDiffDTO;
import apps.sarafrika.elimika.course.model.Course;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.UUID;

/**
 * Draft-over-live editing for published courses.
 * <p>
 * An edit to a published, admin-approved course is held on a shadow "draft" course row
 * rather than applied to the live row, so the live course keeps serving its last-approved
 * content while the edit awaits review. Approving promotes the draft onto the live course;
 * rejecting simply discards the draft, which is why a rejected edit needs no rollback —
 * the live course was never touched.
 *
 * @see CoursePendingEditService for the review state around a draft
 */
public interface CourseDraftService {

    /**
     * The open draft for a live course, creating one by cloning the live course tree if
     * none exists. Idempotent: repeated calls return the same draft, so a creator making
     * several edits keeps building on one working copy.
     *
     * @param liveCourseUuid the published course being edited
     * @return the draft course row
     */
    Course openDraft(UUID liveCourseUuid);

    /**
     * The open draft for a live course, if one exists.
     */
    Optional<Course> findDraft(UUID liveCourseUuid);

    /**
     * Applies the draft's content onto the live course and deletes the draft.
     * <p>
     * The live course row and its lesson uuids are preserved — lessons are matched by
     * {@code sourceLessonUuid} and updated in place — so enrollments, catalogue entries and
     * learner progress stay intact. Writes a {@code CourseVersionSnapshot} of the resulting
     * live tree.
     *
     * @param liveCourseUuid the published course receiving the edit
     * @param pendingEditUuid the edit being promoted, recorded on the snapshot
     */
    void promote(UUID liveCourseUuid, UUID pendingEditUuid);

    /**
     * Deletes the open draft for a course. The live course is not modified.
     */
    void discard(UUID liveCourseUuid);

    /**
     * Serializes a course's full tree — course fields, category uuids, lessons and their
     * content — for the version history. Media fields hold storage keys, not resolved URLs.
     */
    JsonNode snapshotTree(UUID courseUuid);

    /**
     * What the open draft would change about the live course if approved.
     */
    CourseEditDiffDTO diff(UUID liveCourseUuid);

    /**
     * The course a creator's authoring write should actually land on.
     * <p>
     * For a live, approved course this is its draft — opened on demand — so lesson and content
     * edits are reviewed like any other material change. For anything else it is the course
     * itself. Lets the existing lesson endpoints stay unchanged: they simply operate on the
     * uuid this returns.
     */
    UUID resolveEditableCourseUuid(UUID courseUuid);

    /**
     * The lesson a creator's authoring write should actually land on, given the lesson they
     * named and the course they named it under.
     * <p>
     * Also closes an authorization gap: the lesson endpoints authorize on the path course but
     * previously acted on the lesson uuid alone, so a creator could edit a lesson belonging to
     * someone else's course by naming their own course in the path. This verifies the lesson
     * really belongs to that course before returning anything.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if the lesson
     *         does not belong to the course (or its draft)
     */
    UUID resolveEditableLessonUuid(UUID courseUuid, UUID lessonUuid);

    /**
     * The lesson content a creator's authoring write should land on, given the course, lesson
     * and content they named. Same purpose as {@link #resolveEditableLessonUuid}: verifies the
     * content really hangs off that lesson under that course, and redirects to the draft's copy
     * while an edit is awaiting review.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if the content
     *         does not belong to the lesson, or the lesson to the course
     */
    UUID resolveEditableContentUuid(UUID courseUuid, UUID lessonUuid, UUID contentUuid);

    /**
     * The assessment a creator's authoring write should land on, redirecting to the draft's
     * copy while an edit is awaiting review and verifying the assessment belongs to the course.
     */
    UUID resolveEditableAssessmentUuid(UUID courseUuid, UUID assessmentUuid);

    /**
     * The course requirement a creator's authoring write should land on, redirecting to the
     * draft's copy while an edit is awaiting review.
     */
    UUID resolveEditableRequirementUuid(UUID courseUuid, UUID requirementUuid);

    /**
     * The training requirement a creator's authoring write should land on, redirecting to the
     * draft's copy while an edit is awaiting review.
     */
    UUID resolveEditableTrainingRequirementUuid(UUID courseUuid, UUID requirementUuid);
}
