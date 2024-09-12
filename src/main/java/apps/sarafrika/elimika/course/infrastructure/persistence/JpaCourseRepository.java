package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCourseRepository extends JpaRepository<Course, Long> {
}
