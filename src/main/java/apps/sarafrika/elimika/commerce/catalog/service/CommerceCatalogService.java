package apps.sarafrika.elimika.commerce.catalog.service;

import apps.sarafrika.elimika.commerce.catalog.dto.CommerceCatalogItemDTO;
import apps.sarafrika.elimika.commerce.catalog.dto.UpsertCommerceCatalogItemRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommerceCatalogService {

    CommerceCatalogItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogItemRequest request);

    Optional<CommerceCatalogItemDTO> getByCourse(UUID courseUuid);

    Optional<CommerceCatalogItemDTO> getByClassDefinition(UUID classDefinitionUuid);

    Optional<CommerceCatalogItemDTO> getByVariantId(String medusaVariantId);

    List<CommerceCatalogItemDTO> listAll(Boolean activeOnly);
}
