package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTrainingRequirementRepository extends JpaRepository<CourseTrainingRequirement, Long>, JpaSpecificationExecutor<CourseTrainingRequirement> {
    Optional<CourseTrainingRequirement> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<CourseTrainingRequirement> findByCourseUuid(UUID courseUuid);
}
