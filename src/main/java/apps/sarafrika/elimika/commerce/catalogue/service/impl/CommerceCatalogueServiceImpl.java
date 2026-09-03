package apps.sarafrika.elimika.commerce.catalogue.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService.VisibilityContext;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService.ClassScheduleSummary;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;

@Service
@RequiredArgsConstructor
public class CommerceCatalogueServiceImpl implements CommerceCatalogueService {

    /**
     * Search keys an anonymous caller may not influence: the field name, its column name, and any
     * operator-suffixed form of either (e.g. {@code publiclyVisible_noteq}, {@code publicly_visible_in}).
     */
    private static final Set<String> ANONYMOUS_RESERVED_FIELDS = Set.of("publiclyvisible", "publicly_visible", "active");

    private final CommerceCatalogueItemRepository catalogItemRepository;
    private final CurrencyService currencyService;
    private final GenericSpecificationBuilder<CommerceCatalogueItem> specificationBuilder;
    private final CommerceCatalogueAccessService accessService;
    private final CommerceProductVariantRepository variantRepository;
    private final ClassScheduleService classScheduleService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @Override
    @Transactional
    public CommerceCatalogueItemDTO updateItem(UUID catalogUuid, UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItem entity = catalogItemRepository.findByUuid(catalogUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue item not found"));
        validateAssociation(request.courseUuid(), request.classDefinitionUuid(), request.programUuid());
        applyRequest(entity, request);
        return toDto(saveEntity(entity));
    }

    @Override
    @Transactional
    public CommerceCatalogueItemDTO createItem(UpsertCommerceCatalogueItemRequest request) {
        validateAssociation(request.courseUuid(), request.classDefinitionUuid(), request.programUuid());
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
    public List<CommerceCatalogueItemDTO> getByProgram(UUID programUuid) {
        return getByProgram(programUuid, accessService.buildContext());
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
        return getByCourseOrClassOrProgram(courseUuid, classDefinitionUuid, null);
    }

    @Override
    public List<CommerceCatalogueItemDTO> getByCourseOrClassOrProgram(
            UUID courseUuid,
            UUID classDefinitionUuid,
            UUID programUuid) {
        VisibilityContext context = accessService.buildContext();
        List<CommerceCatalogueItemDTO> results = new ArrayList<>();
        results.addAll(getByCourse(courseUuid, context));
        results.addAll(getByClassDefinition(classDefinitionUuid, context));
        results.addAll(getByProgram(programUuid, context));
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

        Specification<CommerceCatalogueItem> spec = buildScopedSpecification(params, accessService.buildContext());
        List<CommerceCatalogueItem> entities = spec == null
                ? catalogItemRepository.findAll()
                : catalogItemRepository.findAll(spec);
        return entities.stream().map(this::toDto).toList();
    }

    @Override
    public Page<CommerceCatalogueItemDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CommerceCatalogueItem> spec = buildScopedSpecification(searchParams, accessService.buildContext());
        Page<CommerceCatalogueItem> page = spec == null
                ? catalogItemRepository.findAll(pageable)
                : catalogItemRepository.findAll(spec, pageable);
        return page.map(this::toDto);
    }

    private void applyRequest(CommerceCatalogueItem entity, UpsertCommerceCatalogueItemRequest request) {
        entity.setCourseUuid(request.courseUuid());
        entity.setClassDefinitionUuid(request.classDefinitionUuid());
        entity.setProgramUuid(request.programUuid());
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

    private void validateAssociation(UUID courseUuid, UUID classDefinitionUuid, UUID programUuid) {
        if (courseUuid == null && classDefinitionUuid == null && programUuid == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Either course_uuid, class_definition_uuid, or program_uuid must be provided");
        }
    }

    private CommerceCatalogueItemDTO toDto(CommerceCatalogueItem entity) {
        return CommerceCatalogueItemDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .classDefinitionUuid(entity.getClassDefinitionUuid())
                .programUuid(entity.getProgramUuid())
                .productCode(entity.getProductCode())
                .variantCode(entity.getVariantCode())
                .unitAmount(resolveUnitAmount(entity))
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

    private List<CommerceCatalogueItemDTO> getByProgram(UUID programUuid, VisibilityContext context) {
        if (programUuid == null) {
            return List.of();
        }
        return mapVisible(catalogItemRepository.findByProgramUuid(programUuid), context);
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

    private BigDecimal resolveUnitAmount(CommerceCatalogueItem item) {
        if (item == null || ObjectUtils.isEmpty(item.getVariantCode())) {
            return null;
        }
        BigDecimal baseAmount = variantRepository.findByCode(item.getVariantCode())
                .map(variant -> variant.getUnitAmount() == null
                        ? null
                        : variant.getUnitAmount().setScale(4, RoundingMode.HALF_UP))
                .orElse(null);
        if (baseAmount == null) {
            return null;
        }

        UUID classDefinitionUuid = item.getClassDefinitionUuid();
        if (classDefinitionUuid == null) {
            return baseAmount;
        }

        ClassScheduleSummary summary = classScheduleService.getScheduleSummary(classDefinitionUuid);
        if (summary == null) {
            return baseAmount;
        }

        RateBasis basis = classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::rateBasis)
                .orElse(RateBasis.PER_HOUR);

        BigDecimal units = billableUnits(basis, summary);
        if (units.signum() <= 0) {
            return baseAmount;
        }
        return baseAmount.multiply(units).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * How many of the priced unit this class contains. The same multiplier the payout side applies,
     * so the margin between the two prices stays a like-for-like subtraction.
     */
    private BigDecimal billableUnits(RateBasis basis, ClassScheduleSummary summary) {
        return switch (basis == null ? RateBasis.PER_HOUR : basis) {
            case PER_SESSION -> BigDecimal.valueOf(summary.scheduledInstances());
            case PER_DAY -> BigDecimal.valueOf(summary.scheduledDays());
            case PER_HOUR -> BigDecimal.valueOf(summary.scheduledMinutes())
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        };
    }

    /**
     * Builds the search specification for the caller. Authenticated callers get exactly the filters they
     * asked for. Anonymous callers are confined to public, active items by a server-side predicate that is
     * ANDed onto the specification after the caller's filters have been built, so nothing in the request
     * map can widen what they see; their own visibility/active keys are dropped rather than honoured.
     */
    private Specification<CommerceCatalogueItem> buildScopedSpecification(
            Map<String, String> searchParams,
            VisibilityContext context) {
        Map<String, String> effectiveParams = new HashMap<>(searchParams);
        if (!context.authenticated()) {
            effectiveParams.keySet().removeIf(CommerceCatalogueServiceImpl::isAnonymousReservedParam);
        }

        Specification<CommerceCatalogueItem> spec = specificationBuilder.buildSpecification(
                CommerceCatalogueItem.class, effectiveParams);
        if (context.authenticated()) {
            return spec;
        }

        Specification<CommerceCatalogueItem> publicAndActive = (root, query, cb) -> cb.and(
                cb.isTrue(root.get("publiclyVisible")),
                cb.isTrue(root.get("active")));
        return spec == null ? publicAndActive : spec.and(publicAndActive);
    }

    private static boolean isAnonymousReservedParam(String key) {
        if (key == null) {
            return false;
        }
        String normalised = key.toLowerCase(Locale.ROOT);
        return ANONYMOUS_RESERVED_FIELDS.stream()
                .anyMatch(field -> normalised.equals(field) || normalised.startsWith(field + "_"));
    }
}
