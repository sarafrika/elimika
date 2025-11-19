package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseCreatorEducationRepository extends JpaRepository<CourseCreatorEducation, Long>,
        JpaSpecificationExecutor<CourseCreatorEducation> {

    Optional<CourseCreatorEducation> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}
