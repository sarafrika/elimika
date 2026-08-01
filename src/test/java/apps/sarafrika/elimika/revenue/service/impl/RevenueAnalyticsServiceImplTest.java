package apps.sarafrika.elimika.revenue.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.commerce.internal.spi.CommercePaymentQueryService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.revenue.dto.RevenueAmountDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueScopeBreakdownDTO;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceRevenueLineItem;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceRevenueQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Guards the promise that the revenue dashboard reports what a wallet will actually receive.
 * <p>
 * The reference for "correct" here is {@code OrderCaptureWalletCreditListener}: on a COURSE sale it
 * credits only the course creator, on a CLASS sale only the class' default instructor, and it
 * credits nobody when the course has no revenue share configured. Any number this service reports
 * that the listener would not credit is a reporting defect.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Revenue analytics earnings")
class RevenueAnalyticsServiceImplTest {

    private static final UUID USER_UUID = UUID.randomUUID();
    private static final UUID CREATOR_UUID = UUID.randomUUID();
    private static final UUID INSTRUCTOR_UUID = UUID.randomUUID();
    private static final UUID COURSE_UUID = UUID.randomUUID();
    private static final UUID CLASS_UUID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    @Mock
    private CommerceRevenueQueryService revenueQueryService;
    @Mock
    private CommercePaymentQueryService paymentQueryService;
    @Mock
    private CourseInfoService courseInfoService;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private CourseCreatorLookupService courseCreatorLookupService;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private StudentGuardianLookupService studentGuardianLookupService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private DomainSecurityService domainSecurityService;

    @InjectMocks
    private RevenueAnalyticsServiceImpl service;

    private static CommerceRevenueLineItem line(PurchaseScope scope, String total) {
        return new CommerceRevenueLineItem(
                "order-1",
                OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                "KES",
                new BigDecimal(total),
                1,
                scope,
                COURSE_UUID,
                scope == PurchaseScope.CLASS ? CLASS_UUID : null);
    }

    private static BigDecimal earnings(RevenueDashboardDTO dashboard) {
        return dashboard.estimatedEarnings().stream()
                .filter(amount -> "KES".equals(amount.currencyCode()))
                .map(RevenueAmountDTO::amount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private void stubCreatorLines(List<CommerceRevenueLineItem> lines, Map<UUID, RevenueShare> shares) {
        when(domainSecurityService.hasAnyDomain(UserDomain.course_creator)).thenReturn(true);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(USER_UUID);
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(CREATOR_UUID));
        when(courseInfoService.findCourseUuidsByCourseCreatorUuid(CREATOR_UUID))
                .thenReturn(List.of(COURSE_UUID));
        when(revenueQueryService.findCapturedRevenueLinesByCourseUuids(any(), any(), anyList()))
                .thenReturn(lines);
        lenient().when(courseInfoService.getRevenueShares(anyList())).thenReturn(shares);
    }

    private void stubInstructorLines(List<CommerceRevenueLineItem> lines, Map<UUID, RevenueShare> shares) {
        when(domainSecurityService.hasAnyDomain(UserDomain.instructor)).thenReturn(true);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(USER_UUID);
        when(instructorLookupService.findInstructorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(INSTRUCTOR_UUID));
        when(classDefinitionLookupService.findClassDefinitionUuidsByInstructorUuid(INSTRUCTOR_UUID))
                .thenReturn(List.of(CLASS_UUID));
        when(revenueQueryService.findCapturedRevenueLinesByClassDefinitionUuids(any(), any(), anyList()))
                .thenReturn(lines);
        lenient().when(courseInfoService.getRevenueShares(anyList())).thenReturn(shares);
    }

    @Nested
    @DisplayName("scope filtering")
    class ScopeFiltering {

        @Test
        @DisplayName("creator earns nothing on a class sale, because only the instructor is credited")
        void creatorEarningsExcludeClassScopeSales() {
            stubCreatorLines(
                    List.of(line(PurchaseScope.CLASS, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.course_creator, START, END);

            // The listener credits the class' instructor 200.00 and the creator nothing. Before the
            // fix this reported 700.00 of income the creator's wallet would never receive.
            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
            // The sale is still visible as gross turnover on the creator's course.
            assertThat(dashboard.grossTotals())
                    .extracting(RevenueAmountDTO::amount)
                    .containsExactly(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("creator still earns the creator share on a course sale")
        void creatorEarningsIncludeCourseScopeSales() {
            stubCreatorLines(
                    List.of(line(PurchaseScope.COURSE, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.course_creator, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(new BigDecimal("700"));
        }

        @Test
        @DisplayName("a mixed period reports only the course-scope share to the creator")
        void creatorEarningsOnMixedScopesCountOnlyCourseSales() {
            stubCreatorLines(
                    List.of(line(PurchaseScope.COURSE, "1000.00"), line(PurchaseScope.CLASS, "500.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.course_creator, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(new BigDecimal("700"));
            assertThat(dashboard.grossTotals())
                    .extracting(RevenueAmountDTO::amount)
                    .containsExactly(new BigDecimal("1500.00"));

            RevenueScopeBreakdownDTO classBreakdown = dashboard.scopeBreakdown().stream()
                    .filter(breakdown -> PurchaseScope.CLASS.name().equals(breakdown.scope()))
                    .findFirst()
                    .orElseThrow();
            assertThat(classBreakdown.estimatedEarnings())
                    .extracting(RevenueAmountDTO::amount)
                    .containsExactly(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("instructor earns nothing on a course sale")
        void instructorEarningsExcludeCourseScopeSales() {
            stubInstructorLines(
                    List.of(line(PurchaseScope.COURSE, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.instructor, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("instructor earns the instructor share on a class sale")
        void instructorEarningsIncludeClassScopeSales() {
            stubInstructorLines(
                    List.of(line(PurchaseScope.CLASS, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.instructor, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(new BigDecimal("200"));
        }

        @Test
        @DisplayName("a creator and an instructor never both report the same sale")
        void theSameClassSaleIsNotReportedToBothParties() {
            stubCreatorLines(
                    List.of(line(PurchaseScope.CLASS, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
            BigDecimal creatorEarnings =
                    earnings(service.getRevenueDashboard(UserDomain.course_creator, START, END));

            assertThat(creatorEarnings).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("missing revenue share")
    class MissingRevenueShare {

        @Test
        @DisplayName("no configured split reports zero to the creator, not the whole sale")
        void nullShareYieldsZeroForCreator() {
            stubCreatorLines(List.of(line(PurchaseScope.COURSE, "1000.00")), Map.of());

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.course_creator, START, END);

            // "Not configured" is not "you get all of it": the listener credits nothing here.
            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.grossTotals())
                    .extracting(RevenueAmountDTO::amount)
                    .containsExactly(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("no configured split reports zero to the instructor, not the whole sale")
        void nullShareYieldsZeroForInstructor() {
            stubInstructorLines(List.of(line(PurchaseScope.CLASS, "1000.00")), Map.of());

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.instructor, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("a null creator percentage on an existing split reports zero")
        void nullCreatorPercentageYieldsZero() {
            stubCreatorLines(
                    List.of(line(PurchaseScope.COURSE, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(null, new BigDecimal("20"))));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.course_creator, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("a null instructor percentage on an existing split reports zero")
        void nullInstructorPercentageYieldsZero() {
            stubInstructorLines(
                    List.of(line(PurchaseScope.CLASS, "1000.00")),
                    Map.of(COURSE_UUID, new RevenueShare(new BigDecimal("70"), null)));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.instructor, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("non-earning domains")
    class NonEarningDomains {

        @Test
        @DisplayName("a student still sees the full amount they spent")
        void studentSpendIsNotSharedDown() {
            UUID studentUuid = UUID.randomUUID();
            when(domainSecurityService.hasAnyDomain(UserDomain.student)).thenReturn(true);
            when(domainSecurityService.getCurrentUserUuid()).thenReturn(USER_UUID);
            when(studentLookupService.findStudentUuidByUserUuid(USER_UUID)).thenReturn(Optional.of(studentUuid));
            when(revenueQueryService.findCapturedRevenueLinesByStudentUuids(any(), any(), anyList()))
                    .thenReturn(List.of(line(PurchaseScope.CLASS, "1000.00")));

            RevenueDashboardDTO dashboard = service.getRevenueDashboard(UserDomain.student, START, END);

            assertThat(earnings(dashboard)).isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }
}
