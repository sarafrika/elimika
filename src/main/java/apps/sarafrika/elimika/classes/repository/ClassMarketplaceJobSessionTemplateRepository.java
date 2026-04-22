package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassMarketplaceJobSessionTemplateRepository extends JpaRepository<ClassMarketplaceJobSessionTemplate, Long> {

    List<ClassMarketplaceJobSessionTemplate> findByJobUuidOrderByCreatedDateAsc(UUID jobUuid);

    void deleteByJobUuid(UUID jobUuid);
}
