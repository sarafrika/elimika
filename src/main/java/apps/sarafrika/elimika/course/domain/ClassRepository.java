package apps.sarafrika.elimika.course.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface ClassRepository {
    Page<Class> findAll(Pageable pageable);

    Optional<Class> findById(Long id);

    void delete(Class classEntity);

    void save(Class classEntity);
}
