package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseLearningObjectiveDTO;
import apps.sarafrika.elimika.course.repository.CourseLearningObjectiveRepository;
import apps.sarafrika.elimika.course.service.CourseLearningObjectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseLearningObjectiveServiceImpl implements CourseLearningObjectiveService {

    private static final String ERROR_COURSE_LEARNING_OBJECTIVE_NOT_FOUND = "Course learning objective not found.";
    private static final String COURSE_LEARNING_OBJECTIVE_FOUND_SUCCESS = "Course learning objective has been retrieved successfully.";
    private static final String COURSE_LEARNING_OBJECTIVE_CREATED_SUCCESS = "Course learning objective has been persisted successfully.";
    private static final String COURSE_LEARNING_OBJECTIVE_UPDATED_SUCCESS = "Course learning objective has been updated successfully.";

    private final CourseLearningObjectiveRepository courseLearningObjectiveRepository;

    @Override
    public CourseLearningObjectiveDTO createCourseLearningObjective(CourseLearningObjectiveDTO courseLearningObjectiveDTO) {
        return null;
    }

    @Override
    public CourseLearningObjectiveDTO getCourseLearningObjectiveByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<CourseLearningObjectiveDTO> getAllCourseLearningObjectives(Pageable pageable) {
        return null;
    }

    @Override
    public CourseLearningObjectiveDTO updateCourseLearningObjective(UUID uuid, CourseLearningObjectiveDTO courseLearningObjectiveDTO) {
        return null;
    }

    @Override
    public void deleteCourseLearningObjective(UUID uuid) {

    }

    @Override
    public Page<CourseLearningObjectiveDTO> searchCourseLearningObjectives(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
