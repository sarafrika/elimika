package apps.sarafrika.elimika.instructor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    Set<Instructor> findByIdIn(Set<Long> ids);
}
