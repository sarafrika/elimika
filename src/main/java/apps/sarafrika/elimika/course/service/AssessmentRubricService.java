package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AssessmentRubricService {
    AssessmentRubricDTO createAssessmentRubric(AssessmentRubricDTO assessmentRubricDTO);

    AssessmentRubricDTO getAssessmentRubricByUuid(UUID uuid);

    Page<AssessmentRubricDTO> getAllAssessmentRubrics(Pageable pageable);

    AssessmentRubricDTO updateAssessmentRubric(UUID uuid, AssessmentRubricDTO assessmentRubricDTO);

    void deleteAssessmentRubric(UUID uuid);

    Page<AssessmentRubricDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Finds all public rubrics available for reuse across courses.
     *
     * @param pageable pagination parameters
     * @return page of public rubrics
     */
    Page<AssessmentRubricDTO> getPublicRubrics(Pageable pageable);

    /**
     * Searches public rubrics by various criteria.
     *
     * @param searchTerm term to search in title and description
     * @param rubricType optional rubric type filter
     * @param pageable pagination parameters
     * @return page of matching public rubrics
     */
    Page<AssessmentRubricDTO> searchPublicRubrics(String searchTerm, String rubricType, Pageable pageable);

    /**
     * Gets rubrics created by a specific instructor.
     *
     * @param instructorUuid the UUID of the instructor
     * @param includePrivate whether to include private rubrics
     * @param pageable pagination parameters
     * @return page of instructor's rubrics
     */
    Page<AssessmentRubricDTO> getInstructorRubrics(UUID instructorUuid, boolean includePrivate, Pageable pageable);

    /**
     * Gets general rubrics not tied to any specific course.
     *
     * @param pageable pagination parameters
     * @return page of general rubrics
     */
    Page<AssessmentRubricDTO> getGeneralRubrics(Pageable pageable);

    /**
     * Gets popular public rubrics based on usage.
     *
     * @param pageable pagination parameters
     * @return page of popular rubrics
     */
    Page<AssessmentRubricDTO> getPopularRubrics(Pageable pageable);

    /**
     * Gets rubrics by content status.
     *
     * @param status the content status to filter by
     * @param pageable pagination parameters
     * @return page of rubrics with the specified status
     */
    Page<AssessmentRubricDTO> getRubricsByStatus(ContentStatus status, Pageable pageable);

    /**
     * Gets usage statistics for all rubrics.
     *
     * @return map containing counts of public rubrics, total rubrics, etc.
     */
    Map<String, Long> getRubricStatistics();

    /**
     * Gets usage statistics for an instructor's rubrics.
     *
     * @param instructorUuid the UUID of the instructor
     * @return map containing counts of the instructor's rubrics
     */
    Map<String, Long> getInstructorRubricStatistics(UUID instructorUuid);
}





