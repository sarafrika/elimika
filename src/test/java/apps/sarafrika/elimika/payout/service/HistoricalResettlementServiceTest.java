package apps.sarafrika.elimika.payout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.payout.service.HistoricalResettlementService.Action;
import apps.sarafrika.elimika.payout.service.HistoricalResettlementService.Line;
import apps.sarafrika.elimika.payout.service.HistoricalResettlementService.Report;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceRevenueQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceSaleLineItemView;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.wallet.service.SaleRecord;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Past class sales are re-settled by paying the creator what was retained")
class HistoricalResettlementServiceTest {

    @Mock
    private CommerceRevenueQueryService revenueQueryService;
    @Mock
    private WalletService walletService;
    @Mock
    private CourseInfoService courseInfoService;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private PurchaseSettlementRecorder purchaseSettlementRecorder;

    @InjectMocks
    private HistoricalResettlementService service;

    private final UUID classUuid = UUID.randomUUID();
    private final UUID courseUuid = UUID.randomUUID();
    private final UUID creatorUserUuid = UUID.randomUUID();
    private final UUID instructorUserUuid = UUID.randomUUID();
    private final UUID organisationUuid = UUID.randomUUID();

    @Test
    @DisplayName("a dry run reports the creator's missing share and writes nothing")
    void dryRunWritesNothing() {
        givenLine("order-1", "line-1", "50000.00");
        givenPriorCredit("order-1:line-1", WalletOwner.user(instructorUserUuid), "35000.00");
        givenDirectClass();

        Report report = service.run(false);

        Line line = report.lines().getFirst();
        assertThat(line.action()).isEqualTo(Action.PAY_CREATOR);
        assertThat(line.creatorCredit()).isEqualByComparingTo("15000.00");
        assertThat(report.applied()).isFalse();
        verify(walletService, never()).settleSaleShare(any(), any(), any(), any(), any(), any());
        verify(walletService, never()).reverseSale(any(), any());
        verify(purchaseSettlementRecorder, never()).recordLineSettlement(any());
    }

    @Test
    @DisplayName("applying pays the creator the retained amount and records the line as fully credited")
    void applyPaysTheCreator() {
        givenLine("order-1", "line-1", "50000.00");
        givenPriorCredit("order-1:line-1", WalletOwner.user(instructorUserUuid), "35000.00");
        givenDirectClass();

        Report report = service.run(true);

        verify(walletService).settleSaleShare(eq(WalletOwner.user(creatorUserUuid)), money("15000.00"), money("0"),
                eq("KES"), eq("order-1:line-1:creator"), any());
        verify(walletService, never()).reverseSale(any(), any());
        verify(purchaseSettlementRecorder).recordLineSettlement(new PurchaseLineSettlement("order-1", "line-1",
                new BigDecimal("50000.00"), BigDecimal.ZERO, new BigDecimal("50000.00"), BigDecimal.ZERO));
        assertThat(report.totalCreatorCredits()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("a job class moves the instructor's credit to the hiring organisation and pays the creator")
    void jobClassMovesTheCreditToTheOrganisation() {
        givenLine("order-2", "line-1", "144000.00");
        givenPriorCredit("order-2:line-1", WalletOwner.user(instructorUserUuid), "93600.00");
        givenClass();
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.of(organisationUuid));

        Report report = service.run(true);

        assertThat(report.lines().getFirst().action()).isEqualTo(Action.MOVE_TO_ORGANISATION_AND_PAY_CREATOR);
        verify(walletService).reverseSale(eq("order-2:line-1"), any());
        verify(walletService).settleSaleShare(eq(WalletOwner.organisation(organisationUuid)), money("93600.00"),
                money("0"), eq("KES"), eq("order-2:line-1:delivery"), any());
        verify(walletService).settleSaleShare(eq(WalletOwner.user(creatorUserUuid)), money("50400.00"), money("0"),
                eq("KES"), eq("order-2:line-1:creator"), any());
        assertThat(report.totalMovedToOrganisations()).isEqualByComparingTo("93600.00");
    }

    @Test
    @DisplayName("a line already re-settled is reported and left alone")
    void alreadySettled() {
        givenLine("order-3", "line-1", "1000.00");
        when(walletService.findSaleByReference("order-3:line-1:creator"))
                .thenReturn(Optional.of(new SaleRecord(WalletOwner.user(creatorUserUuid), new BigDecimal("300"), "KES")));

        Report report = service.run(true);

        assertThat(report.lines().getFirst().action()).isEqualTo(Action.ALREADY_SETTLED);
        verify(walletService, never()).settleSaleShare(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a line nobody was ever credited for is skipped for a person to look at")
    void noEarlierCredit() {
        givenLine("order-4", "line-1", "1000.00");
        when(walletService.findSaleByReference("order-4:line-1:creator")).thenReturn(Optional.empty());
        when(walletService.findSaleByReference("order-4:line-1")).thenReturn(Optional.empty());

        Report report = service.run(true);

        assertThat(report.lines().getFirst().action()).isEqualTo(Action.SKIPPED);
        assertThat(report.lines().getFirst().note()).contains("no earlier credit");
        verify(walletService, never()).settleSaleShare(any(), any(), any(), any(), any(), any());
    }

    private void givenLine(String orderId, String lineItemId, String total) {
        CommerceSaleLineItemView view = new CommerceSaleLineItemView(orderId, "1", OffsetDateTime.now(), "CAPTURED",
                "KES", new BigDecimal(total), new BigDecimal(total), null, null, null, null, null, lineItemId, null,
                "Flower Gardening", 1, new BigDecimal(total), new BigDecimal(total), new BigDecimal(total),
                PurchaseScope.CLASS, courseUuid, classUuid, null);
        when(revenueQueryService.findSales(any(), any(), eq("CAPTURED"), eq(PurchaseScope.CLASS), any()))
                .thenReturn(new PageImpl<>(List.of(view)));
    }

    private void givenPriorCredit(String reference, WalletOwner owner, String amount) {
        when(walletService.findSaleByReference(reference + ":creator")).thenReturn(Optional.empty());
        when(walletService.findSaleByReference(reference))
                .thenReturn(Optional.of(new SaleRecord(owner, new BigDecimal(amount), "KES")));
    }

    private void givenClass() {
        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(new ClassDefinitionSnapshot(
                classUuid, courseUuid, null, "Flower Gardening", null, null, null, RateBasis.PER_HOUR,
                null, null, null, null, null, null, null)));
        when(courseInfoService.getCourseCreatorUserUuid(courseUuid)).thenReturn(Optional.of(creatorUserUuid));
    }

    private void givenDirectClass() {
        givenClass();
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.empty());
    }

    private static BigDecimal money(String value) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(value)) == 0);
    }
}
