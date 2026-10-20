package apps.sarafrika.elimika.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceSaleLineItemView;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The admin sales list runs with no status or scope filter; those null filters must bind on PostgreSQL. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@DisplayName("Sales line query")
class SalesLineQueryIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private CommercePurchaseRepository purchaseRepository;
    @Autowired private CommercePurchaseItemRepository purchaseItemRepository;
    @Autowired private JdbcTemplate jdbc;

    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID CLASS_DEFINITION = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-09-11T00:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-10-11T23:59:59Z");

    @BeforeEach
    void setUp() {
        // The line references a student, course and class the query never joins.
        jdbc.execute("SET LOCAL session_replication_role = replica");
        jdbc.update("delete from commerce_purchase_item");
        jdbc.update("delete from commerce_purchase");
        CommercePurchase purchase = new CommercePurchase();
        purchase.setOrderId(UUID.randomUUID().toString());
        purchase.setPaymentStatus("CAPTURED");
        purchase.setOrderCurrencyCode("KES");
        purchase.setOrderTotalAmount(new BigDecimal("1000.00"));
        purchase.setOrderCreatedAt(OffsetDateTime.parse("2026-10-01T10:00:00Z"));
        CommercePurchaseItem item = new CommercePurchaseItem();
        item.setPurchase(purchase);
        item.setLineItemId("line-1");
        item.setTitle("Advanced Excel Course");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setSubtotal(new BigDecimal("1000.00"));
        item.setTotal(new BigDecimal("1000.00"));
        item.setScope(PurchaseScope.COURSE);
        item.setCourseUuid(COURSE);
        item.setClassDefinitionUuid(CLASS_DEFINITION);
        item.setStudentUuid(STUDENT);
        purchase.getItems().add(item);
        purchaseRepository.saveAndFlush(purchase);
    }

    @Test
    @DisplayName("unfiltered admin sales page returns the line")
    void unfilteredSalesPage() {
        Page<CommerceSaleLineItemView> page = purchaseItemRepository.findSales(FROM, TO, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().title()).isEqualTo("Advanced Excel Course");
    }

    @Test
    @DisplayName("course, class and student scoped sales pages bind null filters too")
    void scopedSalesPagesWithoutFilters() {
        PageRequest page = PageRequest.of(0, 20);
        assertThat(purchaseItemRepository.findSalesByCourseUuids(FROM, TO, null, null, List.of(COURSE), page)
                .getTotalElements()).isEqualTo(1);
        assertThat(purchaseItemRepository.findSalesByClassDefinitionUuids(FROM, TO, null, null, List.of(CLASS_DEFINITION), page)
                .getTotalElements()).isEqualTo(1);
        assertThat(purchaseItemRepository.findSalesByStudentUuids(FROM, TO, null, null, List.of(STUDENT), page)
                .getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("status and scope filters still narrow the page")
    void filteredSalesPage() {
        assertThat(purchaseItemRepository.findSales(FROM, TO, "captured", PurchaseScope.COURSE, PageRequest.of(0, 20))
                .getTotalElements()).isEqualTo(1);
        assertThat(purchaseItemRepository.findSales(FROM, TO, "pending", null, PageRequest.of(0, 20))
                .getTotalElements()).isZero();
    }
}
