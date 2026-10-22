package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.service.WalletMonthStatement;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletTransactionFilter;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Statements, filtered reads and the audit trail come straight from the wallet's rows, scoped to one owner. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Wallet statements, filters and audit")
class WalletStatementsIntegrationTest {

    private static final String KES = "KES";
    private static final AtomicInteger USER_NO = new AtomicInteger(300_000_000);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrencyService currencyService;

    private final YearMonth current = YearMonth.now(ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        jdbc.update("delete from user_wallet_transactions");
        jdbc.update("delete from user_wallets");
    }

    @Test
    @DisplayName("each month opens where the last closed, and a quiet month carries the balance")
    void monthlyStatements() {
        WalletOwner earner = WalletOwner.user(user("Akinyi"));
        walletService.settleSaleShare(earner, new BigDecimal("1000.00"), new BigDecimal("100.00"),
                KES, "order-1:creator", "Class sale share");
        backdate("order-1:creator%", current.minusMonths(2).atDay(5));
        walletService.settleSaleShare(earner, new BigDecimal("500.00"), BigDecimal.ZERO,
                KES, "order-2:creator", "Class sale share");

        List<WalletMonthStatement> statements = walletService.monthlyStatementsOf(earner, KES, 12);

        assertThat(statements).extracting(WalletMonthStatement::month)
                .containsExactly(current, current.minusMonths(1), current.minusMonths(2));
        assertMonth(statements.get(0), "900.00", "500.00", "0.00", "1400.00", 1);
        assertMonth(statements.get(1), "900.00", "0.00", "0.00", "900.00", 0);
        assertMonth(statements.get(2), "0.00", "1000.00", "100.00", "900.00", 2);
    }

    @Test
    @DisplayName("the window keeps only the latest months, opening from the balance before it")
    void windowedStatements() {
        WalletOwner earner = WalletOwner.organisation(UUID.randomUUID());
        walletService.settleSaleShare(earner, new BigDecimal("700.00"), BigDecimal.ZERO,
                KES, "order-3:delivery", "Class sale share");
        backdate("order-3:delivery", current.minusMonths(5).atDay(1));

        List<WalletMonthStatement> statements = walletService.monthlyStatementsOf(earner, KES, 2);

        assertThat(statements).hasSize(2);
        assertMonth(statements.get(1), "700.00", "0.00", "0.00", "700.00", 0);
    }

    @Test
    @DisplayName("an owner with no wallet has no statements")
    void noWallet() {
        assertThat(walletService.monthlyStatementsOf(WalletOwner.user(UUID.randomUUID()), KES, 12)).isEmpty();
    }

    @Test
    @DisplayName("transactions filter by type and by UTC dates")
    void filters() {
        WalletOwner earner = WalletOwner.user(user("Mutua"));
        walletService.settleSaleShare(earner, new BigDecimal("1000.00"), new BigDecimal("100.00"),
                KES, "order-4:creator", "Class sale share");
        LocalDate day = current.minusMonths(1).atDay(10);
        backdate("order-4:creator%", day);
        walletService.settleSaleShare(earner, new BigDecimal("200.00"), BigDecimal.ZERO,
                KES, "order-5:creator", "Class sale share");

        assertThat(walletService.statementOf(earner, KES, WalletTransactionFilter.of(
                List.of(WalletTransactionType.SALE), null, null), PageRequest.of(0, 20)).getContent())
                .hasSize(2);
        assertThat(walletService.statementOf(earner, KES, WalletTransactionFilter.of(null, day, day),
                PageRequest.of(0, 20)).getContent())
                .extracting(row -> row.getTransactionType())
                .containsExactly(WalletTransactionType.PLATFORM_FEE, WalletTransactionType.SALE);
        assertThat(walletService.exportOf(earner, KES, WalletTransactionFilter.all())).hasSize(3);
    }

    @Test
    @DisplayName("every money movement leaves an audit event with the balance before and after")
    void auditTrail() {
        WalletOwner earner = WalletOwner.user(user("Chebet"));
        walletService.settleSaleShare(earner, new BigDecimal("1000.00"), new BigDecimal("100.00"),
                KES, "order-6:creator", "Class sale share");

        List<WalletAuditEvent> events = walletService.auditOf(earner, PageRequest.of(0, 20)).getContent();

        assertThat(events).extracting(WalletAuditEvent::getEventType)
                .containsExactly(WalletAuditEventType.PLATFORM_FEE_CHARGED, WalletAuditEventType.SALE_SHARE_CREDITED);
        assertThat(events.get(1).getActorUserUuid()).isNull();
        assertThat(new BigDecimal(String.valueOf(events.get(0).getPreviousValues().get("balance_amount"))))
                .isEqualByComparingTo("1000.00");
        assertThat(new BigDecimal(String.valueOf(events.get(0).getUpdatedValues().get("balance_amount"))))
                .isEqualByComparingTo("900.00");
        assertThat(walletService.auditOf(WalletOwner.organisation(earner.uuid()), PageRequest.of(0, 20)))
                .isEmpty();
    }

    @Test
    @DisplayName("the audit trail cannot be edited or deleted")
    void auditIsAppendOnly() {
        WalletOwner earner = WalletOwner.user(user("Kiprop"));
        walletService.settleSaleShare(earner, new BigDecimal("50.00"), BigDecimal.ZERO,
                KES, "order-7:creator", "Class sale share");

        assertThatThrownBy(() -> jdbc.update("update wallet_audit_events set reason = 'x' where owner_uuid = ?",
                earner.uuid())).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from wallet_audit_events where owner_uuid = ?",
                earner.uuid())).hasMessageContaining("append-only");
    }

    private static void assertMonth(WalletMonthStatement statement, String opening, String in, String out,
                                    String closing, long count) {
        assertThat(statement.openingBalance()).isEqualByComparingTo(opening);
        assertThat(statement.moneyIn()).isEqualByComparingTo(in);
        assertThat(statement.moneyOut()).isEqualByComparingTo(out);
        assertThat(statement.closingBalance()).isEqualByComparingTo(closing);
        assertThat(statement.transactionCount()).isEqualTo(count);
    }

    private void backdate(String referencePattern, LocalDate day) {
        jdbc.update("update user_wallet_transactions set created_date = ? where reference like ?",
                day.atTime(9, 0), referencePattern);
    }

    private UUID user(String name) {
        UUID uuid = UUID.randomUUID();
        String userNo = String.valueOf(USER_NO.incrementAndGet());
        jdbc.update("insert into users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "values (?, ?, 'Tester', ?, ?, 'TEST')",
                uuid, name, name + "-" + userNo + "@test.local", userNo);
        return uuid;
    }
}
