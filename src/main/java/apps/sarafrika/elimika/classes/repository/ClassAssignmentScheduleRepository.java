package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassAssignmentScheduleRepository extends JpaRepository<ClassAssignmentSchedule, Long> {

    Optional<ClassAssignmentSchedule> findByUuid(UUID uuid);

    Optional<ClassAssignmentSchedule> findByClassDefinitionUuidAndAssignmentUuid(UUID classDefinitionUuid, UUID assignmentUuid);

    List<ClassAssignmentSchedule> findByClassDefinitionUuid(UUID classDefinitionUuid);

    List<ClassAssignmentSchedule> findByClassDefinitionUuidAndLessonUuid(UUID classDefinitionUuid, UUID lessonUuid);

    List<ClassAssignmentSchedule> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteByUuid(UUID uuid);

    long countByDueAtBetween(LocalDateTime start, LocalDateTime end);

    long countByDueAtBefore(LocalDateTime threshold);
}
