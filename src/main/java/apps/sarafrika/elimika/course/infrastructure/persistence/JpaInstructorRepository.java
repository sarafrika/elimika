package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface JpaInstructorRepository extends JpaRepository<Instructor, Long> {

    Set<Instructor> findByIdIn(Set<Long> ids);
}
