package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassAssignmentScheduleRepository extends JpaRepository<ClassAssignmentSchedule, Long> {

    Optional<ClassAssignmentSchedule> findByUuid(UUID uuid);

    Optional<ClassAssignmentSchedule> findByClassDefinitionUuidAndAssignmentUuid(UUID classDefinitionUuid, UUID assignmentUuid);

    List<ClassAssignmentSchedule> findByClassDefinitionUuid(UUID classDefinitionUuid);

    List<ClassAssignmentSchedule> findByClassDefinitionUuidIn(Collection<UUID> classDefinitionUuids);

    List<ClassAssignmentSchedule> findByClassDefinitionUuidAndLessonUuid(UUID classDefinitionUuid, UUID lessonUuid);

    List<ClassAssignmentSchedule> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    /** Claims the one-day reminder for the current due time; 0 when already sent for it. */
    @Modifying
    @Query(value = "UPDATE class_assignment_schedules SET one_day_reminder_sent_for = due_at " +
                   "WHERE uuid = :uuid AND one_day_reminder_sent_for IS DISTINCT FROM due_at",
           nativeQuery = true)
    int claimOneDayReminder(@Param("uuid") UUID uuid);

    /** Claims the due-now reminder for the current due time; 0 when already sent for it. */
    @Modifying
    @Query(value = "UPDATE class_assignment_schedules SET due_reminder_sent_for = due_at " +
                   "WHERE uuid = :uuid AND due_reminder_sent_for IS DISTINCT FROM due_at",
           nativeQuery = true)
    int claimDueReminder(@Param("uuid") UUID uuid);

    void deleteByUuid(UUID uuid);

    long countByDueAtBetween(LocalDateTime start, LocalDateTime end);

    long countByDueAtBefore(LocalDateTime threshold);
}
