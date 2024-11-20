package apps.sarafrika.elimika.instructor.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstructorAvailabilityRepository extends JpaRepository<InstructorAvailability, Long> {

    Page<InstructorAvailability> findAllByInstructorId(Long instructorId, Pageable pageable);

    Optional<InstructorAvailability> findByIdAndInstructorId(Long id, Long instructorId);
}
