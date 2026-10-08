package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingApplicationLessonHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingApplicationLessonHoursRepository extends JpaRepository<TrainingApplicationLessonHours, Long> {

    List<TrainingApplicationLessonHours> findByLearnerGroupUuidIn(Collection<UUID> learnerGroupUuids);
}
