package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.factory.RubricScoringFactory;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
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
    private final RubricCriteriaRepository rubricCriteriaRepository;
    private final RubricScoringLevelRepository rubricScoringLevelRepository;
    private final GenericSpecificationBuilder<RubricScoring> specificationBuilder;

    private static final String RUBRIC_SCORING_NOT_FOUND_TEMPLATE = "Rubric scoring with ID %s not found";

    @Override
    public RubricScoringDTO createRubricScoring(UUID criteriaUuid, RubricScoringDTO rubricScoringDTO) {
        RubricScoring rubricScoring = RubricScoringFactory.toEntity(rubricScoringDTO);
        rubricScoring.setCriteriaUuid(criteriaUuid);
        requireLevelInCriterionRubric(criteriaUuid, rubricScoring.getRubricScoringLevelUuid());

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
        specificationBuilder.validateSortProperties(RubricScoring.class, pageable);
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
        specificationBuilder.validateSortProperties(RubricScoring.class, pageable);
        Specification<RubricScoring> spec = specificationBuilder.buildSpecification(
                RubricScoring.class, searchParams);
        return rubricScoringRepository.findAll(spec, pageable).map(RubricScoringFactory::toDTO);
    }

    /**
     * A scoring cell sits at the intersection of a criterion and a scoring level of the same rubric.
     * A level borrowed from another rubric would make the matrix meaningless, so it is rejected.
     */
    private void requireLevelInCriterionRubric(UUID criteriaUuid, UUID scoringLevelUuid) {
        UUID criterionRubric = rubricCriteriaRepository.findByUuid(criteriaUuid)
                .map(RubricCriteria::getRubricUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Rubric criteria with ID %s not found", criteriaUuid)));
        UUID levelRubric = scoringLevelUuid == null ? null : rubricScoringLevelRepository.findByUuid(scoringLevelUuid)
                .map(RubricScoringLevel::getRubricUuid)
                .orElse(null);
        if (!criterionRubric.equals(levelRubric)) {
            throw new IllegalArgumentException(String.format(
                    "Scoring level %s does not belong to the rubric of criterion %s", scoringLevelUuid, criteriaUuid));
        }
    }

    private void updateRubricScoringFields(RubricScoring existingRubricScoring, RubricScoringDTO dto) {
        // criteria_uuid is not settable: a scoring cell stays under the criterion it was created in,
        // so an update cannot move it into a rubric the caller was never authorised against.
        if (dto.description() != null) {
            existingRubricScoring.setDescription(dto.description());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringDTO> getAllByCriteriaUuid(UUID criteriaUuid, Pageable pageable) {
        return rubricScoringRepository.findAllByCriteriaUuid(criteriaUuid, pageable).map(RubricScoringFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RubricScoringDTO> getAllByRubricAndCriteria(UUID rubricUuid, UUID criteriaUuid, Pageable pageable) {
        if (!rubricCriteriaRepository.existsByUuidAndRubricUuid(criteriaUuid, rubricUuid)) {
            throw new ResourceNotFoundException(
                    String.format("Rubric criteria with ID %s not found for rubric %s", criteriaUuid, rubricUuid));
        }
        return getAllByCriteriaUuid(criteriaUuid, pageable);
    }
}