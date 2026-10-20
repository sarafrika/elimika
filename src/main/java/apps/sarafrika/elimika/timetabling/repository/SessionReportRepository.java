package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.SessionReport;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionReportRepository extends JpaRepository<SessionReport, Long> {

    Optional<SessionReport> findByScheduledInstanceUuid(UUID scheduledInstanceUuid);

    List<SessionReport> findByStatusAndDueAtBeforeAndOverdueNotifiedAtIsNull(SessionReportStatus status, LocalDateTime dueAt);

    List<SessionReport> findByStatusAndEditableUntilBefore(SessionReportStatus status, LocalDateTime editableUntil);

    Page<SessionReport> findByInstructorUuidAndStatusInOrderByDueAtAsc(UUID instructorUuid,
                                                                       Collection<SessionReportStatus> statuses,
                                                                       Pageable pageable);

    Page<SessionReport> findByOrganisationUuidAndStatusInOrderByDueAtAsc(UUID organisationUuid,
                                                                         Collection<SessionReportStatus> statuses,
                                                                         Pageable pageable);

    /** Locked (or past their grace window) reports for sessions that taught this lesson in a class the learner sits in. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM session_reports sr "
            + "JOIN scheduled_instances si ON si.uuid = sr.scheduled_instance_uuid "
            + "JOIN class_definitions cd ON cd.uuid = si.class_definition_uuid "
            + "JOIN class_enrollments ce ON ce.scheduled_instance_uuid = si.uuid AND ce.student_uuid = :studentUuid "
            + "WHERE (LOWER(sr.status) = 'locked' OR (LOWER(sr.status) = 'submitted' AND sr.editable_until < :now)) "
            + "AND si.lesson_uuid = :lessonUuid "
            + "AND (cd.course_uuid = :courseUuid OR cd.program_uuid IN "
            + "(SELECT pc.program_uuid FROM program_courses pc WHERE pc.course_uuid = :courseUuid)))",
            nativeQuery = true)
    boolean existsLockedForStudentLesson(@Param("studentUuid") UUID studentUuid,
                                         @Param("courseUuid") UUID courseUuid,
                                         @Param("lessonUuid") UUID lessonUuid,
                                         @Param("now") LocalDateTime now);

    /** Claims the "report due" reminder for this report; true for exactly one caller. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SessionReport r SET r.remindedAt = :now WHERE r.uuid = :uuid AND r.remindedAt IS NULL")
    int claimReminder(@Param("uuid") UUID uuid, @Param("now") LocalDateTime now);

    /** Claims the overdue notice for this report; true for exactly one caller. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SessionReport r SET r.overdueNotifiedAt = :now WHERE r.uuid = :uuid AND r.overdueNotifiedAt IS NULL")
    int claimOverdueNotice(@Param("uuid") UUID uuid, @Param("now") LocalDateTime now);

    /** Sessions that ended inside the window, still have learners, and whose report has not been reminded yet. */
    @Query(value = "SELECT si.uuid FROM scheduled_instances si "
            + "LEFT JOIN session_reports sr ON sr.scheduled_instance_uuid = si.uuid "
            + "WHERE si.class_definition_uuid IS NOT NULL AND UPPER(si.status) <> 'CANCELLED' "
            + "AND COALESCE(si.concluded_at, si.end_time) > :since AND COALESCE(si.concluded_at, si.end_time) <= :now "
            + "AND (sr.uuid IS NULL OR (sr.reminded_at IS NULL AND LOWER(sr.status) = 'draft')) "
            + "AND EXISTS (SELECT 1 FROM class_enrollments ce WHERE ce.scheduled_instance_uuid = si.uuid "
            + "AND UPPER(ce.status) IN ('ENROLLED', 'ATTENDED', 'ABSENT'))",
            nativeQuery = true)
    List<UUID> findEndedSessionsAwaitingReminder(@Param("since") LocalDateTime since, @Param("now") LocalDateTime now);

    /** Sessions with learners that ended between trackFrom and before but never got a report row (e.g. missed runs). */
    @Query(value = "SELECT si.uuid FROM scheduled_instances si "
            + "WHERE si.class_definition_uuid IS NOT NULL AND UPPER(si.status) <> 'CANCELLED' "
            + "AND COALESCE(si.concluded_at, si.end_time) >= :trackFrom AND COALESCE(si.concluded_at, si.end_time) <= :before "
            + "AND NOT EXISTS (SELECT 1 FROM session_reports sr WHERE sr.scheduled_instance_uuid = si.uuid) "
            + "AND EXISTS (SELECT 1 FROM class_enrollments ce WHERE ce.scheduled_instance_uuid = si.uuid "
            + "AND UPPER(ce.status) IN ('ENROLLED', 'ATTENDED', 'ABSENT'))",
            nativeQuery = true)
    List<UUID> findEndedSessionsWithoutReport(@Param("trackFrom") LocalDateTime trackFrom, @Param("before") LocalDateTime before);
}
