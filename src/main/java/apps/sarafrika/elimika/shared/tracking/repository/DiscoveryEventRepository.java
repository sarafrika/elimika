package apps.sarafrika.elimika.shared.tracking.repository;

import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryEventType;
import apps.sarafrika.elimika.shared.tracking.entity.DiscoveryEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscoveryEventRepository extends JpaRepository<DiscoveryEvent, Long> {

    /** The impression a client event refers to, if that user was really shown that item. */
    Optional<DiscoveryEvent> findFirstByUserUuidAndRecommendationIdAndItemUuidAndEventTypeOrderByIdAsc(
            UUID userUuid, UUID recommendationId, UUID itemUuid, DiscoveryEventType eventType);

    /** Deletes up to {@code limit} rows older than the cutoff; returns how many went. */
    @Modifying
    @Query(value = """
            DELETE FROM discovery_events
            WHERE id IN (SELECT id FROM discovery_events WHERE created_at < :cutoff ORDER BY id LIMIT :limit)
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
