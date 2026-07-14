package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassDefinitionResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassDefinitionResourceRepository extends JpaRepository<ClassDefinitionResource, Long> {

    List<ClassDefinitionResource> findByClassDefinitionUuidOrderByCreatedDateAsc(UUID classDefinitionUuid);
}
