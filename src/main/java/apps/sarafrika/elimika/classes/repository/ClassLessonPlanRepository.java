package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassLessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassLessonPlanRepository extends JpaRepository<ClassLessonPlan, Long> {

    Optional<ClassLessonPlan> findByUuid(UUID uuid);

    List<ClassLessonPlan> findByClassDefinitionUuid(UUID classDefinitionUuid);

    Optional<ClassLessonPlan> findByClassDefinitionUuidAndLessonUuid(UUID classDefinitionUuid, UUID lessonUuid);
}
