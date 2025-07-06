package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseAssessmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseAssessmentService {
    CourseAssessmentDTO createCourseAssessment(CourseAssessmentDTO courseAssessmentDTO);

    CourseAssessmentDTO getCourseAssessmentByUuid(UUID uuid);

    Page<CourseAssessmentDTO> getAllCourseAssessments(Pageable pageable);

    CourseAssessmentDTO updateCourseAssessment(UUID uuid, CourseAssessmentDTO courseAssessmentDTO);

    void deleteCourseAssessment(UUID uuid);

    Page<CourseAssessmentDTO> search(Map<String, String> searchParams, Pageable pageable);
}
