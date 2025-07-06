package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseEnrollmentService {
    CourseEnrollmentDTO createCourseEnrollment(CourseEnrollmentDTO courseEnrollmentDTO);

    CourseEnrollmentDTO getCourseEnrollmentByUuid(UUID uuid);

    Page<CourseEnrollmentDTO> getAllCourseEnrollments(Pageable pageable);

    CourseEnrollmentDTO updateCourseEnrollment(UUID uuid, CourseEnrollmentDTO courseEnrollmentDTO);

    void deleteCourseEnrollment(UUID uuid);

    Page<CourseEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable);
}

