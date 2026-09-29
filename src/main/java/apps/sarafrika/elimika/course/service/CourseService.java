package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CourseService {
    CourseDTO createCourse(CourseDTO courseDTO);

    CourseDTO getCourseByUuid(UUID uuid);

    Page<CourseDTO> getAllCourses(Pageable pageable);

    CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO);

    void deleteCourse(UUID uuid);

    /**
     * Unscoped search over every course row, shadow drafts included. For platform-admin and
     * system-internal use; endpoints serving arbitrary callers use {@link #searchVisible}.
     */
    Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Search limited to the courses the current caller may see: everything for a platform admin;
     * otherwise the public catalogue, the caller's own courses, and courses they are enrolled in
     * or approved to teach — never shadow drafts.
     */
    Page<CourseDTO> searchVisible(Map<String, String> searchParams, Pageable pageable);

    /**
     * A single course as the current caller may see it. Unpublished (draft or in-review) courses
     * resolve only for a platform admin, their author, an enrolled learner or someone approved to
     * teach them; shadow drafts only for a platform admin or their author. Anyone else gets
     * {@link apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException}.
     */
    CourseDTO getVisibleCourseByUuid(UUID uuid);

    /**
     * The courses an instructor may deliver — authored by their user, approved for them personally,
     * approved for an organisation they teach for, or inside a programme approved on either footing —
     * limited to what the current caller may see.
     */
    Page<CourseDTO> getCoursesForInstructor(UUID instructorUuid, Pageable pageable);

    boolean isCourseReadyForPublishing(UUID uuid);

    CourseDTO publishCourse(UUID uuid);

    CourseDTO approveCourse(UUID uuid, String reason);

    CourseDTO unapproveCourse(UUID uuid, String reason, ModerationAction action);

    boolean isCourseApproved(UUID uuid);

    double getCourseCompletionRate(UUID uuid);

    CourseDTO uploadThumbnail(UUID courseUuid, MultipartFile thumbnail);

    CourseDTO uploadBanner(UUID courseUuid, MultipartFile banner);

    CourseDTO uploadIntroVideo(UUID courseUuid, MultipartFile introVideo);

    /**
     * Unpublish a course, making it unavailable for new enrollments
     */
    CourseDTO unpublishCourse(UUID uuid);

    /**
     * Archive a course, making it completely unavailable
     */
    CourseDTO archiveCourse(UUID uuid);

    /**
     * Check if a course can be unpublished (business rules validation)
     */
    boolean canUnpublishCourse(UUID uuid);

    /**
     * Get course status transition options
     */
    List<ContentStatus> getAvailableStatusTransitions(UUID uuid);
}
