package apps.sarafrika.elimika.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.service.impl.CommercePurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the per-line settlement columns against a real PostgreSQL instance.
 * <p>
 * Three claims can only be made once the migration and the entity model are in the same room. That
 * the columns the entity maps actually exist as the migration wrote them (the application runs with
 * {@code ddl-auto: validate}, so a mismatch is a startup failure, not a test failure). That the
 * database itself refuses a set of figures that does not reconcile, so a purchase can always be told
 * as gross = fee + credited + retained. And that historical rows are allowed to carry no settlement
 * at all - the columns are nullable and nothing is backfilled, because those wallets were credited
 * on gross and are not being clawed back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CommercePurchaseSettlementRecorder.class, JpaConfig.class})
@DisplayName("Purchase line settlement")
class PurchaseSettlementIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        // The real migrations build the schema; Hibernate must not touch it.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private CommercePurchaseRepository purchaseRepository;
    @Autowired
    private CommercePurchaseItemRepository purchaseItemRepository;
    @Autowired
    private CommercePurchaseSettlementRecorder recorder;
    @Autowired
    private JdbcTemplate jdbc;

    private String orderId;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from commerce_purchase_item");
        jdbc.update("delete from commerce_purchase");
        orderId = UUID.randomUUID().toString();
    }

    private CommercePurchaseItem recordPurchase(String lineItemId, String lineTotal) {
        CommercePurchase purchase = new CommercePurchase();
        purchase.setOrderId(orderId);
        purchase.setPaymentStatus("CAPTURED");
        purchase.setOrderCurrencyCode("KES");
        purchase.setOrderTotalAmount(new BigDecimal(lineTotal));
        purchase.setOrderCreatedAt(OffsetDateTime.parse("2026-01-15T10:00:00Z"));

        CommercePurchaseItem item = new CommercePurchaseItem();
        item.setPurchase(purchase);
        item.setLineItemId(lineItemId);
        item.setTitle("Advanced Excel Course");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal(lineTotal));
        item.setSubtotal(new BigDecimal(lineTotal));
        item.setTotal(new BigDecimal(lineTotal));
        item.setScope(PurchaseScope.COURSE);
        purchase.getItems().add(item);

        purchaseRepository.saveAndFlush(purchase);
        return item;
    }

    @Test
    @DisplayName("a settled line records gross, fee, credited and retained, and they reconcile")
    void aSettledLineIsStampedAndReconciles() {
        recordPurchase("line-1", "1000.00");

        recorder.recordLineSettlement(new PurchaseLineSettlement(
                orderId, "line-1",
                new BigDecimal("1000.00"), new BigDecimal("10.00"),
                new BigDecimal("693.00"), new BigDecimal("297.00")));

        CommercePurchaseItem stored = purchaseItemRepository
                .findByOrderIdAndLineItemId(orderId, "line-1")
                .orElseThrow();
        assertThat(stored.getPlatformFeeAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(stored.getCreditedAmount()).isEqualByComparingTo(new BigDecimal("693.00"));
        assertThat(stored.getRetainedAmount()).isEqualByComparingTo(new BigDecimal("297.00"));
        assertThat(stored.getPlatformFeeAmount()
                .add(stored.getCreditedAmount())
                .add(stored.getRetainedAmount()))
                .isEqualByComparingTo(stored.getTotal());
    }

    @Test
    @DisplayName("the database refuses figures that do not add up to the line total")
    void figuresThatDoNotReconcileAreRejected() {
        recordPurchase("line-1", "1000.00");

        // Bypasses the Java guard entirely, which is the point: the audit trail is protected by the
        // database, not only by the code path that happens to write it today.
        assertThatThrownBy(() -> jdbc.update(
                "update commerce_purchase_item set platform_fee_amount = 10.00,"
                        + " credited_amount = 700.00, retained_amount = 297.00 where line_item_id = ?",
                "line-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("the database refuses a half-written settlement")
    void aPartiallyWrittenSettlementIsRejected() {
        recordPurchase("line-1", "1000.00");

        // A fee with no credited and no retained figure is indistinguishable from a wrong one.
        assertThatThrownBy(() -> jdbc.update(
                "update commerce_purchase_item set platform_fee_amount = 10.00 where line_item_id = ?",
                "line-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a line settled before this existed carries no settlement, and that is allowed")
    void historicalLinesStayNull() {
        CommercePurchaseItem item = recordPurchase("line-1", "1000.00");

        // No backfill: the columns are nullable precisely so history can say "not recorded" rather
        // than have a figure invented for it.
        assertThat(item.getPlatformFeeAmount()).isNull();
        assertThat(item.getCreditedAmount()).isNull();
        assertThat(item.getRetainedAmount()).isNull();
        Long unsettled = jdbc.queryForObject(
                "select count(*) from commerce_purchase_item where retained_amount is null", Long.class);
        assertThat(unsettled).isOne();
    }

    @Test
    @DisplayName("a line credited to nobody retains everything after the fee")
    void aLineCreditedToNobodyRetainsEverything() {
        recordPurchase("line-1", "1000.00");

        recorder.recordLineSettlement(new PurchaseLineSettlement(
                orderId, "line-1",
                new BigDecimal("1000.00"), new BigDecimal("10.00"),
                BigDecimal.ZERO, new BigDecimal("990.00")));

        assertThat(purchaseItemRepository.findByOrderIdAndLineItemId(orderId, "line-1").orElseThrow()
                .getRetainedAmount()).isEqualByComparingTo(new BigDecimal("990.00"));
    }

    @Test
    @DisplayName("replaying a settlement leaves the same figures")
    void replayingASettlementIsIdempotent() {
        recordPurchase("line-1", "1000.00");
        PurchaseLineSettlement settlement = new PurchaseLineSettlement(
                orderId, "line-1",
                new BigDecimal("1000.00"), new BigDecimal("10.00"),
                new BigDecimal("693.00"), new BigDecimal("297.00"));

        recorder.recordLineSettlement(settlement);
        recorder.recordLineSettlement(settlement);

        assertThat(purchaseItemRepository.findByOrderIdAndLineItemId(orderId, "line-1").orElseThrow()
                .getCreditedAmount()).isEqualByComparingTo(new BigDecimal("693.00"));
    }

    @Test
    @DisplayName("a settlement for a line that was never recorded is raised, not silently dropped")
    void anUnknownLineIsRaised() {
        assertThatThrownBy(() -> recorder.recordLineSettlement(new PurchaseLineSettlement(
                orderId, "line-does-not-exist",
                new BigDecimal("1000.00"), new BigDecimal("10.00"),
                new BigDecimal("693.00"), new BigDecimal("297.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line-does-not-exist");
    }
}
