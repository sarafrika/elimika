package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.dto.CriteriaCreationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface RubricCriteriaService {
    RubricCriteriaDTO createRubricCriteria(UUID rubricUuid, RubricCriteriaDTO rubricCriteriaDTO);

    RubricCriteriaDTO getRubricCriteriaByUuid(UUID uuid);

    Page<RubricCriteriaDTO> getAllRubricCriterias(Pageable pageable);

    RubricCriteriaDTO updateRubricCriteria(UUID rubricUuid, UUID criteriaUuid, RubricCriteriaDTO rubricCriteriaDTO);

    void deleteRubricCriteria(UUID rubricUuid, UUID criteriaUuid);

    Page<RubricCriteriaDTO> search(Map<String, String> searchParams, Pageable pageable);

    Page<RubricCriteriaDTO> getAllByRubricUuid(UUID rubricUuid, Pageable pageable);

    /**
     * Creates rubric criteria and checks if matrix can be auto-generated.
     * If scoring levels exist, automatically generates matrix structure.
     *
     * @param rubricUuid the UUID of the rubric
     * @param rubricCriteriaDTO the criteria to create
     * @return response containing created criteria and potentially matrix
     */
    CriteriaCreationResponse createRubricCriteriaWithMatrixCheck(UUID rubricUuid, RubricCriteriaDTO rubricCriteriaDTO);
}