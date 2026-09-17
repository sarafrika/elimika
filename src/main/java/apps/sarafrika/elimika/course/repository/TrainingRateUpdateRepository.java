package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingRateUpdate;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface TrainingRateUpdateRepository<U extends TrainingRateUpdate> extends JpaRepository<U, Long> {

    Optional<U> findByUuid(UUID uuid);

    boolean existsByApplicationUuidAndStatus(UUID applicationUuid, TrainingRateUpdateStatus status);

    Optional<U> findFirstByApplicationUuidAndStatus(UUID applicationUuid, TrainingRateUpdateStatus status);

    List<U> findByApplicationUuidOrderByCreatedDateDesc(UUID applicationUuid);

    List<U> findByApplicationUuidInAndStatus(Collection<UUID> applicationUuids, TrainingRateUpdateStatus status);
}
