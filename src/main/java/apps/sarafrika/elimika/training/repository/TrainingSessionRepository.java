package apps.sarafrika.elimika.training.repository;

import apps.sarafrika.elimika.training.model.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long>, JpaSpecificationExecutor<TrainingSession> {
    Optional<TrainingSession> findByUuid(UUID uuid);

    Optional<TrainingSession> deleteByUuid(UUID uuid);
}
