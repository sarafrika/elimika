package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Course;
import apps.sarafrika.elimika.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaCourseRepositoryAdapter implements CourseRepository {
    private final JpaCourseRepository jpaCourseRepository;

    @Override
    public void delete(Course course) {
        jpaCourseRepository.delete(course);
    }

    @Override
    public Optional<Course> findById(Long id) {
        return jpaCourseRepository.findById(id);
    }

    @Override
    public void save(Course course) {
        jpaCourseRepository.save(course);
    }

    @Override
    public Page<Course> findAll(Pageable pageable) {
        return jpaCourseRepository.findAll(pageable);
    }
}
