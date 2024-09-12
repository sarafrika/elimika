package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.course.domain.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JpaInstructorRepositoryAdapter implements InstructorRepository {
    private final JpaInstructorRepository jpaInstructorRepository;

    @Override
    public Page<Instructor> findAll(Pageable pageable) {
        return jpaInstructorRepository.findAll(pageable);
    }

    @Override
    public void save(Instructor instructor) {
        jpaInstructorRepository.save(instructor);
    }

    @Override
    public void delete(Instructor instructor) {
        jpaInstructorRepository.delete(instructor);
    }

    @Override
    public Optional<Instructor> findById(Long id) {
        return jpaInstructorRepository.findById(id);
    }

    @Override
    public Set<Instructor> findByIdIn(Set<Long> ids) {
        return jpaInstructorRepository.findByIdIn(ids);
    }
}
