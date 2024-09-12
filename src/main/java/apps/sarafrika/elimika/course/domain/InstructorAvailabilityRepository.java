package apps.sarafrika.elimika.course.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface InstructorAvailabilityRepository {
    Optional<InstructorAvailability> findById(Long id);

    Page<InstructorAvailability> findAllByInstructor(Pageable pageable, Instructor instructor);

    void delete(InstructorAvailability instructorAvailability);

    void save(InstructorAvailability instructorAvailability);

    void saveAll(Set<InstructorAvailability> availabilitySlots);
}
