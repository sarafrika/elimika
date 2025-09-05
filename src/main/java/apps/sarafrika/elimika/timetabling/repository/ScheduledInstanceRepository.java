package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.util.enums.SchedulingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledInstanceRepository extends JpaRepository<ScheduledInstance, Long>, JpaSpecificationExecutor<ScheduledInstance> {

    Optional<ScheduledInstance> findByUuid(UUID uuid);

    List<ScheduledInstance> findByInstructorUuid(UUID instructorUuid);

    List<ScheduledInstance> findByClassDefinitionUuid(UUID classDefinitionUuid);

    List<ScheduledInstance> findByStatus(SchedulingStatus status);

    List<ScheduledInstance> findByInstructorUuidAndStatus(UUID instructorUuid, SchedulingStatus status);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.instructorUuid = :instructorUuid " +
           "AND si.startTime >= :startTime AND si.endTime <= :endTime " +
           "ORDER BY si.startTime")
    List<ScheduledInstance> findByInstructorAndTimeRange(@Param("instructorUuid") UUID instructorUuid,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.classDefinitionUuid = :classDefinitionUuid " +
           "AND si.startTime >= :startTime AND si.endTime <= :endTime " +
           "ORDER BY si.startTime")
    List<ScheduledInstance> findByClassDefinitionAndTimeRange(@Param("classDefinitionUuid") UUID classDefinitionUuid,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.startTime >= :startTime AND si.endTime <= :endTime " +
           "AND si.status != 'CANCELLED' ORDER BY si.startTime")
    List<ScheduledInstance> findActiveInstancesInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.instructorUuid = :instructorUuid " +
           "AND si.startTime <= :endTime AND si.endTime >= :startTime " +
           "AND si.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<ScheduledInstance> findOverlappingInstancesForInstructor(@Param("instructorUuid") UUID instructorUuid,
                                                                @Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.status = 'SCHEDULED' AND si.startTime <= :currentTime")
    List<ScheduledInstance> findScheduledInstancesPastStartTime(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT si FROM ScheduledInstance si WHERE si.status = 'ONGOING' AND si.endTime <= :currentTime")
    List<ScheduledInstance> findOngoingInstancesPastEndTime(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(si) FROM ScheduledInstance si WHERE si.instructorUuid = :instructorUuid " +
           "AND si.status NOT IN ('CANCELLED') " +
           "AND si.startTime >= :startDate AND si.startTime < :endDate")
    Long countInstancesForInstructorInPeriod(@Param("instructorUuid") UUID instructorUuid,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(si) FROM ScheduledInstance si WHERE si.classDefinitionUuid = :classDefinitionUuid " +
           "AND si.status NOT IN ('CANCELLED') " +
           "AND si.startTime >= :startDate AND si.startTime < :endDate")
    Long countInstancesForClassDefinitionInPeriod(@Param("classDefinitionUuid") UUID classDefinitionUuid,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}