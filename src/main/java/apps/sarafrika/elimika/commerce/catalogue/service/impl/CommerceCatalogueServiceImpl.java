package apps.sarafrika.elimika.commerce.catalogue.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService.VisibilityContext;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import apps.sarafrika.elimika.shared.spi.CourseCatalogueLookupService;
import apps.sarafrika.elimika.shared.spi.CourseCatalogueSnapshot;
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
     * Search keys a caller other than a platform admin may not influence: the field name, its column name, and any
     * operator-suffixed form of either (e.g. {@code publiclyVisible_noteq}, {@code publicly_visible_in}).
     */
    private static final Set<String> ANONYMOUS_RESERVED_FIELDS = Set.of("publiclyvisible", "publicly_visible", "active");

    private final CommerceCatalogueItemRepository catalogItemRepository;
    private final CurrencyService currencyService;
    private final GenericSpecificationBuilder<CommerceCatalogueItem> specificationBuilder;
    private final CommerceCatalogueAccessService accessService;
    private final CourseCatalogueLookupService courseCatalogueLookupService;
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
        return toDtos(entities, Map.of());
    }

    @Override
    public Page<CommerceCatalogueItemDTO> search(Map<String, String> searchParams, Pageable pageable) {
        // The sort allow-list is applied to what the client asked for; visibility is then enforced
        // inside the query itself rather than by filtering rows afterwards.
        specificationBuilder.validateSortProperties(CommerceCatalogueItem.class, pageable);
        Specification<CommerceCatalogueItem> spec = buildScopedSpecification(searchParams, accessService.buildContext());
        Page<CommerceCatalogueItem> page = spec == null
                ? catalogItemRepository.findAll(pageable)
                : catalogItemRepository.findAll(spec, pageable);

        /*
         * Resolve every course on the page in one go.
         *
         * A catalogue row carries a course uuid and a price and nothing a storefront can render, so
         * clients fetched each course themselves — one request per row, against an endpoint that
         * needs a token. The public catalogue therefore came back empty for anonymous visitors
         * while the page's own counters, which never made that call, still reported courses.
         *
         * Attaching the projection here costs three queries for the whole page however long it is,
         * and the storefront needs no per-row call at all.
         */
        Map<UUID, CourseCatalogueSnapshot> courses = courseCatalogueLookupService.findPublicByUuids(
                page.getContent().stream()
                        .map(CommerceCatalogueItem::getCourseUuid)
                        .filter(Objects::nonNull)
                        .toList());

        Titles titles = resolveTitles(page.getContent(), courses);
        Prices prices = resolvePrices(page.getContent(), titles);
        return page.map(entity -> toDto(
                entity,
                lookup(courses, entity.getCourseUuid()),
                titles,
                resolveUnitAmount(entity, titles, prices)));
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

    /** Without a course projection: the shape every endpoint but {@code /search} returns. */
    private CommerceCatalogueItemDTO toDto(CommerceCatalogueItem entity) {
        return toDtos(List.of(entity), Map.of()).getFirst();
    }

    private List<CommerceCatalogueItemDTO> toDtos(
            List<CommerceCatalogueItem> entities,
            Map<UUID, CourseCatalogueSnapshot> courses) {
        Titles titles = resolveTitles(entities, courses);
        return entities.stream()
                .map(entity -> toDto(entity, lookup(courses, entity.getCourseUuid()), titles))
                .toList();
    }

    /** Class snapshots and course names for a batch of rows: two queries however many rows there are. */
    private record Titles(Map<UUID, ClassDefinitionLookupService.ClassDefinitionSnapshot> classes,
                          Map<UUID, String> courseNames) {
    }

    private Titles resolveTitles(List<CommerceCatalogueItem> entities, Map<UUID, CourseCatalogueSnapshot> courses) {
        Map<UUID, ClassDefinitionLookupService.ClassDefinitionSnapshot> classes =
                classDefinitionLookupService.findByUuids(entities.stream()
                        .map(CommerceCatalogueItem::getClassDefinitionUuid)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList());

        Map<UUID, String> courseNames = new HashMap<>();
        courses.forEach((uuid, course) -> {
            if (course != null && course.name() != null) {
                courseNames.put(uuid, course.name());
            }
        });
        List<UUID> missing = Stream.concat(
                        entities.stream().map(CommerceCatalogueItem::getCourseUuid),
                        classes.values().stream().map(ClassDefinitionLookupService.ClassDefinitionSnapshot::courseUuid))
                .filter(Objects::nonNull)
                .filter(uuid -> !courseNames.containsKey(uuid))
                .distinct()
                .toList();
        if (!missing.isEmpty()) {
            courseNames.putAll(courseCatalogueLookupService.findNamesByUuids(missing));
        }
        return new Titles(classes, courseNames);
    }

    /** Null-safe get: lookups may hand back immutable maps, which reject a null key. */
    private static <V> V lookup(Map<UUID, V> map, UUID key) {
        return key == null ? null : map.get(key);
    }

    private CommerceCatalogueItemDTO toDto(CommerceCatalogueItem entity, CourseCatalogueSnapshot course, Titles titles) {
        return toDto(entity, course, titles,
                resolveUnitAmount(entity, lookup(titles.classes(), entity.getClassDefinitionUuid())));
    }

    private CommerceCatalogueItemDTO toDto(
            CommerceCatalogueItem entity,
            CourseCatalogueSnapshot course,
            Titles titles,
            BigDecimal unitAmount) {
        ClassDefinitionLookupService.ClassDefinitionSnapshot classDefinition =
                lookup(titles.classes(), entity.getClassDefinitionUuid());
        UUID titleCourseUuid = entity.getCourseUuid() != null
                ? entity.getCourseUuid()
                : classDefinition == null ? null : classDefinition.courseUuid();
        return CommerceCatalogueItemDTO.builder()
                .course(course)
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .classDefinitionUuid(entity.getClassDefinitionUuid())
                .classDefinitionTitle(classDefinition == null ? null : classDefinition.title())
                .courseTitle(lookup(titles.courseNames(), titleCourseUuid))
                .programUuid(entity.getProgramUuid())
                .productCode(entity.getProductCode())
                .variantCode(entity.getVariantCode())
                .unitAmount(unitAmount)
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
        return toDtos(items.stream()
                .filter(item -> accessService.canView(item, context))
                .toList(), Map.of());
    }

    private BigDecimal resolveUnitAmount(
            CommerceCatalogueItem item,
            ClassDefinitionLookupService.ClassDefinitionSnapshot classDefinition) {
        if (item == null || ObjectUtils.isEmpty(item.getVariantCode())) {
            return null;
        }
        BigDecimal baseAmount = variantRepository.findByCode(item.getVariantCode())
                .map(CommerceCatalogueServiceImpl::baseAmount)
                .orElse(null);
        return applyRateBasis(baseAmount, item, classDefinition, classScheduleService::getScheduleSummary);
    }

    /** The search-page counterpart: every input comes from the page-wide batch lookups. */
    private BigDecimal resolveUnitAmount(CommerceCatalogueItem item, Titles titles, Prices prices) {
        if (item == null || ObjectUtils.isEmpty(item.getVariantCode())) {
            return null;
        }
        CommerceProductVariant variant = prices.variantsByCode().get(item.getVariantCode());
        return applyRateBasis(
                variant == null ? null : baseAmount(variant),
                item,
                lookup(titles.classes(), item.getClassDefinitionUuid()),
                uuid -> lookup(prices.summariesByUuid(), uuid));
    }

    /** Variants and schedule summaries for a page of rows: two queries however many rows there are. */
    private record Prices(Map<String, CommerceProductVariant> variantsByCode,
                          Map<UUID, ClassScheduleSummary> summariesByUuid) {
    }

    private Prices resolvePrices(List<CommerceCatalogueItem> entities, Titles titles) {
        List<String> codes = entities.stream()
                .map(CommerceCatalogueItem::getVariantCode)
                .filter(code -> !ObjectUtils.isEmpty(code))
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return new Prices(Map.of(), Map.of());
        }
        Map<String, CommerceProductVariant> variants = variantRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(CommerceProductVariant::getCode, Function.identity(), (a, b) -> a));

        // Only priced rows whose class carries a rate basis need its timetable.
        List<UUID> classUuids = entities.stream()
                .filter(entity -> !ObjectUtils.isEmpty(entity.getVariantCode()))
                .map(CommerceCatalogueItem::getClassDefinitionUuid)
                .filter(Objects::nonNull)
                .filter(uuid -> {
                    ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = lookup(titles.classes(), uuid);
                    return snapshot != null && snapshot.rateBasis() != null;
                })
                .distinct()
                .toList();
        Map<UUID, ClassScheduleSummary> summaries = classUuids.isEmpty()
                ? Map.of()
                : classScheduleService.getScheduleSummaries(classUuids);
        return new Prices(variants, summaries);
    }

    private static BigDecimal baseAmount(CommerceProductVariant variant) {
        return variant.getUnitAmount() == null ? null : variant.getUnitAmount().setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal applyRateBasis(
            BigDecimal baseAmount,
            CommerceCatalogueItem item,
            ClassDefinitionLookupService.ClassDefinitionSnapshot classDefinition,
            Function<UUID, ClassScheduleSummary> summaries) {
        if (baseAmount == null) {
            return null;
        }

        UUID classDefinitionUuid = item.getClassDefinitionUuid();
        if (classDefinitionUuid == null) {
            return baseAmount;
        }

        RateBasis basis = classDefinition == null ? null : classDefinition.rateBasis();
        if (basis == null) {
            // Without the class there is no contracted unit to multiply by, so the variant price stands alone.
            return baseAmount;
        }

        ClassScheduleSummary summary = summaries.apply(classDefinitionUuid);
        if (summary == null) {
            return baseAmount;
        }

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
        return switch (basis) {
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
        if (!context.admin()) {
            effectiveParams.keySet().removeIf(CommerceCatalogueServiceImpl::isAnonymousReservedParam);
        }

        Specification<CommerceCatalogueItem> spec = specificationBuilder.buildSpecification(
                CommerceCatalogueItem.class, effectiveParams);
        Specification<CommerceCatalogueItem> visibility = accessService.buildVisibilitySpecification(context);
        if (visibility == null) {
            return spec;
        }
        return spec == null ? visibility : spec.and(visibility);
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
