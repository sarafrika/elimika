package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CourseLearningObjectiveNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseLearningObjectiveResponseDTO;
import apps.sarafrika.elimika.course.persistence.CourseLearningObjective;
import apps.sarafrika.elimika.course.persistence.CourseLearningObjectiveFactory;
import apps.sarafrika.elimika.course.persistence.CourseLearningObjectiveRepository;
import apps.sarafrika.elimika.course.service.CourseLearningObjectiveService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearningObjectiveServiceImpl implements CourseLearningObjectiveService {

    private static final String ERROR_COURSE_LEARNING_OBJECTIVE_NOT_FOUND = "Course learning objective not found.";
    private static final String COURSE_LEARNING_OBJECTIVE_FOUND_SUCCESS = "Course learning objective has been retrieved successfully.";
    private static final String COURSE_LEARNING_OBJECTIVE_CREATED_SUCCESS = "Course learning objective has been persisted successfully.";
    private static final String COURSE_LEARNING_OBJECTIVE_UPDATED_SUCCESS = "Course learning objective has been updated successfully.";

    private final CourseLearningObjectiveRepository courseLearningObjectiveRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<CourseLearningObjectiveResponseDTO> findCourseLearningObjective(Long id) {

        CourseLearningObjective courseLearningObjective = findCourseLearningObjectiveById(id);

        return new ResponseDTO<>(CourseLearningObjectiveResponseDTO.from(courseLearningObjective), HttpStatus.OK.value(), COURSE_LEARNING_OBJECTIVE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private CourseLearningObjective findCourseLearningObjectiveById(Long id) {

        return courseLearningObjectiveRepository.findById(id).orElseThrow(() -> new CourseLearningObjectiveNotFoundException(ERROR_COURSE_LEARNING_OBJECTIVE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<List<CourseLearningObjectiveResponseDTO>> findAllCourseLearningObjectives(Long courseId) {

        List<CourseLearningObjective> courseLearningObjectives = courseLearningObjectiveRepository.findAllByCourseId(courseId);

        List<CourseLearningObjectiveResponseDTO> courseLearningObjectiveResponseDTOS = courseLearningObjectives.stream()
                .map(CourseLearningObjectiveResponseDTO::from)
                .toList();

        return new ResponseDTO<>(courseLearningObjectiveResponseDTOS, HttpStatus.OK.value(), COURSE_LEARNING_OBJECTIVE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<CourseLearningObjectiveResponseDTO> createCourseLearningObjective(CreateCourseLearningObjectiveRequestDTO createCourseLearningObjectiveRequestDTO, Long courseId) {

        CourseLearningObjective courseLearningObjective = CourseLearningObjectiveFactory.create(createCourseLearningObjectiveRequestDTO);
        courseLearningObjective.setCourseId(courseId);

        CourseLearningObjective savedCourseLearningObjective = courseLearningObjectiveRepository.save(courseLearningObjective);

        return new ResponseDTO<>(CourseLearningObjectiveResponseDTO.from(savedCourseLearningObjective), HttpStatus.CREATED.value(), COURSE_LEARNING_OBJECTIVE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<List<CourseLearningObjectiveResponseDTO>> createCourseLearningObjectives(List<CreateCourseLearningObjectiveRequestDTO> createCourseLearningObjectiveRequestDTOS, Long courseId) {

        List<CourseLearningObjective> courseLearningObjectives = createCourseLearningObjectiveRequestDTOS.stream()
                .map(createCourseLearningObjectiveRequestDTO -> {
                    CourseLearningObjective courseLearningObjective = CourseLearningObjectiveFactory.create(createCourseLearningObjectiveRequestDTO);
                    courseLearningObjective.setCourseId(courseId);

                    return courseLearningObjective;
                })
                .toList();

        List<CourseLearningObjective> savedCourseLearningObjectives = courseLearningObjectiveRepository.saveAll(courseLearningObjectives);

        List<CourseLearningObjectiveResponseDTO> courseLearningObjectiveResponseDTOS = savedCourseLearningObjectives.stream()
                .map(CourseLearningObjectiveResponseDTO::from)
                .toList();

        return new ResponseDTO<>(courseLearningObjectiveResponseDTOS, HttpStatus.CREATED.value(), COURSE_LEARNING_OBJECTIVE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<CourseLearningObjectiveResponseDTO> updateCourseLearningObjective(Long id, UpdateCourseLearningObjectiveRequestDTO updateCourseLearningObjectiveRequestDTO) {

        CourseLearningObjective courseLearningObjective = findCourseLearningObjectiveById(id);

        CourseLearningObjectiveFactory.update(courseLearningObjective, updateCourseLearningObjectiveRequestDTO);

        CourseLearningObjective savedCourseLearningObjective = courseLearningObjectiveRepository.save(courseLearningObjective);

        return new ResponseDTO<>(CourseLearningObjectiveResponseDTO.from(savedCourseLearningObjective), HttpStatus.OK.value(), COURSE_LEARNING_OBJECTIVE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<List<CourseLearningObjectiveResponseDTO>> updateCourseLearningObjectives(List<UpdateCourseLearningObjectiveRequestDTO> updateCourseLearningObjectiveRequestDTOS) {
        List<Long> ids = updateCourseLearningObjectiveRequestDTOS.stream().map(UpdateCourseLearningObjectiveRequestDTO::id).toList();

        List<CourseLearningObjective> foundObjectives = findAllCourseLearningObjectivesByIds(ids);

        if (foundObjectives.size() != ids.size()) {
            Set<Long> foundIds = foundObjectives.stream().map(CourseLearningObjective::getId).collect(Collectors.toSet());

            List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();

            throw new CourseLearningObjectiveNotFoundException(ERROR_COURSE_LEARNING_OBJECTIVE_NOT_FOUND);
        }

        Map<Long, CourseLearningObjective> objectivesMap = foundObjectives.stream()
                .collect(Collectors.toMap(CourseLearningObjective::getId, objective -> objective));

        List<CourseLearningObjective> updatedObjectives = updateCourseLearningObjectiveRequestDTOS.stream()
                .map(updateDTO -> {
                    CourseLearningObjective objective = objectivesMap.get(updateDTO.id());
                    CourseLearningObjectiveFactory.update(objective, updateDTO);
                    return objective;
                })
                .toList();

        List<CourseLearningObjective> savedObjectives = courseLearningObjectiveRepository.saveAll(updatedObjectives);

        return new ResponseDTO<>(savedObjectives.stream().map(CourseLearningObjectiveResponseDTO::from).toList(), HttpStatus.OK.value(), COURSE_LEARNING_OBJECTIVE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    private List<CourseLearningObjective> findAllCourseLearningObjectivesByIds(List<Long> ids) {

        return courseLearningObjectiveRepository.findByIdIn(ids);
    }

    @Transactional
    @Override
    public void deleteCourseLearningObjective(Long id) {

        CourseLearningObjective courseLearningObjective = findCourseLearningObjectiveById(id);

        courseLearningObjectiveRepository.delete(courseLearningObjective);
    }
}
