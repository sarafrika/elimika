package apps.sarafrika.elimika.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface RubricCriteriaService {
    RubricCriteriaDTO createRubricCriteria(RubricCriteriaDTO rubricCriteriaDTO);
    RubricCriteriaDTO getRubricCriteriaByUuid(UUID uuid);
    Page<RubricCriteriaDTO> getAllRubricCriterias(Pageable pageable);
    RubricCriteriaDTO updateRubricCriteria(UUID uuid, RubricCriteriaDTO rubricCriteriaDTO);
    void deleteRubricCriteria(UUID uuid);
    Page<RubricCriteriaDTO> search(Map<String, String> searchParams, Pageable pageable);
}