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

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.scheduledInstanceUuid = :scheduledInstanceUuid " +
           "AND e.status IN :statuses")
    Long countEnrollmentsByScheduledInstanceAndStatusIn(@Param("scheduledInstanceUuid") UUID scheduledInstanceUuid,
                                                        @Param("statuses") Collection<EnrollmentStatus> statuses);

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
     * cut-off date onward. Each month counts <em>distinct students per distinct course</em>
     * (falling back to the programme, then the class definition, when a class is not tied to a
     * course) rather than raw enrolment rows, so a learner attending several sessions of the
     * same course is counted once. Cancelled and waitlisted enrolments are excluded.
     * Returns rows of {@code [month (YYYY-MM string), total (long)]}.
     */
    @Query(value = "SELECT to_char(ce.created_date, 'YYYY-MM') AS month, " +
                   "COUNT(DISTINCT (ce.student_uuid, " +
                   "COALESCE(cd.course_uuid, cd.program_uuid, si.class_definition_uuid))) AS total " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.created_date >= :since " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
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

    /**
     * Weekly enrolment counts for classes owned by the given organisation, from a
     * cut-off date onward. Each ISO week counts <em>distinct students per distinct course</em>
     * (same offering key as {@link #findEnrolmentTrendsForOrganisation}), excluding cancelled
     * and waitlisted enrolments. Returns rows of {@code [week (IYYY-"W"IW string), total (long)]}.
     */
    @Query(value = "SELECT to_char(ce.created_date, 'IYYY-\"W\"IW') AS week, " +
                   "COUNT(DISTINCT (ce.student_uuid, " +
                   "COALESCE(cd.course_uuid, cd.program_uuid, si.class_definition_uuid))) AS total " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.created_date >= :since " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
                   "GROUP BY 1 " +
                   "ORDER BY 1",
           nativeQuery = true)
    List<Object[]> findWeeklyEnrolmentGrowthForOrganisation(@Param("organisationUuid") UUID organisationUuid,
                                                            @Param("since") LocalDateTime since);

    /**
     * Distinct active-enrolment counts per class definition for an organisation.
     * Returns rows of {@code [class_definition_uuid (UUID), enrolled (long)]}.
     */
    @Query(value = "SELECT si.class_definition_uuid AS class_uuid, COUNT(DISTINCT ce.student_uuid) AS enrolled " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
                   "GROUP BY si.class_definition_uuid",
           nativeQuery = true)
    List<Object[]> findClassEnrolmentCountsForOrganisation(@Param("organisationUuid") UUID organisationUuid);

    /**
     * Recent, human-meaningful activity for an organisation, newest first: students enrolling,
     * classes being opened, and instructors being paid. Returns rows of
     * {@code [event_type (String), occurred_at (Timestamp), class_title (String),
     * subject_uuid (UUID), amount (BigDecimal), currency_code (String)]} — the amount/currency are
     * only populated for {@code PAYOUT} rows, and subject_uuid is null for {@code CLASS_OPENED}.
     * Reads settled instructor obligations directly for the payout events; this is a read-only
     * dashboard aggregation, deliberately spanning tables the way the other org analytics do.
     * Enrolment events are collapsed to one per (student, class) at their latest enrolment time, so a
     * learner attending many sessions of a class shows as a single "enrolled" entry, not one per session.
     */
    @Query(value = "SELECT event_type, occurred_at, class_title, subject_uuid, amount, currency_code FROM ( " +
                   "SELECT 'ENROLMENT' AS event_type, MAX(ce.created_date) AS occurred_at, cd.title AS class_title, " +
                   "       ce.student_uuid AS subject_uuid, CAST(NULL AS numeric) AS amount, " +
                   "       CAST(NULL AS varchar) AS currency_code " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
                   "GROUP BY ce.student_uuid, cd.uuid, cd.title " +
                   "UNION ALL " +
                   "SELECT 'CLASS_OPENED', cd.created_date, cd.title, CAST(NULL AS uuid), " +
                   "       CAST(NULL AS numeric), CAST(NULL AS varchar) " +
                   "FROM class_definitions cd " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "UNION ALL " +
                   "SELECT 'PAYOUT', io.settled_at, cd.title, io.instructor_user_uuid, io.rate_amount, io.currency_code " +
                   "FROM instructor_obligations io " +
                   "LEFT JOIN class_definitions cd ON io.class_definition_uuid = cd.uuid " +
                   "WHERE io.organisation_uuid = :organisationUuid " +
                   "AND io.status = 'SETTLED' AND io.settled_at IS NOT NULL " +
                   ") feed " +
                   "ORDER BY occurred_at DESC",
           nativeQuery = true)
    List<Object[]> findActivityFeedForOrganisation(@Param("organisationUuid") UUID organisationUuid,
                                                   Pageable pageable);

    /**
     * Per-student enrolment/attendance summary for an organisation. Returns rows of
     * {@code [student_uuid (UUID), total (long), completed (long)]} where total
     * excludes cancelled/waitlisted and completed counts ATTENDED enrolments.
     */
    @Query(value = "SELECT ce.student_uuid AS student_uuid, " +
                   "COUNT(*) AS total, " +
                   "COUNT(*) FILTER (WHERE ce.status = 'ATTENDED') AS completed " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
                   "GROUP BY ce.student_uuid",
           nativeQuery = true)
    List<Object[]> findStudentEnrolmentSummariesForOrganisation(@Param("organisationUuid") UUID organisationUuid);

    /**
     * One student's per-class performance <em>within a single organisation</em>. Returns rows of
     * {@code [class_definition_uuid (UUID), class_title (String), total_sessions (long),
     * attended (long), absent (long), last_session (Timestamp)]}.
     * <p>
     * The join through {@code class_definitions.organisation_uuid} is what confines an
     * organisation to its own classes: a student's learning at any other institution is
     * unreachable through this query by construction, rather than by filtering afterwards.
     */
    @Query(value = "SELECT cd.uuid AS class_definition_uuid, " +
                   "cd.title AS class_title, " +
                   "COUNT(*) AS total_sessions, " +
                   "COUNT(*) FILTER (WHERE ce.status = 'ATTENDED') AS attended, " +
                   "COUNT(*) FILTER (WHERE ce.status = 'ABSENT') AS absent, " +
                   "MAX(si.start_time) AS last_session " +
                   "FROM class_enrollments ce " +
                   "JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid " +
                   "JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid " +
                   "WHERE cd.organisation_uuid = :organisationUuid " +
                   "AND ce.student_uuid = :studentUuid " +
                   "AND ce.status NOT IN ('CANCELLED', 'WAITLISTED') " +
                   "GROUP BY cd.uuid, cd.title " +
                   "ORDER BY MAX(si.start_time) DESC NULLS LAST",
           nativeQuery = true)
    List<Object[]> findStudentPerformanceForOrganisation(@Param("organisationUuid") UUID organisationUuid,
                                                         @Param("studentUuid") UUID studentUuid);
}
