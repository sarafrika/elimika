package apps.sarafrika.elimika.commerce.catalogue.repository;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CommerceCatalogueItemRepository extends JpaRepository<CommerceCatalogueItem, Long>,
        JpaSpecificationExecutor<CommerceCatalogueItem> {

    Optional<CommerceCatalogueItem> findByUuid(UUID uuid);

    List<CommerceCatalogueItem> findByCourseUuid(UUID courseUuid);

    List<CommerceCatalogueItem> findByClassDefinitionUuid(UUID classDefinitionUuid);

    Optional<CommerceCatalogueItem> findByVariantCode(String variantCode);

    List<CommerceCatalogueItem> findByActiveTrue();
}
