package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long>,
        JpaSpecificationExecutor<CourseAssessment> {
    Optional<CourseAssessment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}
