package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.LedgerAccountType;
import apps.sarafrika.elimika.wallet.enums.LedgerOwnerType;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerService;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
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

/**
 * A sale share lands in the earner's wallet as two statement rows - the share, then the platform fee
 * on it - for users and organisations alike, and both are mirrored into the ledger.
 */
// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Sale share settlement")
class SaleShareSettlementIntegrationTest {

    private static final String KES = "KES";
    private static final AtomicInteger USER_NO = new AtomicInteger(200_000_000);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private WalletLedgerReconciliationJob reconciliationJob;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
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

    @Test
    @DisplayName("a creator's share shows as the share and then the fee, leaving the net in the wallet")
    void userShareAndFee() {
        WalletOwner creator = WalletOwner.user(user("Wanjiru"));

        boolean settled = walletService.settleSaleShare(creator, new BigDecimal("15000.00"), new BigDecimal("1500.00"),
                KES, "order-1:line-1:creator", "Class sale share - Flower Gardening");

        assertThat(settled).isTrue();
        assertThat(walletService.balanceOf(creator, KES).balanceAmount()).isEqualByComparingTo("13500.00");
        List<UserWalletTransaction> rows = statement(creator);
        assertThat(rows).extracting(UserWalletTransaction::getTransactionType)
                .containsExactly(WalletTransactionType.PLATFORM_FEE, WalletTransactionType.SALE);
        assertThat(rows.get(1).getAmount()).isEqualByComparingTo("15000.00");
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo("1500.00");
        assertThat(rows.get(0).getBalanceAfter()).isEqualByComparingTo("13500.00");

        assertThat(ledgerService.derivedBalance(earnings(LedgerOwnerType.USER, creator.uuid())))
                .isEqualByComparingTo("13500.00");
        assertThat(ledgerService.derivedBalance(platformFeeRevenue())).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("an organisation holds its own wallet, with no user behind it")
    void organisationWallet() {
        WalletOwner organisation = WalletOwner.organisation(UUID.randomUUID());

        walletService.settleSaleShare(organisation, new BigDecimal("35000.00"), new BigDecimal("3500.00"),
                KES, "order-1:line-1:delivery", "Class sale share - Flower Gardening");

        assertThat(walletService.balanceOf(organisation, KES).balanceAmount()).isEqualByComparingTo("31500.00");
        assertThat(ledgerService.derivedBalance(earnings(LedgerOwnerType.ORGANISATION, organisation.uuid())))
                .isEqualByComparingTo("31500.00");
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a user and an organisation with the same uuid never share a wallet")
    void ownersAreKeptApart() {
        UUID shared = user("Otieno");

        walletService.settleSaleShare(WalletOwner.user(shared), new BigDecimal("100.00"), BigDecimal.ZERO,
                KES, "order-2:line-1:creator", "Share");
        walletService.settleSaleShare(WalletOwner.organisation(shared), new BigDecimal("300.00"), BigDecimal.ZERO,
                KES, "order-2:line-1:delivery", "Share");

        assertThat(walletService.balanceOf(WalletOwner.user(shared), KES).balanceAmount()).isEqualByComparingTo("100.00");
        assertThat(walletService.balanceOf(WalletOwner.organisation(shared), KES).balanceAmount())
                .isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("settling the same share twice pays once")
    void idempotent() {
        WalletOwner instructor = WalletOwner.user(user("Kamau"));

        assertThat(walletService.settleSaleShare(instructor, new BigDecimal("35000.00"), new BigDecimal("3500.00"),
                KES, "order-3:line-1:delivery", "Share")).isTrue();
        assertThat(walletService.settleSaleShare(instructor, new BigDecimal("35000.00"), new BigDecimal("3500.00"),
                KES, "order-3:line-1:delivery", "Share")).isFalse();

        assertThat(walletService.balanceOf(instructor, KES).balanceAmount()).isEqualByComparingTo("31500.00");
        assertThat(statement(instructor)).hasSize(2);
    }

    @Test
    @DisplayName("with no fee only the share row is written")
    void noFeeRow() {
        WalletOwner instructor = WalletOwner.user(user("Achieng"));

        walletService.settleSaleShare(instructor, new BigDecimal("7000.00"), BigDecimal.ZERO,
                KES, "order-4:line-1:delivery", "Share");

        assertThat(statement(instructor)).extracting(UserWalletTransaction::getTransactionType)
                .containsExactly(WalletTransactionType.SALE);
    }

    @Test
    @DisplayName("a wallet's totals are summed over every row, not just one page")
    void totalsCoverTheWholeStatement() {
        WalletOwner creator = WalletOwner.user(user("Mumbi"));
        for (int i = 0; i < 120; i++) {
            walletService.settleSaleShare(creator, new BigDecimal("100.00"), new BigDecimal("10.00"),
                    KES, "order-t" + i + ":line-1:creator", "Share");
        }

        WalletTotals totals = walletService.totalsOf(creator, KES);

        assertThat(totals.sharesTotal()).isEqualByComparingTo("12000.00");
        assertThat(totals.platformFeesTotal()).isEqualByComparingTo("1200.00");
        assertThat(totals.salesCount()).isEqualTo(120);
        assertThat(totals.transferredOutTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a wallet with no rows has zero totals")
    void emptyTotals() {
        assertThat(walletService.totalsOf(WalletOwner.organisation(UUID.randomUUID()), KES).salesCount()).isZero();
    }

    private List<UserWalletTransaction> statement(WalletOwner owner) {
        return walletService.statementOf(owner, KES, PageRequest.of(0, 20)).getContent();
    }

    private UUID earnings(LedgerOwnerType ownerType, UUID ownerUuid) {
        return ledgerService.getOrCreateAccountUuid(new LedgerAccountRef(
                ownerType, ownerUuid, LedgerAccountType.LIABILITY, LedgerPurse.EARNINGS, KES));
    }

    private UUID platformFeeRevenue() {
        return ledgerService.getOrCreateAccountUuid(
                LedgerAccountRef.platform(LedgerAccountType.REVENUE, LedgerPurse.PLATFORM_FEE_REVENUE, KES));
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
