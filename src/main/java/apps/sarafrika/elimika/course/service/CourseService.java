package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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
}