package apps.sarafrika.elimika.commerce.catalog.repository;

import apps.sarafrika.elimika.commerce.catalog.entity.CommerceCatalogItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommerceCatalogItemRepository extends JpaRepository<CommerceCatalogItem, Long> {

    Optional<CommerceCatalogItem> findByUuid(UUID uuid);

    Optional<CommerceCatalogItem> findByCourseUuid(UUID courseUuid);

    Optional<CommerceCatalogItem> findByClassDefinitionUuid(UUID classDefinitionUuid);

    Optional<CommerceCatalogItem> findByMedusaVariantId(String medusaVariantId);

    List<CommerceCatalogItem> findByActiveTrue();
}
