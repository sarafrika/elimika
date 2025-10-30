package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassQuizScheduleRepository extends JpaRepository<ClassQuizSchedule, Long> {

    Optional<ClassQuizSchedule> findByUuid(UUID uuid);

    Optional<ClassQuizSchedule> findByClassDefinitionUuidAndQuizUuid(UUID classDefinitionUuid, UUID quizUuid);

    List<ClassQuizSchedule> findByClassDefinitionUuid(UUID classDefinitionUuid);

    List<ClassQuizSchedule> findByClassDefinitionUuidAndLessonUuid(UUID classDefinitionUuid, UUID lessonUuid);

    List<ClassQuizSchedule> findByDueAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteByUuid(UUID uuid);

    long countByDueAtBetween(LocalDateTime start, LocalDateTime end);

    long countByDueAtBefore(LocalDateTime threshold);
}
