package apps.sarafrika.elimika.commerce.catalogue.service;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

public interface CommerceCatalogueService {

    CommerceCatalogueItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogueItemRequest request);

    List<CommerceCatalogueItemDTO> getByCourse(UUID courseUuid);

    List<CommerceCatalogueItemDTO> getByClassDefinition(UUID classDefinitionUuid);

    List<CommerceCatalogueItemDTO> getByProgram(UUID programUuid);

    List<CommerceCatalogueItemDTO> getByCourseOrClass(UUID courseUuid, UUID classDefinitionUuid);

    List<CommerceCatalogueItemDTO> getByCourseOrClassOrProgram(UUID courseUuid, UUID classDefinitionUuid, UUID programUuid);

    Optional<CommerceCatalogueItemDTO> getByVariantCode(String variantCode);

    List<CommerceCatalogueItemDTO> listAll(Boolean activeOnly);

    CommerceCatalogueItemDTO createItem(UpsertCommerceCatalogueItemRequest request);

    Page<CommerceCatalogueItemDTO> search(Map<String, String> searchParams, Pageable pageable);
}
