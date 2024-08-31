package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

}
