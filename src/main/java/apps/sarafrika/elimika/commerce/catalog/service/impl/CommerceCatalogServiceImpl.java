package apps.sarafrika.elimika.commerce.catalog.service.impl;

import apps.sarafrika.elimika.commerce.catalog.dto.CommerceCatalogItemDTO;
import apps.sarafrika.elimika.commerce.catalog.dto.UpsertCommerceCatalogItemRequest;
import apps.sarafrika.elimika.commerce.catalog.entity.CommerceCatalogItem;
import apps.sarafrika.elimika.commerce.catalog.repository.CommerceCatalogItemRepository;
import apps.sarafrika.elimika.commerce.catalog.service.CommerceCatalogService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommerceCatalogServiceImpl implements CommerceCatalogService {

    private final CommerceCatalogItemRepository catalogItemRepository;

    @Override
    @Transactional
    public CommerceCatalogItemDTO createItem(UpsertCommerceCatalogItemRequest request) {
        validateAssociation(request.courseUuid(), request.classDefinitionUuid());
        CommerceCatalogItem entity = new CommerceCatalogItem();
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    @Transactional
    public CommerceCatalogItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogItemRequest request) {
        CommerceCatalogItem entity = catalogItemRepository.findByUuid(catalogUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found"));
        validateAssociation(request.courseUuid(), request.classDefinitionUuid());
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    public Optional<CommerceCatalogItemDTO> getByCourse(UUID courseUuid) {
        if (courseUuid == null) {
            return Optional.empty();
        }
        return catalogItemRepository.findByCourseUuid(courseUuid).map(this::toDto);
    }

    @Override
    public Optional<CommerceCatalogItemDTO> getByClassDefinition(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid).map(this::toDto);
    }

    @Override
    public Optional<CommerceCatalogItemDTO> getByVariantId(String medusaVariantId) {
        if (ObjectUtils.isEmpty(medusaVariantId)) {
            return Optional.empty();
        }
        return catalogItemRepository.findByMedusaVariantId(medusaVariantId).map(this::toDto);
    }

    @Override
    public List<CommerceCatalogItemDTO> listAll(Boolean activeOnly) {
        List<CommerceCatalogItem> entities = Boolean.TRUE.equals(activeOnly)
                ? catalogItemRepository.findByActiveTrue()
                : catalogItemRepository.findAll();
        return entities.stream().map(this::toDto).toList();
    }

    private void applyRequest(CommerceCatalogItem entity, UpsertCommerceCatalogItemRequest request) {
        entity.setCourseUuid(request.courseUuid());
        entity.setClassDefinitionUuid(request.classDefinitionUuid());
        entity.setMedusaProductId(request.medusaProductId());
        entity.setMedusaVariantId(request.medusaVariantId());
        entity.setCurrencyCode(request.currencyCode());
        if (request.active() != null) {
            entity.setActive(request.active());
        }
    }

    private CommerceCatalogItem saveEntity(CommerceCatalogItem entity) {
        try {
            return catalogItemRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Catalog mapping violates uniqueness constraints", ex);
        }
    }

    private void validateAssociation(UUID courseUuid, UUID classDefinitionUuid) {
        if (courseUuid == null && classDefinitionUuid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either course_uuid or class_definition_uuid must be provided");
        }
    }

    private CommerceCatalogItemDTO toDto(CommerceCatalogItem entity) {
        return CommerceCatalogItemDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .classDefinitionUuid(entity.getClassDefinitionUuid())
                .medusaProductId(entity.getMedusaProductId())
                .medusaVariantId(entity.getMedusaVariantId())
                .currencyCode(entity.getCurrencyCode())
                .active(entity.isActive())
                .createdDate(entity.getCreatedDate())
                .updatedDate(entity.getLastModifiedDate())
                .build();
    }
}
