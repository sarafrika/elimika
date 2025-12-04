package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
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
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    Optional<Enrollment> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<Enrollment> findByStudentUuid(UUID studentUuid);

    List<Enrollment> findByScheduledInstanceUuid(UUID scheduledInstanceUuid);

    List<Enrollment> findByStatus(EnrollmentStatus status);

    List<Enrollment> findByStudentUuidAndStatus(UUID studentUuid, EnrollmentStatus status);

    List<Enrollment> findByScheduledInstanceUuidAndStatus(UUID scheduledInstanceUuid, EnrollmentStatus status);

    Optional<Enrollment> findByScheduledInstanceUuidAndStudentUuid(UUID scheduledInstanceUuid, UUID studentUuid);

    boolean existsByScheduledInstanceUuidAndStudentUuid(UUID scheduledInstanceUuid, UUID studentUuid);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.startTime >= :startTime AND si.endTime <= :endTime " +
           "AND si.status <> 'CANCELLED' " +
           "AND e.status <> 'CANCELLED' " +
           "ORDER BY si.startTime")
    List<Enrollment> findByStudentAndTimeRange(@Param("studentUuid") UUID studentUuid,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.startTime >= :startTime AND si.endTime <= :endTime " +
           "AND si.status <> 'CANCELLED' " +
           "AND e.status = :status " +
           "ORDER BY si.startTime")
    List<Enrollment> findByStudentTimeRangeAndStatus(@Param("studentUuid") UUID studentUuid,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE si.classDefinitionUuid = :classDefinitionUuid " +
           "AND si.status <> 'CANCELLED' " +
           "AND e.status <> 'CANCELLED'")
    List<Enrollment> findByClassDefinitionUuid(@Param("classDefinitionUuid") UUID classDefinitionUuid);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.instructorUuid = :instructorUuid " +
           "ORDER BY si.startTime")
    List<Enrollment> findByStudentAndInstructor(@Param("studentUuid") UUID studentUuid,
                                              @Param("instructorUuid") UUID instructorUuid);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.scheduledInstanceUuid = :scheduledInstanceUuid " +
           "AND e.status NOT IN ('CANCELLED', 'WAITLISTED')")
    Long countActiveEnrollmentsByScheduledInstance(@Param("scheduledInstanceUuid") UUID scheduledInstanceUuid);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.scheduledInstanceUuid = :scheduledInstanceUuid " +
           "AND e.status = :status")
    Long countEnrollmentsByScheduledInstanceAndStatus(@Param("scheduledInstanceUuid") UUID scheduledInstanceUuid,
                                                    @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.attendanceMarkedAt IS NULL " +
           "AND e.status IN ('ENROLLED') " +
           "AND e.scheduledInstanceUuid IN " +
           "(SELECT si.uuid FROM ScheduledInstance si WHERE si.status = 'ONGOING')")
    List<Enrollment> findEnrollmentsNeedingAttendance();

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.startTime <= :endTime AND si.endTime >= :startTime " +
           "AND e.status NOT IN ('CANCELLED')")
    List<Enrollment> findOverlappingEnrollmentsForStudent(@Param("studentUuid") UUID studentUuid,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    long countByStatusAndAttendanceMarkedAtBetween(EnrollmentStatus status, LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedDateBetween(EnrollmentStatus status, LocalDateTime start, LocalDateTime end);
}
