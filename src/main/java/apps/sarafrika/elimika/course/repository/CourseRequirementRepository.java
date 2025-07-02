package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRequirementRepository extends JpaRepository<CourseRequirement, Long>, JpaSpecificationExecutor<CourseRequirement> {
    Optional<CourseRequirement> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}