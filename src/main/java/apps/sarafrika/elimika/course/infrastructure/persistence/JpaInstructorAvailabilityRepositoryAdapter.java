package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.course.domain.InstructorAvailability;
import apps.sarafrika.elimika.course.domain.InstructorAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JpaInstructorAvailabilityRepositoryAdapter implements InstructorAvailabilityRepository {
    private final JpaInstructorAvailabilityRepository jpaInstructorAvailabilityRepository;

    @Override
    public Optional<InstructorAvailability> findById(Long id) {
        return jpaInstructorAvailabilityRepository.findById(id);
    }

    @Override
    public Page<InstructorAvailability> findAllByInstructor(Pageable pageable, Instructor instructor) {
        return jpaInstructorAvailabilityRepository.findAllByInstructor(pageable, instructor);
    }

    @Override
    public void delete(InstructorAvailability instructorAvailability) {
        jpaInstructorAvailabilityRepository.delete(instructorAvailability);
    }

    @Override
    public void save(InstructorAvailability instructorAvailability) {
        jpaInstructorAvailabilityRepository.save(instructorAvailability);
    }

    @Override
    public void saveAll(Set<InstructorAvailability> availabilitySlots) {
        jpaInstructorAvailabilityRepository.saveAll(availabilitySlots);
    }
}
