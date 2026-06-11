package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassSessionTemplateRepository extends JpaRepository<ClassSessionTemplate, Long> {

    List<ClassSessionTemplate> findByClassDefinitionUuidOrderByTemplateOrderAscCreatedDateAsc(UUID classDefinitionUuid);

    long countByClassDefinitionUuid(UUID classDefinitionUuid);
}
