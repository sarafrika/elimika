package apps.sarafrika.elimika.course.infrastructure.persistence;

import apps.sarafrika.elimika.course.domain.Class;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaClassRepository extends JpaRepository<Class, Long> {
}
