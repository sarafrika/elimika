package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.RubricScoring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RubricScoringRepository extends JpaRepository<RubricScoring, Long>,
        JpaSpecificationExecutor<RubricScoring> {
    Optional<RubricScoring> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Page<RubricScoring> findAllByCriteriaUuid(UUID criteriaUuid, Pageable pageable);

    Optional<RubricScoring> findByUuidAndCriteriaUuid(UUID uuid, UUID criteriaUuid);

    boolean existsByUuidAndCriteriaUuid(UUID uuid, UUID criteriaUuid);
}
