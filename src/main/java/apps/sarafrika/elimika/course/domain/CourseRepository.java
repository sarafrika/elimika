package apps.sarafrika.elimika.course.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CourseRepository {
    void delete(Course course);

    Optional<Course> findById(Long id);

    void save(Course course);

    Page<Course> findAll(Pageable pageable);
}
