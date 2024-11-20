package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseLearningObjectiveRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseLearningObjectiveResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

import java.util.List;

public interface CourseLearningObjectiveService {

    ResponseDTO<CourseLearningObjectiveResponseDTO> findCourseLearningObjective(Long id);

    ResponseDTO<List<CourseLearningObjectiveResponseDTO>> findAllCourseLearningObjectives(Long courseId);

    ResponseDTO<CourseLearningObjectiveResponseDTO> createCourseLearningObjective(CreateCourseLearningObjectiveRequestDTO createCourseLearningObjectiveRequestDTO, Long courseId);

    ResponseDTO<List<CourseLearningObjectiveResponseDTO>> createCourseLearningObjectives(List<CreateCourseLearningObjectiveRequestDTO> createCourseLearningObjectiveRequestDTOS, Long courseId);

    ResponseDTO<CourseLearningObjectiveResponseDTO> updateCourseLearningObjective(Long id, UpdateCourseLearningObjectiveRequestDTO updateCourseLearningObjectiveRequestDTO);

    ResponseDTO<List<CourseLearningObjectiveResponseDTO>> updateCourseLearningObjectives(List<UpdateCourseLearningObjectiveRequestDTO> updateCourseLearningObjectiveRequestDTOS);

    void deleteCourseLearningObjective(Long id);
}
