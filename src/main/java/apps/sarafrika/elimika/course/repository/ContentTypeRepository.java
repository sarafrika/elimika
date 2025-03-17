package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentTypeRepository extends JpaRepository<ContentType, Long> {

    Optional<ContentType> findByName(String name);
}
