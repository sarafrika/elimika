package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.factory.RubricScoringFactory;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.repository.RubricScoringRepository;
import apps.sarafrika.elimika.course.service.RubricScoringService;
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
public class RubricScoringServiceImpl implements RubricScoringService {

    private final RubricScoringRepository rubricScoringRepository;
    private final GenericSpecificationBuilder<RubricScoring> specificationBuilder;

    private static final String RUBRIC_SCORING_NOT_FOUND_TEMPLATE = "Rubric scoring with ID %s not found";

    @Override
    public RubricScoringDTO createRubricScoring(UUID criteriaUuid, RubricScoringDTO rubricScoringDTO) {
        RubricScoring rubricScoring = RubricScoringFactory.toEntity(rubricScoringDTO);
        rubricScoring.setCriteriaUuid(criteriaUuid);

        RubricScoring savedRubricScoring = rubricScoringRepository.save(rubricScoring);
        return RubricScoringFactory.toDTO(savedRubricScoring);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricScoringDTO getRubricScoringByUuid(UUID uuid) {
        return rubricScoringRepository.findByUuid(uuid)
                .map(RubricScoringFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(RUBRIC_SCORING_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringDTO> getAllRubricScorings(Pageable pageable) {
        return rubricScoringRepository.findAll(pageable).map(RubricScoringFactory::toDTO);
    }

    @Override
    public RubricScoringDTO updateRubricScoring(UUID criteriaUuid, UUID scoringUuid, RubricScoringDTO rubricScoringDTO) {
        RubricScoring existingRubricScoring = rubricScoringRepository.findByUuidAndCriteriaUuid(scoringUuid, criteriaUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Rubric scoring with ID %s not found in criteria %s", scoringUuid, criteriaUuid)));

        updateRubricScoringFields(existingRubricScoring, rubricScoringDTO);

        RubricScoring updatedRubricScoring = rubricScoringRepository.save(existingRubricScoring);
        return RubricScoringFactory.toDTO(updatedRubricScoring);
    }

    @Override
    public void deleteRubricScoring(UUID criteriaUuid, UUID scoringUuid) {
        if (!rubricScoringRepository.existsByUuidAndCriteriaUuid(scoringUuid, criteriaUuid)) {
            throw new ResourceNotFoundException(
                    String.format("Rubric scoring with ID %s not found in criteria %s", scoringUuid, criteriaUuid));
        }
        rubricScoringRepository.deleteByUuid(scoringUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<RubricScoring> spec = specificationBuilder.buildSpecification(
                RubricScoring.class, searchParams);
        return rubricScoringRepository.findAll(spec, pageable).map(RubricScoringFactory::toDTO);
    }

    private void updateRubricScoringFields(RubricScoring existingRubricScoring, RubricScoringDTO dto) {
        if (dto.criteriaUuid() != null) {
            existingRubricScoring.setCriteriaUuid(dto.criteriaUuid());
        }
        if (dto.gradingLevelUuid() != null) {
            existingRubricScoring.setGradingLevelUuid(dto.gradingLevelUuid());
        }
        if (dto.description() != null) {
            existingRubricScoring.setDescription(dto.description());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringDTO> getAllByCriteriaUuid(UUID criteriaUuid, Pageable pageable) {
        return rubricScoringRepository.findAllByCriteriaUuid(criteriaUuid, pageable).map(RubricScoringFactory::toDTO);
    }
}