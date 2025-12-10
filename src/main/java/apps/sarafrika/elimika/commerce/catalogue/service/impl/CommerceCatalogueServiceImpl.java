package apps.sarafrika.elimika.commerce.catalogue.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService.VisibilityContext;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final GenericSpecificationBuilder<CommerceCatalogueItem> specificationBuilder;
    private final CommerceCatalogueAccessService accessService;

    @Override
    @Transactional
    public CommerceCatalogueItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItem entity = catalogItemRepository.findByUuid(catalogUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue item not found"));
        validateAssociation(request.courseUuid(), request.classDefinitionUuid());
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    @Transactional
    public CommerceCatalogueItemDTO createItem(UpsertCommerceCatalogueItemRequest request) {
        validateAssociation(request.courseUuid(), request.classDefinitionUuid());
        CommerceCatalogueItem entity = new CommerceCatalogueItem();
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    public List<CommerceCatalogueItemDTO> getByCourse(UUID courseUuid) {
        return getByCourse(courseUuid, accessService.buildContext());
    }

    @Override
    public List<CommerceCatalogueItemDTO> getByClassDefinition(UUID classDefinitionUuid) {
        return getByClassDefinition(classDefinitionUuid, accessService.buildContext());
    }

    @Override
    public Optional<CommerceCatalogueItemDTO> getByVariantCode(String variantCode) {
        return mapIfVisible(
                ObjectUtils.isEmpty(variantCode)
                        ? Optional.empty()
                        : catalogItemRepository.findByVariantCode(variantCode),
                accessService.buildContext());
    }

    @Override
    public List<CommerceCatalogueItemDTO> getByCourseOrClass(UUID courseUuid, UUID classDefinitionUuid) {
        VisibilityContext context = accessService.buildContext();
        List<CommerceCatalogueItemDTO> results = new ArrayList<>();
        results.addAll(getByCourse(courseUuid, context));
        results.addAll(getByClassDefinition(classDefinitionUuid, context));
        LinkedHashMap<UUID, CommerceCatalogueItemDTO> distinct = new LinkedHashMap<>();
        for (CommerceCatalogueItemDTO dto : results) {
            if (dto.uuid() != null) {
                distinct.putIfAbsent(dto.uuid(), dto);
            }
        }
        return new ArrayList<>(distinct.values());
    }

    @Override
    public List<CommerceCatalogueItemDTO> listAll(Boolean activeOnly) {
        Map<String, String> params = new HashMap<>();
        if (Boolean.TRUE.equals(activeOnly)) {
            params.put("active", "true");
        }
        applyPublicFilterWhenAnonymous(params);

        Specification<CommerceCatalogueItem> spec = specificationBuilder.buildSpecification(
                CommerceCatalogueItem.class, params);
        List<CommerceCatalogueItem> entities = spec == null
                ? catalogItemRepository.findAll()
                : catalogItemRepository.findAll(spec);
        return entities.stream().map(this::toDto).toList();
    }

    @Override
    public Page<CommerceCatalogueItemDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> effectiveParams = new HashMap<>(searchParams);
        applyPublicFilterWhenAnonymous(effectiveParams);

        Specification<CommerceCatalogueItem> spec = specificationBuilder.buildSpecification(
                CommerceCatalogueItem.class, effectiveParams);
        Page<CommerceCatalogueItem> page = spec == null
                ? catalogItemRepository.findAll(pageable)
                : catalogItemRepository.findAll(spec, pageable);
        return page.map(this::toDto);
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

    private List<CommerceCatalogueItemDTO> getByCourse(UUID courseUuid, VisibilityContext context) {
        if (courseUuid == null) {
            return List.of();
        }
        return mapVisible(catalogItemRepository.findByCourseUuid(courseUuid), context);
    }

    private List<CommerceCatalogueItemDTO> getByClassDefinition(UUID classDefinitionUuid, VisibilityContext context) {
        if (classDefinitionUuid == null) {
            return List.of();
        }
        return mapVisible(catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid), context);
    }

    private Optional<CommerceCatalogueItemDTO> mapIfVisible(Optional<CommerceCatalogueItem> item, VisibilityContext context) {
        return item.filter(candidate -> accessService.canView(candidate, context))
                .map(this::toDto);
    }

    private List<CommerceCatalogueItemDTO> mapVisible(List<CommerceCatalogueItem> items, VisibilityContext context) {
        return items.stream()
                .filter(item -> accessService.canView(item, context))
                .map(this::toDto)
                .toList();
    }

    private void applyPublicFilterWhenAnonymous(Map<String, String> params) {
        VisibilityContext context = accessService.buildContext();
        if (!context.authenticated() && !params.containsKey("publiclyVisible")) {
            params.put("publiclyVisible", "true");
        }
    }
}
