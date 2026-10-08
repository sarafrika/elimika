package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AgeGroupLessonHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AgeGroupLessonHoursRepository extends JpaRepository<AgeGroupLessonHours, Long> {

    List<AgeGroupLessonHours> findByAgeGroupUuidIn(Collection<UUID> ageGroupUuids);
}
