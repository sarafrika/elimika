package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    Optional<Enrollment> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<Enrollment> findByStudentUuid(UUID studentUuid);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "ORDER BY si.startTime ASC, COALESCE(e.lastModifiedDate, e.createdDate) ASC, e.uuid ASC")
    List<Enrollment> findByStudentUuidOrderByScheduledInstanceStartTime(@Param("studentUuid") UUID studentUuid);

    @Query(value = "SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "ORDER BY si.startTime ASC, COALESCE(e.lastModifiedDate, e.createdDate) ASC, e.uuid ASC",
           countQuery = "SELECT COUNT(e) FROM Enrollment e WHERE e.studentUuid = :studentUuid")
    Page<Enrollment> findPageByStudentUuidOrderByScheduledInstanceStartTime(
            @Param("studentUuid") UUID studentUuid,
            Pageable pageable);

    @Query(value = "SELECT si.classDefinitionUuid FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.classDefinitionUuid IS NOT NULL " +
           "GROUP BY si.classDefinitionUuid " +
           "ORDER BY MAX(COALESCE(e.lastModifiedDate, e.createdDate)) DESC, MAX(si.startTime) DESC, si.classDefinitionUuid ASC",
           countQuery = "SELECT COUNT(DISTINCT si.classDefinitionUuid) FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.classDefinitionUuid IS NOT NULL")
    Page<UUID> findClassDefinitionUuidsByStudentUuid(@Param("studentUuid") UUID studentUuid, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.classDefinitionUuid IN :classDefinitionUuids " +
           "ORDER BY si.startTime ASC, COALESCE(e.lastModifiedDate, e.createdDate) ASC, e.uuid ASC")
    List<Enrollment> findByStudentUuidAndClassDefinitionUuidIn(
            @Param("studentUuid") UUID studentUuid,
            @Param("classDefinitionUuids") Collection<UUID> classDefinitionUuids);

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

    @Query("SELECT COUNT(DISTINCT e.studentUuid) FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE si.classDefinitionUuid = :classDefinitionUuid " +
           "AND si.status <> 'CANCELLED' " +
           "AND e.status = :status")
    long countDistinctStudentsByClassDefinitionUuidAndStatus(@Param("classDefinitionUuid") UUID classDefinitionUuid,
                                                             @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN ScheduledInstance si ON e.scheduledInstanceUuid = si.uuid " +
           "WHERE e.studentUuid = :studentUuid " +
           "AND si.instructorUuid = :instructorUuid " +
           "ORDER BY si.startTime")
    List<Enrollment> findByStudentAndInstructor(@Param("studentUuid") UUID studentUuid,
                                              @Param("instructorUuid") UUID instructorUuid);

    @Query(value = "SELECT ce.* " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE ce.student_uuid = :studentUuid " +
                   "AND cd.course_uuid = :courseUuid " +
                   "ORDER BY COALESCE(ce.updated_date, ce.created_date) DESC " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<Enrollment> findLatestByStudentAndCourseUuid(@Param("studentUuid") UUID studentUuid,
                                                          @Param("courseUuid") UUID courseUuid);

    @Query(value = "SELECT ce.* " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "WHERE ce.student_uuid = :studentUuid " +
                   "AND si.class_definition_uuid = :classDefinitionUuid " +
                   "ORDER BY COALESCE(ce.updated_date, ce.created_date) DESC " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<Enrollment> findLatestByStudentAndClassDefinitionUuid(@Param("studentUuid") UUID studentUuid,
                                                                    @Param("classDefinitionUuid") UUID classDefinitionUuid);

    @Query(value = "SELECT ce.* " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE ce.student_uuid = :studentUuid " +
                   "AND cd.course_uuid = :courseUuid " +
                   "AND ce.status IN ('ENROLLED') " +
                   "ORDER BY COALESCE(ce.updated_date, ce.created_date) DESC " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<Enrollment> findLatestActiveByStudentAndCourseUuid(@Param("studentUuid") UUID studentUuid,
                                                                @Param("courseUuid") UUID courseUuid);

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

    /**
     * Monthly enrolment counts for classes owned by the given organisation, from a
     * cut-off date onward. Returns rows of {@code [month (YYYY-MM string), total (long)]}.
     */
    @Query(value = "SELECT to_char(ce.created_date, 'YYYY-MM') AS month, COUNT(*) AS total " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.created_date >= :since " +
                   "GROUP BY 1 " +
                   "ORDER BY 1",
           nativeQuery = true)
    List<Object[]> findEnrolmentTrendsForOrganisation(@Param("organisationUuid") UUID organisationUuid,
                                                      @Param("since") LocalDateTime since);

    /**
     * Hourly enrolment counts for the current day for classes owned by the given
     * organisation. Returns rows of {@code [hour (HH:00 string), total (long)]}.
     */
    @Query(value = "SELECT to_char(ce.created_date, 'HH24:00') AS hour, COUNT(*) AS total " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.created_date >= :startOfDay " +
                   "GROUP BY 1 " +
                   "ORDER BY 1",
           nativeQuery = true)
    List<Object[]> findEnrolmentsByHourTodayForOrganisation(@Param("organisationUuid") UUID organisationUuid,
                                                            @Param("startOfDay") LocalDateTime startOfDay);
}
