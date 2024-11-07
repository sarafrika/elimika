package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CreateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.LearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LearningMaterialResponseDTO;
import apps.sarafrika.elimika.course.service.LearningMaterialService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = LearningMaterialController.ROOT_PATH)
class LearningMaterialController {

    protected static final String ROOT_PATH = "api/v1/learning-materials";
    private static final String ID_PATH = "{learningMaterialId}";

    private final LearningMaterialService learningMaterialService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<LearningMaterialResponseDTO> getAllLearningMaterials(LearningMaterialRequestDTO learningMaterialRequestDTO, Pageable pageable) {

        return learningMaterialService.findAllLearningMaterials(learningMaterialRequestDTO, pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<LearningMaterialResponseDTO> getLearningMaterial(final @PathVariable Long learningMaterialId) {

        return learningMaterialService.findLearningMaterial(learningMaterialId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createLearningMaterial(@RequestBody final CreateLearningMaterialRequestDTO createLearningMaterialRequestDTO) {

        return learningMaterialService.createLearningMaterial(createLearningMaterialRequestDTO);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateLearningMaterial(@RequestBody final UpdateLearningMaterialRequestDTO updateLearningMaterialRequestDTO, final @PathVariable Long learningMaterialId) {

        return learningMaterialService.updateLearningMaterial(learningMaterialId, updateLearningMaterialRequestDTO);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLearningMaterial(final @PathVariable Long learningMaterialId) {

        learningMaterialService.deleteLearningMaterial(learningMaterialId);
    }
}
