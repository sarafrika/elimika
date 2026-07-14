package apps.sarafrika.elimika.resourcing.repository;

import apps.sarafrika.elimika.resourcing.model.ResourceAvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceAvailabilityRuleRepository extends JpaRepository<ResourceAvailabilityRule, Long> {

    Optional<ResourceAvailabilityRule> findByUuid(UUID uuid);

    List<ResourceAvailabilityRule> findByResourceUuidOrderByCreatedDateAsc(UUID resourceUuid);

    List<ResourceAvailabilityRule> findByResourceUuidIn(Collection<UUID> resourceUuids);
}
