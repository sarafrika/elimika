package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.RubricCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface RubricCriteriaRepository extends JpaRepository<RubricCriteria, Long>,
        JpaSpecificationExecutor<RubricCriteria> {
    Optional<RubricCriteria> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Page<RubricCriteria> findAllByRubricUuid(UUID rubricUuid, Pageable pageable);

    List<RubricCriteria> findByRubricUuidOrderByDisplayOrderAsc(UUID rubricUuid);

    Optional<RubricCriteria> findByUuidAndRubricUuid(UUID uuid, UUID rubricUuid);

    boolean existsByUuidAndRubricUuid(UUID uuid, UUID rubricUuid);
}
