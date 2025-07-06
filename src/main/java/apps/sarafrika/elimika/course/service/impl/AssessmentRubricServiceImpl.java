package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.factory.AssessmentRubricFactory;
import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.service.AssessmentRubricService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentRubricServiceImpl implements AssessmentRubricService {

    private final AssessmentRubricRepository assessmentRubricRepository;
    private final GenericSpecificationBuilder<AssessmentRubric> specificationBuilder;

    private static final String ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE = "Assessment rubric with ID %s not found";

    @Override
    public AssessmentRubricDTO createAssessmentRubric(AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubric assessmentRubric = AssessmentRubricFactory.toEntity(assessmentRubricDTO);

        // Set defaults based on AssessmentRubricDTO business logic
        if (assessmentRubric.getStatus() == null) {
            assessmentRubric.setStatus(ContentStatus.DRAFT);
        }
        if (assessmentRubric.getActive() == null) {
            assessmentRubric.setActive(false);
        }
        if (assessmentRubric.getIsPublic() == null) {
            assessmentRubric.setIsPublic(false);
        }

        AssessmentRubric savedAssessmentRubric = assessmentRubricRepository.save(assessmentRubric);
        return AssessmentRubricFactory.toDTO(savedAssessmentRubric);
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentRubricDTO getAssessmentRubricByUuid(UUID uuid) {
        return assessmentRubricRepository.findByUuid(uuid)
                .map(AssessmentRubricFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricDTO> getAllAssessmentRubrics(Pageable pageable) {
        return assessmentRubricRepository.findAll(pageable).map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public AssessmentRubricDTO updateAssessmentRubric(UUID uuid, AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubric existingAssessmentRubric = assessmentRubricRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid)));

        updateAssessmentRubricFields(existingAssessmentRubric, assessmentRubricDTO);

        AssessmentRubric updatedAssessmentRubric = assessmentRubricRepository.save(existingAssessmentRubric);
        return AssessmentRubricFactory.toDTO(updatedAssessmentRubric);
    }

    @Override
    public void deleteAssessmentRubric(UUID uuid) {
        if (!assessmentRubricRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid));
        }
        assessmentRubricRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<AssessmentRubric> spec = specificationBuilder.buildSpecification(
                AssessmentRubric.class, searchParams);
        return assessmentRubricRepository.findAll(spec, pageable).map(AssessmentRubricFactory::toDTO);
    }

    private void updateAssessmentRubricFields(AssessmentRubric existingAssessmentRubric, AssessmentRubricDTO dto) {
        if (dto.title() != null) {
            existingAssessmentRubric.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingAssessmentRubric.setDescription(dto.description());
        }
        if (dto.courseUuid() != null) {
            existingAssessmentRubric.setCourseUuid(dto.courseUuid());
        }
        if (dto.rubricType() != null) {
            existingAssessmentRubric.setRubricType(dto.rubricType());
        }
        if (dto.instructorUuid() != null) {
            existingAssessmentRubric.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.isPublic() != null) {
            existingAssessmentRubric.setIsPublic(dto.isPublic());
        }
        if (dto.status() != null) {
            existingAssessmentRubric.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingAssessmentRubric.setActive(dto.active());
        }
    }
}