package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.GradingLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingLevelRepository extends JpaRepository<GradingLevel, Long>, JpaSpecificationExecutor<GradingLevel> {
    Optional<GradingLevel> findByUuid(UUID uuid);
    Optional<GradingLevel> findByName(String name);
    void deleteByUuid(UUID uuid);
}