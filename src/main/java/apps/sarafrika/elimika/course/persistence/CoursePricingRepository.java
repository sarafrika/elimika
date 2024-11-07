package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoursePricingRepository extends JpaRepository<CoursePricing, Long> {

    Optional<CoursePricing> findByCourseId(Long courseId);
}
