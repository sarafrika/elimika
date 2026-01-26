package apps.sarafrika.elimika.revenue.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueQueryService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.revenue.dto.RevenueAmountDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueScopeBreakdownDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueTimeSeriesPointDTO;
import apps.sarafrika.elimika.revenue.service.RevenueAnalyticsService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RevenueAnalyticsServiceImpl implements RevenueAnalyticsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;
    private static final String UNKNOWN_CURRENCY = "UNKNOWN";

    private final CommerceRevenueQueryService revenueQueryService;
    private final CourseInfoService courseInfoService;
    private final ClassDefinitionService classDefinitionService;
    private final InstructorLookupService instructorLookupService;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final UserLookupService userLookupService;
    private final DomainSecurityService domainSecurityService;

    @Override
    public RevenueDashboardDTO getRevenueDashboard(UserDomain domain, LocalDate startDate, LocalDate endDate) {
        if (domain == null) {
            throw new IllegalArgumentException("domain is required");
        }
        if (!domainSecurityService.hasAnyDomain(domain)) {
            throw new AccessDeniedException("User does not have access to the requested revenue domain");
        }

        DateRange range = resolveDateRange(startDate, endDate);
        List<CommerceRevenueLineItem> lineItems = loadRevenueLines(domain, range);
        Map<UUID, RevenueShare> revenueShares = loadRevenueShares(domain, lineItems);

        return buildDashboard(domain, range, lineItems, revenueShares);
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate resolvedEnd = endDate != null ? endDate : now.toLocalDate();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusDays(30);
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new IllegalArgumentException("start_date must be on or before end_date");
        }
        OffsetDateTime start = resolvedStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = resolvedEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        return new DateRange(resolvedStart, resolvedEnd, start, end);
    }

    private List<CommerceRevenueLineItem> loadRevenueLines(UserDomain domain, DateRange range) {
        return switch (domain) {
            case admin -> revenueQueryService.findCapturedRevenueLines(range.startDateTime(), range.endDateTime());
            case course_creator -> loadCourseCreatorLines(range);
            case instructor -> loadInstructorLines(range);
            case organisation_user -> loadOrganisationLines(range);
            case student, parent -> List.of();
        };
    }

    private List<CommerceRevenueLineItem> loadCourseCreatorLines(DateRange range) {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            return List.of();
        }
        UUID creatorUuid = courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).orElse(null);
        if (creatorUuid == null) {
            return List.of();
        }
        List<UUID> courseUuids = courseInfoService.findCourseUuidsByCourseCreatorUuid(creatorUuid);
        return revenueQueryService.findCapturedRevenueLinesByCourseUuids(
                range.startDateTime(),
                range.endDateTime(),
                courseUuids
        );
    }

    private List<CommerceRevenueLineItem> loadInstructorLines(DateRange range) {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            return List.of();
        }
        UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(userUuid).orElse(null);
        if (instructorUuid == null) {
            return List.of();
        }
        List<UUID> classDefinitionUuids = classDefinitionService.findClassesForInstructor(instructorUuid).stream()
                .map(ClassDefinitionResponseDTO::classDefinition)
                .filter(Objects::nonNull)
                .map(dto -> dto.uuid())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return revenueQueryService.findCapturedRevenueLinesByClassDefinitionUuids(
                range.startDateTime(),
                range.endDateTime(),
                classDefinitionUuids
        );
    }

    private List<CommerceRevenueLineItem> loadOrganisationLines(DateRange range) {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            return List.of();
        }
        List<UUID> organisations = userLookupService.getUserOrganizations(userUuid);
        if (organisations == null || organisations.isEmpty()) {
            return List.of();
        }
        Set<UUID> classDefinitionUuids = new HashSet<>();
        for (UUID organisationUuid : organisations) {
            if (organisationUuid == null) {
                continue;
            }
            classDefinitionService.findClassesForOrganisation(organisationUuid).stream()
                    .map(ClassDefinitionResponseDTO::classDefinition)
                    .filter(Objects::nonNull)
                    .map(dto -> dto.uuid())
                    .filter(Objects::nonNull)
                    .forEach(classDefinitionUuids::add);
        }
        return revenueQueryService.findCapturedRevenueLinesByClassDefinitionUuids(
                range.startDateTime(),
                range.endDateTime(),
                new ArrayList<>(classDefinitionUuids)
        );
    }

    private Map<UUID, RevenueShare> loadRevenueShares(UserDomain domain, List<CommerceRevenueLineItem> lineItems) {
        if (domain != UserDomain.course_creator && domain != UserDomain.instructor) {
            return Map.of();
        }
        List<UUID> courseUuids = lineItems.stream()
                .map(CommerceRevenueLineItem::courseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return courseInfoService.getRevenueShares(courseUuids);
    }

    private RevenueDashboardDTO buildDashboard(
            UserDomain domain,
            DateRange range,
            List<CommerceRevenueLineItem> lineItems,
            Map<UUID, RevenueShare> revenueShares
    ) {
        Aggregate overall = new Aggregate();
        Map<String, Set<String>> ordersByCurrency = new HashMap<>();
        Map<PurchaseScope, Aggregate> scopeAggregates = new EnumMap<>(PurchaseScope.class);
        Map<LocalDate, Aggregate> dailyAggregates = new TreeMap<>();

        for (CommerceRevenueLineItem item : lineItems) {
            if (item == null) {
                continue;
            }
            String currency = normalizeCurrency(item.currencyCode());
            BigDecimal gross = safeAmount(item.itemTotal());
            BigDecimal earnings = calculateEarnings(domain, item, revenueShares);
            int quantity = Math.max(item.quantity(), 0);

            overall.add(item.orderId(), currency, gross, earnings, quantity);

            if (item.scope() != null) {
                scopeAggregates.computeIfAbsent(item.scope(), scope -> new Aggregate())
                        .add(item.orderId(), currency, gross, earnings, quantity);
            }

            LocalDate date = toUtcDate(item.orderCreatedAt());
            if (date != null) {
                dailyAggregates.computeIfAbsent(date, key -> new Aggregate())
                        .add(item.orderId(), currency, gross, earnings, quantity);
            }

            if (StringUtils.hasText(item.orderId())) {
                ordersByCurrency.computeIfAbsent(currency, key -> new HashSet<>()).add(item.orderId());
            }
        }

        List<RevenueAmountDTO> averageOrderValue = buildAverageOrderValues(overall.grossTotals, ordersByCurrency);
        List<RevenueScopeBreakdownDTO> scopeBreakdown = buildScopeBreakdown(scopeAggregates);
        List<RevenueTimeSeriesPointDTO> dailySeries = buildDailySeries(dailyAggregates);

        return new RevenueDashboardDTO(
                domain.name(),
                range.startDate(),
                range.endDate(),
                toAmountList(overall.grossTotals),
                toAmountList(overall.earningsTotals),
                overall.orderIds.size(),
                overall.lineItemCount,
                overall.unitsSold,
                averageOrderValue,
                scopeBreakdown,
                dailySeries
        );
    }

    private List<RevenueAmountDTO> buildAverageOrderValues(
            Map<String, BigDecimal> grossTotals,
            Map<String, Set<String>> ordersByCurrency
    ) {
        List<RevenueAmountDTO> averages = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : grossTotals.entrySet()) {
            String currency = entry.getKey();
            int orderCount = ordersByCurrency.getOrDefault(currency, Set.of()).size();
            BigDecimal avg = orderCount > 0
                    ? entry.getValue().divide(BigDecimal.valueOf(orderCount), SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            averages.add(new RevenueAmountDTO(currency, avg));
        }
        averages.sort(Comparator.comparing(RevenueAmountDTO::currencyCode, Comparator.nullsLast(String::compareTo)));
        return averages;
    }

    private List<RevenueScopeBreakdownDTO> buildScopeBreakdown(Map<PurchaseScope, Aggregate> scopeAggregates) {
        List<RevenueScopeBreakdownDTO> breakdowns = new ArrayList<>();
        for (Map.Entry<PurchaseScope, Aggregate> entry : scopeAggregates.entrySet()) {
            Aggregate aggregate = entry.getValue();
            breakdowns.add(new RevenueScopeBreakdownDTO(
                    entry.getKey().name(),
                    toAmountList(aggregate.grossTotals),
                    toAmountList(aggregate.earningsTotals),
                    aggregate.lineItemCount,
                    aggregate.unitsSold
            ));
        }
        breakdowns.sort(Comparator.comparing(RevenueScopeBreakdownDTO::scope));
        return breakdowns;
    }

    private List<RevenueTimeSeriesPointDTO> buildDailySeries(Map<LocalDate, Aggregate> dailyAggregates) {
        List<RevenueTimeSeriesPointDTO> series = new ArrayList<>();
        for (Map.Entry<LocalDate, Aggregate> entry : dailyAggregates.entrySet()) {
            Aggregate aggregate = entry.getValue();
            series.add(new RevenueTimeSeriesPointDTO(
                    entry.getKey(),
                    toAmountList(aggregate.grossTotals),
                    toAmountList(aggregate.earningsTotals),
                    aggregate.orderIds.size(),
                    aggregate.unitsSold
            ));
        }
        return series;
    }

    private List<RevenueAmountDTO> toAmountList(Map<String, BigDecimal> totals) {
        if (totals.isEmpty()) {
            return List.of();
        }
        return totals.entrySet().stream()
                .map(entry -> new RevenueAmountDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RevenueAmountDTO::currencyCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private BigDecimal calculateEarnings(
            UserDomain domain,
            CommerceRevenueLineItem item,
            Map<UUID, RevenueShare> revenueShares
    ) {
        BigDecimal amount = safeAmount(item.itemTotal());
        return switch (domain) {
            case course_creator -> applyShare(amount, revenueShares.get(item.courseUuid()), true);
            case instructor -> {
                if (item.scope() != PurchaseScope.CLASS) {
                    yield BigDecimal.ZERO;
                }
                yield applyShare(amount, revenueShares.get(item.courseUuid()), false);
            }
            default -> amount;
        };
    }

    private BigDecimal applyShare(BigDecimal amount, RevenueShare share, boolean creatorShare) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (share == null) {
            return amount;
        }
        BigDecimal percentage = creatorShare ? share.creatorSharePercentage() : share.instructorSharePercentage();
        if (percentage == null) {
            return amount;
        }
        return amount.multiply(percentage).divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private String normalizeCurrency(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) {
            return UNKNOWN_CURRENCY;
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate toUtcDate(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private static final class Aggregate {
        private final Map<String, BigDecimal> grossTotals = new HashMap<>();
        private final Map<String, BigDecimal> earningsTotals = new HashMap<>();
        private final Set<String> orderIds = new HashSet<>();
        private long lineItemCount;
        private long unitsSold;

        private void add(String orderId, String currency, BigDecimal gross, BigDecimal earnings, int quantity) {
            lineItemCount += 1;
            unitsSold += quantity;
            if (StringUtils.hasText(orderId)) {
                orderIds.add(orderId);
            }
            mergeAmount(grossTotals, currency, gross);
            mergeAmount(earningsTotals, currency, earnings);
        }

        private void mergeAmount(Map<String, BigDecimal> totals, String currency, BigDecimal amount) {
            totals.merge(currency, amount, BigDecimal::add);
        }
    }

    private record DateRange(
            LocalDate startDate,
            LocalDate endDate,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime
    ) {
    }
}
