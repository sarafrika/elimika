package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Class;
import apps.sarafrika.elimika.course.domain.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaClassRepositoryAdapter implements ClassRepository {

    private final JpaClassRepository jpaClassRepository;

    @Override
    public Page<Class> findAll(Pageable pageable) {

        return jpaClassRepository.findAll(pageable);
    }

    @Override
    public Optional<Class> findById(Long id) {

        return jpaClassRepository.findById(id);
    }

    @Override
    public void delete(Class classEntity) {

        jpaClassRepository.delete(classEntity);
    }

    @Override
    public void save(Class classEntity) {

        jpaClassRepository.save(classEntity);
    }

}
