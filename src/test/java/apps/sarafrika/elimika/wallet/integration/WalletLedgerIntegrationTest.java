package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerService;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationReport;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The ledger against a real PostgreSQL running the real migrations, in two halves.
 * <p>
 * The first half is the dual-write: a wallet operation through {@link WalletServiceImpl} and the
 * ledger rows it leaves behind. The wallet service commits for real here (no ambient test
 * transaction), which matters - the ledger posting is registered as an after-commit callback, so a
 * rolled-back test transaction would show an empty ledger and prove nothing.
 * <p>
 * The second half goes underneath the Java entirely, writing rows through raw JDBC, because the
 * zero-sum rule lives in a deferred constraint trigger and "deferred" is exactly the part a
 * Java-level test cannot see. Those cases commit explicitly: COMMIT is the moment of truth and
 * nothing earlier is.
 * <p>
 * Both halves share one context and one container deliberately - the suite runs every Testcontainers
 * slice in a single JVM, and a second context here is enough to push it into a heap it does not have.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Wallet ledger")
class WalletLedgerIntegrationTest {

    private static final String KES = "KES";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        // Small enough that the reconciliation sweep has to page rather than read everything once.
        registry.add("wallet.ledger.reconciliation.batch-size", () -> "2");
        registry.add("wallet.ledger.reconciliation.max-batches-per-run", () -> "50");
    }

    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private WalletLedgerReconciliationJob reconciliationJob;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));

        // The ledger tables are append-only by trigger, so a clean slate has to say so out loud.
        jdbc.execute("alter table ledger_entries disable trigger user");
        jdbc.execute("alter table ledger_transactions disable trigger user");
        jdbc.update("delete from ledger_entries");
        jdbc.update("delete from ledger_transactions");
        jdbc.execute("alter table ledger_entries enable trigger user");
        jdbc.execute("alter table ledger_transactions enable trigger user");
        jdbc.update("delete from ledger_account_balances");
        jdbc.update("delete from ledger_accounts where owner_type <> 'PLATFORM'");
        jdbc.update("delete from user_wallet_transactions");
        jdbc.update("delete from user_wallets");
        jdbc.update("delete from users");
    }

    /** {@code users.user_no} is constrained to exactly nine digits, hence the counter. */
    private static final AtomicInteger USER_NO = new AtomicInteger(100_000_000);

    private UUID user(String name) {
        UUID uuid = UUID.randomUUID();
        String userNo = String.valueOf(USER_NO.incrementAndGet());
        jdbc.update("insert into users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "values (?, ?, 'Tester', ?, ?, 'TEST')",
                uuid, name, name + "-" + userNo + "@test.local", userNo);
        return uuid;
    }

    private UUID earningsAccount(UUID userUuid) {
        return ledgerService.getOrCreateAccountUuid(LedgerAccountRef.userEarnings(userUuid, KES));
    }

    private BigDecimal walletBalance(UUID userUuid) {
        return jdbc.queryForObject(
                "select balance_amount from user_wallets where user_uuid = ? and currency_code = ?",
                BigDecimal.class, userUuid, KES);
    }

    private long ledgerTransactionCount() {
        Long count = jdbc.queryForObject("select count(*) from ledger_transactions", Long.class);
        return count == null ? 0 : count;
    }

    private String backfillScript() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:db/migration/V*__backfill_wallet_ledger_opening_balances.sql");
            assertThat(resources).as("opening-balance backfill migration").hasSize(1);
            return new String(FileCopyUtils.copyToByteArray(resources[0].getInputStream()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Test
    @DisplayName("a sale credit writes balanced entries that add up to the wallet delta")
    void creditProducesBalancedEntriesMatchingTheWalletDelta() {
        UUID earner = user("Amina");

        UserWallet wallet = walletService.creditSale(earner, new BigDecimal("700.00"), KES, "order-1:line-1", "Sale");

        assertThat(wallet.getBalanceAmount()).isEqualByComparingTo("700.00");
        assertThat(walletBalance(earner)).isEqualByComparingTo("700.00");

        // Two legs, netting to zero, and the credited side matches the wallet exactly.
        assertThat(ledgerTransactionCount()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "select sum(case when direction = 'DEBIT' then amount else -amount end) from ledger_entries",
                BigDecimal.class)).isEqualByComparingTo("0");

        UUID account = earningsAccount(earner);
        assertThat(ledgerService.derivedBalance(account)).isEqualByComparingTo("700.00");
        assertThat(ledgerService.cachedBalance(account)).isEqualByComparingTo("700.00");

        // The funding leg is the platform's unallocated revenue, drawn down by what was paid out.
        UUID unallocated = ledgerService.getOrCreateAccountUuid(
                LedgerAccountRef.platformUnallocatedRevenue(KES));
        assertThat(ledgerService.derivedBalance(unallocated)).isEqualByComparingTo("-700.00");
    }

    @Test
    @DisplayName("a deposit is funded from platform cash rather than revenue")
    void depositIsFundedFromCash() {
        UUID holder = user("Brian");

        walletService.deposit(holder, new BigDecimal("250.00"), KES, "topup-1", "Top up");

        UUID cash = ledgerService.getOrCreateAccountUuid(LedgerAccountRef.platformCashMpesa(KES));
        assertThat(ledgerService.derivedBalance(cash)).isEqualByComparingTo("250.00");
        assertThat(ledgerService.derivedBalance(earningsAccount(holder))).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("a duplicate sale credit does not double-post to the ledger")
    void duplicateSaleCreditsDoNotDoublePost() {
        UUID earner = user("Cynthia");
        String reference = "order-9:line-1";

        assertThat(walletService.creditSaleIdempotent(earner, new BigDecimal("400.00"), KES, reference, "Sale")).isTrue();
        assertThat(walletService.creditSaleIdempotent(earner, new BigDecimal("400.00"), KES, reference, "Sale")).isFalse();

        assertThat(walletBalance(earner)).isEqualByComparingTo("400.00");
        assertThat(ledgerTransactionCount()).isOne();
        assertThat(ledgerService.derivedBalance(earningsAccount(earner))).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("derived balance equals cached balance after a run of deposits, sales and a transfer")
    void derivedBalanceEqualsCachedBalanceAfterASeriesOfOperations() {
        UUID sender = user("Dorcas");
        UUID recipient = user("Elijah");

        walletService.deposit(sender, new BigDecimal("1000.00"), KES, "topup-2", "Top up");
        walletService.creditSale(sender, new BigDecimal("500.00"), KES, "order-2:line-1", "Sale");
        walletService.transfer(sender, recipient, new BigDecimal("300.00"), KES, "gift-1", "Gift");

        UUID senderAccount = earningsAccount(sender);
        UUID recipientAccount = earningsAccount(recipient);

        assertThat(walletBalance(sender)).isEqualByComparingTo("1200.00");
        assertThat(walletBalance(recipient)).isEqualByComparingTo("300.00");
        assertThat(ledgerService.derivedBalance(senderAccount)).isEqualByComparingTo("1200.00");
        assertThat(ledgerService.derivedBalance(recipientAccount)).isEqualByComparingTo("300.00");
        assertThat(ledgerService.cachedBalance(senderAccount))
                .isEqualByComparingTo(ledgerService.derivedBalance(senderAccount));
        assertThat(ledgerService.cachedBalance(recipientAccount))
                .isEqualByComparingTo(ledgerService.derivedBalance(recipientAccount));

        // Whatever the accounts say individually, the whole ledger nets to zero.
        assertThat(jdbc.queryForObject(
                "select sum(case when direction = 'DEBIT' then amount else -amount end) from ledger_entries",
                BigDecimal.class)).isEqualByComparingTo("0");
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("reconciliation is clean when the dual write worked")
    void reconciliationIsCleanAcrossManyWallets() {
        // More wallets than one batch holds, so the keyset paging is actually walked.
        for (int i = 0; i < 5; i++) {
            walletService.creditSale(user("Earner" + i), new BigDecimal("100.00"), KES, "order-b" + i, "Sale");
        }

        WalletLedgerReconciliationReport report = reconciliationJob.runOnce();

        assertThat(report.scanned()).isEqualTo(5);
        assertThat(report.truncated()).isFalse();
        assertThat(report.divergences()).isEmpty();
    }

    @Test
    @DisplayName("a failed ledger write leaves the wallet intact and shows up in reconciliation")
    void walletSurvivesAFailedLedgerWriteAndReconciliationReportsIt() {
        UUID earner = user("Faith");

        // Break only the ledger. NOT VALID so existing rows are left alone; every new entry insert
        // now fails, which is as close to a real ledger outage as a test can get without mocks.
        jdbc.execute("alter table ledger_entries add constraint tmp_ledger_outage check (false) not valid");
        try {
            assertThatCode(() -> walletService.creditSale(
                    earner, new BigDecimal("700.00"), KES, "order-3:line-1", "Sale"))
                    .doesNotThrowAnyException();
        } finally {
            jdbc.execute("alter table ledger_entries drop constraint tmp_ledger_outage");
        }

        // The earner was paid. That is the point of swallowing the ledger failure.
        assertThat(walletBalance(earner)).isEqualByComparingTo("700.00");
        assertThat(jdbc.queryForObject("select count(*) from user_wallet_transactions", Long.class)).isEqualTo(1L);
        // And the posting rolled back whole - no orphaned transaction row without its entries.
        assertThat(ledgerTransactionCount()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries", Long.class)).isZero();

        // The divergence is not left to be discovered by a customer complaint.
        WalletLedgerReconciliationReport report = reconciliationJob.runOnce();
        assertThat(report.divergences()).hasSize(1);
        assertThat(report.divergences().getFirst().userUuid()).isEqualTo(earner);
        assertThat(report.divergences().getFirst().difference()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("reconciliation notices a wallet edited behind the ledger's back")
    void reconciliationDetectsAnIntroducedDivergence() {
        UUID earner = user("Grace");
        walletService.creditSale(earner, new BigDecimal("500.00"), KES, "order-4:line-1", "Sale");
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();

        jdbc.update("update user_wallets set balance_amount = balance_amount + 250 where user_uuid = ?", earner);

        WalletLedgerReconciliationReport report = reconciliationJob.runOnce();

        assertThat(report.isClean()).isFalse();
        assertThat(report.divergences()).hasSize(1);
        assertThat(report.divergences().getFirst().walletBalance()).isEqualByComparingTo("750.00");
        assertThat(report.divergences().getFirst().ledgerBalance()).isEqualByComparingTo("500.00");
        assertThat(report.divergences().getFirst().difference()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("the opening-balance backfill is idempotent")
    void backfillIsIdempotent() {
        // A wallet that pre-dates the ledger: written straight to the table, no postings behind it.
        UUID legacy = user("Hadija");
        jdbc.update("insert into user_wallets (user_uuid, currency_code, balance_amount, created_by) "
                + "values (?, ?, 850.0000, 'TEST')", legacy, KES);
        UUID zeroBalance = user("Ibrahim");
        jdbc.update("insert into user_wallets (user_uuid, currency_code, balance_amount, created_by) "
                + "values (?, ?, 0, 'TEST')", zeroBalance, KES);

        String script = backfillScript();
        jdbc.execute(script);

        UUID account = earningsAccount(legacy);
        assertThat(ledgerTransactionCount()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries", Long.class)).isEqualTo(2L);
        assertThat(ledgerService.derivedBalance(account)).isEqualByComparingTo("850.0000");
        assertThat(ledgerService.cachedBalance(account)).isEqualByComparingTo("850.0000");
        // A wallet holding nothing gets an account but no opening transaction; there is nothing to open.
        assertThat(jdbc.queryForObject(
                "select count(*) from ledger_accounts where owner_type = 'USER' and owner_uuid = ?",
                Long.class, zeroBalance)).isOne();
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();

        jdbc.execute(script);

        assertThat(ledgerTransactionCount()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries", Long.class)).isEqualTo(2L);
        assertThat(ledgerService.derivedBalance(account)).isEqualByComparingTo("850.0000");
        assertThat(ledgerService.cachedBalance(account)).isEqualByComparingTo("850.0000");
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("the backfill leaves live dual-written balances alone when it is replayed")
    void backfillDoesNotDisturbLiveLedgerActivity() {
        UUID legacy = user("Joyce");
        jdbc.update("insert into user_wallets (user_uuid, currency_code, balance_amount, created_by) "
                + "values (?, ?, 400.0000, 'TEST')", legacy, KES);

        String script = backfillScript();
        jdbc.execute(script);
        walletService.creditSale(legacy, new BigDecimal("100.00"), KES, "order-5:line-1", "Sale");

        assertThat(walletBalance(legacy)).isEqualByComparingTo("500.00");
        assertThat(ledgerService.derivedBalance(earningsAccount(legacy))).isEqualByComparingTo("500.00");

        // Replaying the backfill after live traffic must not re-open the balance nor rewrite the cache.
        jdbc.execute(script);

        assertThat(ledgerTransactionCount()).isEqualTo(2L);
        assertThat(ledgerService.derivedBalance(earningsAccount(legacy))).isEqualByComparingTo("500.00");
        assertThat(ledgerService.cachedBalance(earningsAccount(legacy))).isEqualByComparingTo("500.00");
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("the purse taxonomy stays closed to anything phase 1 did not define")
    void purseValuesAreConstrained() {
        assertThat(LedgerPurse.values()).hasSize(6);
        assertThatThrownBy(() -> jdbc.update(
                "insert into ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, created_by) "
                        + "values ('USER', ?, 'LIABILITY', 'RESTRICTED_SKILLS_FUND', ?, 'TEST')",
                UUID.randomUUID(), KES))
                .hasMessageContaining("chk_ledger_account_purse");
    }

    private record Leg(UUID accountUuid, String direction, String amount) {
    }

    private UUID platformAccount(String purse) {
        return jdbc.queryForObject(
                "select uuid from ledger_accounts where owner_type = 'PLATFORM' and owner_uuid is null "
                        + "and purse = ? and currency_code = ?",
                UUID.class, purse, KES);
    }

    /**
     * Writes one transaction and its legs inside a single JDBC transaction, exactly as Hibernate
     * would: one INSERT per row, then a commit.
     */
    private UUID post(String idempotencyKey, List<Leg> legs) throws SQLException {
        UUID transactionUuid = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "insert into ledger_transactions (uuid, idempotency_key, occurred_at, created_by) "
                                + "values (?, ?, now(), 'TEST')")) {
                    statement.setObject(1, transactionUuid);
                    statement.setString(2, idempotencyKey);
                    statement.executeUpdate();
                }
                for (Leg leg : legs) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "insert into ledger_entries (uuid, transaction_uuid, account_uuid, direction, amount, "
                                    + "currency_code, created_by) values (?, ?, ?, ?, ?, ?, 'TEST')")) {
                        statement.setObject(1, UUID.randomUUID());
                        statement.setObject(2, transactionUuid);
                        statement.setObject(3, leg.accountUuid());
                        statement.setString(4, leg.direction());
                        statement.setBigDecimal(5, new BigDecimal(leg.amount()));
                        statement.setString(6, KES);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
        return transactionUuid;
    }

    private long transactionsWithKey(String idempotencyKey) {
        Long count = jdbc.queryForObject(
                "select count(*) from ledger_transactions where idempotency_key = ?", Long.class, idempotencyKey);
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("the platform's internal accounts are seeded once each")
    void seedsThePlatformInternalAccounts() {
        List<String> purses = jdbc.queryForList(
                "select purse from ledger_accounts where owner_type = 'PLATFORM' and currency_code = ?",
                String.class, KES);

        assertThat(purses).containsExactlyInAnyOrder(
                "PAYOUT_FEES",
                "PAYOUTS_IN_FLIGHT",
                "PLATFORM_CASH_MPESA",
                "PLATFORM_FEE_REVENUE",
                "PLATFORM_UNALLOCATED_REVENUE");
    }

    @Test
    @DisplayName("a second copy of a platform account is refused despite the null owner")
    void refusesDuplicatePlatformAccounts() {
        // A plain UNIQUE over a nullable owner_uuid would allow this, because null is distinct from
        // null. If this ever starts passing, every platform balance silently splits in two.
        assertThatThrownBy(() -> jdbc.update(
                "insert into ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, created_by) "
                        + "values ('PLATFORM', null, 'ASSET', 'PLATFORM_CASH_MPESA', ?, 'TEST')", KES))
                .hasMessageContaining("uq_ledger_account_identity");
    }

    @Test
    @DisplayName("the database rejects an unbalanced transaction")
    void rejectsAnUnbalancedTransaction() {
        UUID cash = platformAccount("PLATFORM_CASH_MPESA");
        UUID revenue = platformAccount("PLATFORM_UNALLOCATED_REVENUE");
        String key = "unbalanced:" + UUID.randomUUID();

        assertThatThrownBy(() -> post(key, List.of(
                new Leg(cash, "DEBIT", "100.0000"),
                new Leg(revenue, "CREDIT", "90.0000"))))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("does not balance");

        // Not merely refused - gone. The whole posting rolled back.
        assertThat(transactionsWithKey(key)).isZero();
    }

    @Test
    @DisplayName("a balanced transaction commits even though its legs arrive one INSERT at a time")
    void acceptsABalancedTransaction() throws SQLException {
        UUID cash = platformAccount("PLATFORM_CASH_MPESA");
        UUID revenue = platformAccount("PLATFORM_UNALLOCATED_REVENUE");
        String key = "balanced:" + UUID.randomUUID();

        UUID transactionUuid = post(key, List.of(
                new Leg(cash, "DEBIT", "100.0000"),
                new Leg(revenue, "CREDIT", "100.0000")));

        // This is what the deferral buys: after the first INSERT the transaction was unbalanced,
        // and an immediate check would have rejected the only way Hibernate can write entries.
        assertThat(transactionsWithKey(key)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries where transaction_uuid = ?",
                Long.class, transactionUuid)).isEqualTo(2L);
    }

    @Test
    @DisplayName("the database rejects a transaction carrying no entries at all")
    void rejectsAnEmptyTransaction() {
        String key = "empty:" + UUID.randomUUID();

        // Vacuously "balanced" - the hole an entries-only trigger would leave wide open.
        assertThatThrownBy(() -> post(key, List.of()))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("needs at least two");

        assertThat(transactionsWithKey(key)).isZero();
    }

    @Test
    @DisplayName("the database rejects a transaction with a single leg")
    void rejectsASingleLeggedTransaction() {
        UUID cash = platformAccount("PLATFORM_CASH_MPESA");
        String key = "single-leg:" + UUID.randomUUID();

        assertThatThrownBy(() -> post(key, List.of(new Leg(cash, "DEBIT", "100.0000"))))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("needs at least two");

        assertThat(transactionsWithKey(key)).isZero();
    }

    @Test
    @DisplayName("posted entries and transactions cannot be edited or deleted")
    void ledgerRowsAreAppendOnly() throws SQLException {
        UUID cash = platformAccount("PLATFORM_CASH_MPESA");
        UUID revenue = platformAccount("PLATFORM_UNALLOCATED_REVENUE");
        String key = "immutable:" + UUID.randomUUID();
        UUID transactionUuid = post(key, List.of(
                new Leg(cash, "DEBIT", "250.0000"),
                new Leg(revenue, "CREDIT", "250.0000")));

        assertThatThrownBy(() -> jdbc.update(
                "update ledger_entries set amount = 1 where transaction_uuid = ?", transactionUuid))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update(
                "delete from ledger_entries where transaction_uuid = ?", transactionUuid))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update(
                "update ledger_transactions set description = 'edited' where uuid = ?", transactionUuid))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update(
                "delete from ledger_transactions where uuid = ?", transactionUuid))
                .hasMessageContaining("append-only");

        assertThat(jdbc.queryForObject("select sum(amount) from ledger_entries where transaction_uuid = ?",
                BigDecimal.class, transactionUuid)).isEqualByComparingTo("500.0000");
    }

    @Test
    @DisplayName("the same idempotency key cannot be posted twice")
    void refusesADuplicateIdempotencyKey() throws SQLException {
        UUID cash = platformAccount("PLATFORM_CASH_MPESA");
        UUID revenue = platformAccount("PLATFORM_UNALLOCATED_REVENUE");
        String key = "duplicate:" + UUID.randomUUID();
        List<Leg> legs = List.of(
                new Leg(cash, "DEBIT", "10.0000"),
                new Leg(revenue, "CREDIT", "10.0000"));

        assertThatCode(() -> post(key, legs)).doesNotThrowAnyException();
        assertThatThrownBy(() -> post(key, legs))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_ledger_transaction_idempotency_key");

        assertThat(transactionsWithKey(key)).isOne();
    }
}
