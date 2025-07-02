package apps.sarafrika.elimika.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseAssessmentScoreService {
    CourseAssessmentScoreDTO createCourseAssessmentScore(CourseAssessmentScoreDTO courseAssessmentScoreDTO);

    CourseAssessmentScoreDTO getCourseAssessmentScoreByUuid(UUID uuid);

    Page<CourseAssessmentScoreDTO> getAllCourseAssessmentScores(Pageable pageable);

    CourseAssessmentScoreDTO updateCourseAssessmentScore(UUID uuid, CourseAssessmentScoreDTO courseAssessmentScoreDTO);

    void deleteCourseAssessmentScore(UUID uuid);

    Page<CourseAssessmentScoreDTO> search(Map<String, String> searchParams, Pageable pageable);
}