package apps.sarafrika.elimika.course.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface InstructorRepository {
    Page<Instructor> findAll(Pageable pageable);

    void save(Instructor instructor);

    void delete(Instructor instructor);

    Optional<Instructor> findById(Long id);

    Set<Instructor> findByIdIn(Set<Long> ids);
}
