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
}