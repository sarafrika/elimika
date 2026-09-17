package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingApplicationEvent;
import apps.sarafrika.elimika.course.repository.projection.TrainingApplicationFirstOpenedView;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingApplicationEventRepository extends JpaRepository<TrainingApplicationEvent, Long> {

    List<TrainingApplicationEvent> findByApplicationTypeAndApplicationUuidOrderByCreatedDateDescIdDesc(
            TrainingApplicationType applicationType, UUID applicationUuid);

    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.TrainingApplicationFirstOpenedView(
                       event.applicationUuid, MIN(event.createdDate))
            FROM TrainingApplicationEvent event
            WHERE event.applicationType = :applicationType
              AND event.eventType = apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType.OPENED_BY_CREATOR
              AND event.applicationUuid IN :applicationUuids
            GROUP BY event.applicationUuid
            """)
    List<TrainingApplicationFirstOpenedView> findFirstOpened(@Param("applicationType") TrainingApplicationType applicationType,
                                                             @Param("applicationUuids") Collection<UUID> applicationUuids);

    /** Inserts the first-open event unless one exists; the partial unique index makes this race-free. */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO training_application_events
                (uuid, application_type, application_uuid, event_type, actor_uuid, actor_name, created_date, created_by)
            VALUES (gen_random_uuid(), :applicationType, :applicationUuid, 'OPENED_BY_CREATOR', :actorUuid, :actorName,
                    :createdDate, :createdBy)
            ON CONFLICT DO NOTHING
            """)
    int insertFirstOpenIfAbsent(@Param("applicationType") String applicationType,
                                @Param("applicationUuid") UUID applicationUuid,
                                @Param("actorUuid") UUID actorUuid,
                                @Param("actorName") String actorName,
                                @Param("createdDate") LocalDateTime createdDate,
                                @Param("createdBy") String createdBy);
}
