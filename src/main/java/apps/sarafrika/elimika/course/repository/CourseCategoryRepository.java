package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    List<CourseCategory> findByCourseId(Long courseId);
}