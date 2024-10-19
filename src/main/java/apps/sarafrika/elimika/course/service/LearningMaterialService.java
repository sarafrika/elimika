package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.LearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LearningMaterialResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface LearningMaterialService {

    ResponsePageableDTO<LearningMaterialResponseDTO> findAllLearningMaterials(LearningMaterialRequestDTO learningMaterialRequestDTO, Pageable pageable);

    ResponseDTO<LearningMaterialResponseDTO> findLearningMaterial(Long learningMaterialId);

    ResponseDTO<Void> createLearningMaterial(CreateLearningMaterialRequestDTO createLearningMaterialRequestDTO);

    ResponseDTO<Void> updateLearningMaterial(Long learningMaterialId, UpdateLearningMaterialRequestDTO updateLearningMaterialRequestDTO);

    void deleteLearningMaterial(Long learningMaterialId);
}
