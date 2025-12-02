package apps.sarafrika.elimika.commerce.catalogue.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
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
public class CommerceCatalogueServiceImpl implements CommerceCatalogueService {

    private final CommerceCatalogueItemRepository catalogItemRepository;
    private final CurrencyService currencyService;

    @Override
    @Transactional
    public CommerceCatalogueItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItem entity = catalogItemRepository.findByUuid(catalogUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found"));
        validateAssociation(request.courseUuid(), request.classDefinitionUuid());
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    public Optional<CommerceCatalogueItemDTO> getByCourse(UUID courseUuid) {
        if (courseUuid == null) {
            return Optional.empty();
        }
        return catalogItemRepository.findByCourseUuid(courseUuid).map(this::toDto);
    }

    @Override
    public Optional<CommerceCatalogueItemDTO> getByClassDefinition(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid).map(this::toDto);
    }

    @Override
    public Optional<CommerceCatalogueItemDTO> getByVariantCode(String variantCode) {
        if (ObjectUtils.isEmpty(variantCode)) {
            return Optional.empty();
        }
        return catalogItemRepository.findByVariantCode(variantCode).map(this::toDto);
    }

    @Override
    public List<CommerceCatalogueItemDTO> listAll(Boolean activeOnly) {
        List<CommerceCatalogueItem> entities = Boolean.TRUE.equals(activeOnly)
                ? catalogItemRepository.findByActiveTrue()
                : catalogItemRepository.findAll();
        return entities.stream().map(this::toDto).toList();
    }

    private void applyRequest(CommerceCatalogueItem entity, UpsertCommerceCatalogueItemRequest request) {
        entity.setCourseUuid(request.courseUuid());
        entity.setClassDefinitionUuid(request.classDefinitionUuid());
        entity.setProductCode(request.productCode());
        entity.setVariantCode(request.variantCode());
        String currencyCode = currencyService.resolveCurrencyOrDefault(request.currencyCode()).getCode();
        entity.setCurrencyCode(currencyCode);
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        if (request.publiclyVisible() != null) {
            entity.setPubliclyVisible(request.publiclyVisible());
        }
    }

    private CommerceCatalogueItem saveEntity(CommerceCatalogueItem entity) {
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

    private CommerceCatalogueItemDTO toDto(CommerceCatalogueItem entity) {
        return CommerceCatalogueItemDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .classDefinitionUuid(entity.getClassDefinitionUuid())
                .productCode(entity.getProductCode())
                .variantCode(entity.getVariantCode())
                .currencyCode(entity.getCurrencyCode())
                .active(entity.isActive())
                .publiclyVisible(entity.isPubliclyVisible())
                .createdDate(entity.getCreatedDate())
                .updatedDate(entity.getLastModifiedDate())
                .build();
    }
}
