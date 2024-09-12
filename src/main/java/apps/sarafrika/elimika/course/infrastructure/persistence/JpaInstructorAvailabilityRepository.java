package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.course.domain.InstructorAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInstructorAvailabilityRepository extends JpaRepository<InstructorAvailability, Long> {

    Page<InstructorAvailability> findAllByInstructor(Pageable pageable, Instructor instructor);
}
