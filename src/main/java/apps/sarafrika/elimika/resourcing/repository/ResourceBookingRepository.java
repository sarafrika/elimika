package apps.sarafrika.elimika.resourcing.repository;

import apps.sarafrika.elimika.resourcing.model.ResourceBooking;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, Long>,
        JpaSpecificationExecutor<ResourceBooking> {

    /**
     * Active (HOLD/CONFIRMED) bookings overlapping the window. Strict inequalities so
     * back-to-back bookings sharing a boundary do not collide.
     */
    @Query("""
            SELECT b FROM ResourceBooking b
            WHERE b.resourceUuid = :resourceUuid
              AND b.status IN :statuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<ResourceBooking> findActiveOverlaps(@Param("resourceUuid") UUID resourceUuid,
                                             @Param("statuses") Collection<ResourceBookingStatus> statuses,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    List<ResourceBooking> findByJobUuidAndStatus(UUID jobUuid, ResourceBookingStatus status);

    List<ResourceBooking> findByScheduledInstanceUuidAndStatusIn(UUID scheduledInstanceUuid,
                                                                 Collection<ResourceBookingStatus> statuses);

    List<ResourceBooking> findByResourceUuidAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID resourceUuid,
            Collection<ResourceBookingStatus> statuses,
            LocalDateTime endTime,
            LocalDateTime startTime);

    boolean existsByResourceUuidAndStatusInAndEndTimeAfter(UUID resourceUuid,
                                                           Collection<ResourceBookingStatus> statuses,
                                                           LocalDateTime after);
}
