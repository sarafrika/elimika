package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceRevenueQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceSaleLineItemView;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.wallet.service.SaleRecord;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * One-off correction of class sales settled before creators and hiring organisations were paid: the creator
 * gets what was retained, and a job class's instructor credit moves to the organisation. Dry run by default.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalResettlementService {

    private static final OffsetDateTime BEGINNING = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final int PAGE_SIZE = 100;

    private final CommerceRevenueQueryService revenueQueryService;
    private final WalletService walletService;
    private final CourseInfoService courseInfoService;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final PurchaseSettlementRecorder purchaseSettlementRecorder;

    public enum Action { PAY_CREATOR, MOVE_TO_ORGANISATION_AND_PAY_CREATOR, ALREADY_SETTLED, SKIPPED }

    public record Line(
            @JsonProperty("order_id") String orderId,
            @JsonProperty("line_item_id") String lineItemId,
            @JsonProperty("title") String title,
            @JsonProperty("gross") BigDecimal gross,
            @JsonProperty("earlier_credit") BigDecimal earlierCredit,
            @JsonProperty("creator_credit") BigDecimal creatorCredit,
            @JsonProperty("moved_to_organisation") BigDecimal movedToOrganisation,
            @JsonProperty("action") Action action,
            @JsonProperty("note") String note) { }

    public record Report(
            @JsonProperty("applied") boolean applied,
            @JsonProperty("lines") List<Line> lines,
            @JsonProperty("total_creator_credits") BigDecimal totalCreatorCredits,
            @JsonProperty("total_moved_to_organisations") BigDecimal totalMovedToOrganisations) { }

    public Report run(boolean apply) {
        List<Line> lines = new ArrayList<>();
        int page = 0;
        Page<CommerceSaleLineItemView> sales;
        do {
            sales = revenueQueryService.findSales(BEGINNING, OffsetDateTime.now(ZoneOffset.UTC), "CAPTURED",
                    PurchaseScope.CLASS, PageRequest.of(page++, PAGE_SIZE));
            for (CommerceSaleLineItemView sale : sales.getContent()) {
                lines.add(resettle(sale, apply));
            }
        } while (sales.hasNext());

        BigDecimal creatorCredits = lines.stream().map(Line::creatorCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal moved = lines.stream().map(Line::movedToOrganisation).reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Historical re-settlement {}: {} lines, creators {} , moved to organisations {}",
                apply ? "APPLIED" : "dry run", lines.size(), creatorCredits, moved);
        return new Report(apply, lines, creatorCredits, moved);
    }

    private Line resettle(CommerceSaleLineItemView sale, boolean apply) {
        String reference = sale.orderId() + ":" + sale.lineItemId();
        BigDecimal gross = sale.total();
        if (walletService.findSaleByReference(reference + ":creator").isPresent()) {
            return line(sale, null, BigDecimal.ZERO, BigDecimal.ZERO, Action.ALREADY_SETTLED, "creator already paid");
        }
        Optional<SaleRecord> earlier = walletService.findSaleByReference(reference);
        if (earlier.isEmpty()) {
            return line(sale, null, BigDecimal.ZERO, BigDecimal.ZERO, Action.SKIPPED, "no earlier credit to settle against");
        }
        SaleRecord credit = earlier.get();
        Optional<ClassDefinitionSnapshot> snapshot = classDefinitionLookupService.findByUuid(sale.classDefinitionUuid());
        Optional<UUID> creator = snapshot.map(ClassDefinitionSnapshot::courseUuid)
                .flatMap(courseInfoService::getCourseCreatorUserUuid);
        BigDecimal creatorAmount = gross.subtract(credit.amount());
        if (creator.isEmpty() || creatorAmount.signum() <= 0) {
            return line(sale, credit.amount(), BigDecimal.ZERO, BigDecimal.ZERO, Action.SKIPPED,
                    creator.isEmpty() ? "course creator not found" : "nothing was retained");
        }

        Optional<UUID> hiringOrganisation = classDefinitionLookupService.findHiringOrganisationUuid(sale.classDefinitionUuid());
        boolean move = hiringOrganisation.isPresent()
                && !credit.owner().equals(WalletOwner.organisation(hiringOrganisation.get()));
        String description = "Class sale share - " + snapshot.map(ClassDefinitionSnapshot::title).orElse(sale.title())
                + " (re-settled)";
        if (apply) {
            if (move) {
                walletService.reverseSale(reference, "Moved to the hiring organisation - " + sale.title());
                walletService.settleSaleShare(WalletOwner.organisation(hiringOrganisation.get()), credit.amount(),
                        BigDecimal.ZERO, credit.currencyCode(), reference + ":delivery", description);
            }
            walletService.settleSaleShare(WalletOwner.user(creator.get()), creatorAmount, BigDecimal.ZERO,
                    credit.currencyCode(), reference + ":creator", description);
            purchaseSettlementRecorder.recordLineSettlement(new PurchaseLineSettlement(
                    sale.orderId(), sale.lineItemId(), gross, BigDecimal.ZERO, gross, BigDecimal.ZERO));
        }
        return line(sale, credit.amount(), creatorAmount, move ? credit.amount() : BigDecimal.ZERO,
                move ? Action.MOVE_TO_ORGANISATION_AND_PAY_CREATOR : Action.PAY_CREATOR, null);
    }

    private static Line line(CommerceSaleLineItemView sale, BigDecimal earlierCredit, BigDecimal creatorCredit,
                             BigDecimal moved, Action action, String note) {
        return new Line(sale.orderId(), sale.lineItemId(), sale.title(), sale.total(), earlierCredit, creatorCredit,
                moved, action, note);
    }
}
