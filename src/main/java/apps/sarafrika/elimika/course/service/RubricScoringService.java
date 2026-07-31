package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface RubricScoringService {
    RubricScoringDTO createRubricScoring(UUID criteriaUuid, RubricScoringDTO rubricScoringDTO);

    RubricScoringDTO getRubricScoringByUuid(UUID uuid);

    Page<RubricScoringDTO> getAllRubricScorings(Pageable pageable);

    RubricScoringDTO updateRubricScoring(UUID criteriaUuid, UUID scoringUuid, RubricScoringDTO rubricScoringDTO);

    void deleteRubricScoring(UUID criteriaUuid, UUID scoringUuid);

    Page<RubricScoringDTO> search(Map<String, String> searchParams, Pageable pageable);

    Page<RubricScoringDTO> getAllByCriteriaUuid(UUID criteriaUuid, Pageable pageable);

    /**
     * Scoring levels for a criterion, asserting the criterion actually belongs to the given rubric.
     * <p>
     * The endpoint serving this authorizes on the rubric, so reading by criterion alone would let a
     * caller entitled to one rubric read the scoring of a criterion belonging to another simply by
     * pairing their own rubric UUID with a foreign criterion UUID.
     */
    Page<RubricScoringDTO> getAllByRubricAndCriteria(UUID rubricUuid, UUID criteriaUuid, Pageable pageable);
}