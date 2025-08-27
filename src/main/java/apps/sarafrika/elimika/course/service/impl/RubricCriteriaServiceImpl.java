package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.dto.CriteriaCreationResponse;
import apps.sarafrika.elimika.course.dto.RubricMatrixDTO;
import apps.sarafrika.elimika.course.dto.RubricScoringLevelDTO;
import apps.sarafrika.elimika.course.factory.RubricCriteriaFactory;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.service.RubricCriteriaService;
import apps.sarafrika.elimika.course.service.RubricScoringLevelService;
import apps.sarafrika.elimika.course.service.RubricMatrixService;
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
public class RubricCriteriaServiceImpl implements RubricCriteriaService {

    private final RubricCriteriaRepository rubricCriteriaRepository;
    private final GenericSpecificationBuilder<RubricCriteria> specificationBuilder;
    private final RubricScoringLevelService rubricScoringLevelService;
    private final RubricMatrixService rubricMatrixService;

    private static final String RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE = "Rubric criteria with ID %s not found";

    @Override
    public RubricCriteriaDTO createRubricCriteria(UUID rubricUuid, RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteria rubricCriteria = RubricCriteriaFactory.toEntity(rubricCriteriaDTO);
        rubricCriteria.setRubricUuid(rubricUuid);

        RubricCriteria savedRubricCriteria = rubricCriteriaRepository.save(rubricCriteria);
        return RubricCriteriaFactory.toDTO(savedRubricCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricCriteriaDTO getRubricCriteriaByUuid(UUID uuid) {
        return rubricCriteriaRepository.findByUuid(uuid)
                .map(RubricCriteriaFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RUBRIC_CRITERIA_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> getAllRubricCriterias(Pageable pageable) {
        return rubricCriteriaRepository.findAll(pageable).map(RubricCriteriaFactory::toDTO);
    }

    @Override
    public RubricCriteriaDTO updateRubricCriteria(UUID rubricUuid, UUID criteriaUuid, RubricCriteriaDTO rubricCriteriaDTO) {
        RubricCriteria existingRubricCriteria = rubricCriteriaRepository.findByUuidAndRubricUuid(criteriaUuid, rubricUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Rubric criteria with ID %s not found in rubric %s", criteriaUuid, rubricUuid)));

        updateRubricCriteriaFields(existingRubricCriteria, rubricCriteriaDTO);

        RubricCriteria updatedRubricCriteria = rubricCriteriaRepository.save(existingRubricCriteria);
        return RubricCriteriaFactory.toDTO(updatedRubricCriteria);
    }

    @Override
    public void deleteRubricCriteria(UUID rubricUuid, UUID criteriaUuid) {
        if (!rubricCriteriaRepository.existsByUuidAndRubricUuid(criteriaUuid, rubricUuid)) {
            throw new ResourceNotFoundException(
                    String.format("Rubric criteria with ID %s not found in rubric %s", criteriaUuid, rubricUuid));
        }
        rubricCriteriaRepository.deleteByUuid(criteriaUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<RubricCriteria> spec = specificationBuilder.buildSpecification(
                RubricCriteria.class, searchParams);
        return rubricCriteriaRepository.findAll(spec, pageable).map(RubricCriteriaFactory::toDTO);
    }

    private void updateRubricCriteriaFields(RubricCriteria existingRubricCriteria, RubricCriteriaDTO dto) {
        if (dto.rubricUuid() != null) {
            existingRubricCriteria.setRubricUuid(dto.rubricUuid());
        }
        if (dto.componentName() != null) {
            existingRubricCriteria.setComponentName(dto.componentName());
        }
        if (dto.description() != null) {
            existingRubricCriteria.setDescription(dto.description());
        }
        if (dto.displayOrder() != null) {
            existingRubricCriteria.setDisplayOrder(dto.displayOrder());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricCriteriaDTO> getAllByRubricUuid(UUID rubricUuid, Pageable pageable) {
        return rubricCriteriaRepository.findAllByRubricUuid(rubricUuid, pageable).map(RubricCriteriaFactory::toDTO);
    }

    @Override
    public CriteriaCreationResponse createRubricCriteriaWithMatrixCheck(UUID rubricUuid, RubricCriteriaDTO rubricCriteriaDTO) {
        // Create the criteria first
        RubricCriteriaDTO createdCriteria = createRubricCriteria(rubricUuid, rubricCriteriaDTO);
        
        // Check if matrix can be auto-generated (scoring levels exist)
        try {
            java.util.List<RubricScoringLevelDTO> scoringLevels = rubricScoringLevelService.getScoringLevelsByRubricUuid(rubricUuid);
            
            if (!scoringLevels.isEmpty()) {
                // Auto-generate matrix cells
                rubricMatrixService.autoGenerateMatrixCells(rubricUuid);
                
                // Get the complete matrix
                RubricMatrixDTO matrix = rubricMatrixService.getRubricMatrix(rubricUuid);
                
                return new CriteriaCreationResponse(
                    createdCriteria,
                    matrix,
                    true,
                    "Criterion added and matrix auto-generated successfully"
                );
            } else {
                // No scoring levels exist yet
                return new CriteriaCreationResponse(
                    createdCriteria,
                    null,
                    false,
                    "Criterion added successfully. Add scoring levels to generate matrix."
                );
            }
        } catch (Exception e) {
            // If any error occurs during matrix generation, still return the criteria
            return new CriteriaCreationResponse(
                createdCriteria,
                null,
                false,
                "Criterion added successfully, but matrix generation failed: " + e.getMessage()
            );
        }
    }
}