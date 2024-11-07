package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.LearningMaterialNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.LearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLearningMaterialRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LearningMaterialResponseDTO;
import apps.sarafrika.elimika.course.persistence.LearningMaterial;
import apps.sarafrika.elimika.course.persistence.LearningMaterialFactory;
import apps.sarafrika.elimika.course.persistence.LearningMaterialRepository;
import apps.sarafrika.elimika.course.persistence.LearningMaterialSpecification;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.LearningMaterialService;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
class LearningMaterialServiceImpl implements LearningMaterialService {

    private static final String ERROR_LEARNING_MATERIAL_NOT_FOUND = "Learning material not found.";
    private static final String LEARNING_MATERIAL_FOUND_SUCCESS = "Learning material retrieved successfully.";
    private static final String LEARNING_MATERIAL_CREATED_SUCCESS = "Learning material persisted successfully.";
    private static final String LEARNING_MATERIAL_UPDATED_SUCCESS = "Learning material updated successfully.";

    private final CourseService courseService;
    private final LessonService lessonService;
    private final LearningMaterialRepository learningMaterialRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<LearningMaterialResponseDTO> findAllLearningMaterials(LearningMaterialRequestDTO learningMaterialRequestDTO, Pageable pageable) {

        final Specification<LearningMaterial> specification = new LearningMaterialSpecification(learningMaterialRequestDTO);

        final Page<LearningMaterial> learningMaterials = learningMaterialRepository.findAll(specification, pageable);

        List<LearningMaterialResponseDTO> learningMaterialResponseDTOs = learningMaterials.stream()
                .map(LearningMaterialResponseDTO::from)
                .toList();

        return new ResponsePageableDTO<>(learningMaterialResponseDTOs, learningMaterials.getNumber(), learningMaterials.getSize(),
                learningMaterials.getTotalPages(), learningMaterials.getTotalElements(), HttpStatus.OK.value(), LEARNING_MATERIAL_FOUND_SUCCESS);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<LearningMaterialResponseDTO> findLearningMaterial(Long learningMaterialId) {

        final LearningMaterial learningMaterial = findLearningMaterialById(learningMaterialId);

        LearningMaterialResponseDTO learningMaterialResponseDTO = LearningMaterialResponseDTO.from(learningMaterial);

        return new ResponseDTO<>(learningMaterialResponseDTO, HttpStatus.OK.value(), LEARNING_MATERIAL_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private LearningMaterial findLearningMaterialById(Long learningMaterialId) {

        return learningMaterialRepository.findById(learningMaterialId).orElseThrow(() -> new LearningMaterialNotFoundException(ERROR_LEARNING_MATERIAL_NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createLearningMaterial(CreateLearningMaterialRequestDTO createLearningMaterialRequestDTO) {

        courseService.findCourse(createLearningMaterialRequestDTO.courseId());

        if (createLearningMaterialRequestDTO.lessonId() != null) {

            lessonService.findLesson(createLearningMaterialRequestDTO.courseId(), createLearningMaterialRequestDTO.lessonId());
        }

        LearningMaterial learningMaterial = LearningMaterialFactory.create(createLearningMaterialRequestDTO);

        learningMaterialRepository.save(learningMaterial);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), LEARNING_MATERIAL_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateLearningMaterial(Long learningMaterialId, UpdateLearningMaterialRequestDTO updateLearningMaterialRequestDTO) {

        LearningMaterial learningMaterial = findLearningMaterialById(learningMaterialId);

        LearningMaterialFactory.update(learningMaterial, updateLearningMaterialRequestDTO);

        learningMaterialRepository.save(learningMaterial);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), LEARNING_MATERIAL_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteLearningMaterial(Long learningMaterialId) {

        final LearningMaterial learningMaterial = findLearningMaterialById(learningMaterialId);

        learningMaterialRepository.delete(learningMaterial);
    }
}
