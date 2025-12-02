package apps.sarafrika.elimika.commerce.catalogue.service;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommerceCatalogueService {

    CommerceCatalogueItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogueItemRequest request);

    Optional<CommerceCatalogueItemDTO> getByCourse(UUID courseUuid);

    Optional<CommerceCatalogueItemDTO> getByClassDefinition(UUID classDefinitionUuid);

    Optional<CommerceCatalogueItemDTO> getByVariantCode(String variantCode);

    List<CommerceCatalogueItemDTO> listAll(Boolean activeOnly);

    CommerceCatalogueItemDTO createItem(UpsertCommerceCatalogueItemRequest request);
}
