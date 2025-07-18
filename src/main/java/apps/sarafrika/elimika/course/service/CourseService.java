package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
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

    Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable);

    boolean isCourseReadyForPublishing(UUID uuid);

    CourseDTO publishCourse(UUID uuid);

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