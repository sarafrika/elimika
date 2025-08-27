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

import java.util.HashMap;
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
        if (assessmentRubric.getIsActive() == null) {
            assessmentRubric.setIsActive(false);
        }
        if (assessmentRubric.getIsPublic() == null) {
            assessmentRubric.setIsPublic(false);
        }
        if (assessmentRubric.getTotalWeight() == null) {
            assessmentRubric.setTotalWeight(new java.math.BigDecimal("100.00"));
        }
        if (assessmentRubric.getWeightUnit() == null) {
            assessmentRubric.setWeightUnit("percentage");
        }
        if (assessmentRubric.getUsesCustomLevels() == null) {
            assessmentRubric.setUsesCustomLevels(true);
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
            existingAssessmentRubric.setIsActive(dto.active());
        }
        if (dto.totalWeight() != null) {
            existingAssessmentRubric.setTotalWeight(dto.totalWeight());
        }
        if (dto.weightUnit() != null) {
            existingAssessmentRubric.setWeightUnit(dto.weightUnit());
        }
        if (dto.usesCustomLevels() != null) {
            existingAssessmentRubric.setUsesCustomLevels(dto.usesCustomLevels());
        }
        if (dto.maxScore() != null) {
            existingAssessmentRubric.setMaxScore(dto.maxScore());
        }
        if (dto.minPassingScore() != null) {
            existingAssessmentRubric.setMinPassingScore(dto.minPassingScore());
        }
    }

    @Override
    public Page<AssessmentRubricDTO> getPublicRubrics(Pageable pageable) {
        return assessmentRubricRepository.findByIsPublicTrueAndIsActiveTrueOrderByCreatedDateDesc(pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> searchPublicRubrics(String searchTerm, String rubricType, Pageable pageable) {
        if (searchTerm != null && rubricType != null) {
            // Search with both term and type
            return assessmentRubricRepository.findPublicRubricsBySearchTerm(searchTerm, pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else if (searchTerm != null) {
            // Search by term only
            return assessmentRubricRepository.findPublicRubricsBySearchTerm(searchTerm, pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else if (rubricType != null) {
            // Filter by type only
            return assessmentRubricRepository.findByIsPublicTrueAndIsActiveTrueAndRubricTypeContainingIgnoreCaseOrderByCreatedDateDesc(rubricType, pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else {
            // No filters - return all public rubrics
            return getPublicRubrics(pageable);
        }
    }

    @Override
    public Page<AssessmentRubricDTO> getInstructorRubrics(UUID instructorUuid, boolean includePrivate, Pageable pageable) {
        return assessmentRubricRepository.findInstructorShareableRubrics(instructorUuid, includePrivate, pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> getGeneralRubrics(Pageable pageable) {
        // All rubrics are now general-use and can be associated with multiple courses
        return getPublicRubrics(pageable);
    }

    @Override
    public Page<AssessmentRubricDTO> getPopularRubrics(Pageable pageable) {
        return assessmentRubricRepository.findPopularPublicRubrics(pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> getRubricsByStatus(ContentStatus status, Pageable pageable) {
        return assessmentRubricRepository.findByStatusAndIsActiveTrueOrderByCreatedDateDesc(status, pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Map<String, Long> getRubricStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalPublicRubrics", assessmentRubricRepository.countByIsPublicTrueAndIsActiveTrue());
        stats.put("totalRubrics", assessmentRubricRepository.count());
        // Add more statistics as needed
        return stats;
    }

    @Override
    public Map<String, Long> getInstructorRubricStatistics(UUID instructorUuid) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalRubrics", assessmentRubricRepository.countByInstructorUuidAndIsActiveTrue(instructorUuid));
        // Add more instructor-specific statistics as needed
        return stats;
    }
}