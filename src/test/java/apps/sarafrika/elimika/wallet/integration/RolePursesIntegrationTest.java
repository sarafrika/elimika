package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.controller.AdminPayoutBatchController;
import apps.sarafrika.elimika.wallet.controller.AdminPayoutController;
import apps.sarafrika.elimika.wallet.controller.WithdrawalController;
import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutBatchSender;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WalletCheck;
import apps.sarafrika.elimika.wallet.service.PurseTransferResult;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WithdrawalNotifier;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import apps.sarafrika.elimika.wallet.service.WithdrawalWalletChecks;
import apps.sarafrika.elimika.wallet.service.impl.PayoutBatchServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WithdrawalServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WithdrawalWalletChecksImpl;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One person, several purses: each is withdrawn from, checked, reversed into and moved between on its own, and the
 * daily limit still counts the person, not the purse.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class,
        WithdrawalServiceImpl.class, PayoutSettingsService.class, OneTimeCodeService.class, WithdrawalNotifier.class,
        PayoutBatchServiceImpl.class, PayoutBatchSender.class, WithdrawalWalletChecksImpl.class,
        RolePursesIntegrationTest.Fakes.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Role purses")
class RolePursesIntegrationTest {

    private static final String KES = "KES";
    private static final AtomicInteger SEQ = new AtomicInteger(800_000_000);
    private static final Pattern DIGITS = Pattern.compile("is (\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "role-purse-secret");
        registry.add("otp.resend-cooldown", () -> "PT0S");
        registry.add("otp.max-per-hour", () -> "500");
    }

    @TestConfiguration
    static class Fakes {
        static final List<String> SMS = new ArrayList<>();
        static volatile boolean failPayouts;

        @Bean
        SmsSender smsSender() {
            return (msisdn, message) -> {
                SMS.add(message);
                return SmsDelivery.accepted("m");
            };
        }

        @Bean
        MobileMoneyPayoutGateway gateway() {
            return new MobileMoneyPayoutGateway() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public PayoutOutcome submit(String reference, String msisdn, BigDecimal amount, String remarks) {
                    return failPayouts
                            ? new PayoutOutcome("gw-" + UUID.randomUUID(), PayoutState.FAILED, null, null, null,
                            "Recipient unknown", false, null)
                            : new PayoutOutcome("gw-" + UUID.randomUUID(), PayoutState.SUCCESS, "RCPT" + SEQ.get(),
                            "JANE TESTER", BigDecimal.ZERO, null, false, null);
                }

                @Override
                public Optional<PayoutOutcome> status(String reference) {
                    return Optional.empty();
                }

                @Override
                public Optional<PayoutFloat> currentFloat() {
                    return Optional.of(new PayoutFloat(new BigDecimal("10000000"), BigDecimal.ZERO,
                            OffsetDateTime.now(ZoneOffset.UTC)));
                }
            };
        }
    }

    @Autowired
    private WithdrawalService withdrawals;
    @Autowired
    private WithdrawalWalletChecks walletChecks;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private WalletLedgerReconciliationJob reconciliation;
    @Autowired
    private apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository methodRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrencyService currencyService;
    @MockitoBean
    private DomainSecurityService domainSecurityService;

    private WalletOwner person;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        Fakes.SMS.clear();
        Fakes.failPayouts = false;
        jdbc.update("update wallet_payout_settings set require_admin_approval = false, fee_payer = 'PLATFORM', "
                + "earner_fee = 0, min_amount = 100, max_amount = 150000, daily_limit = 300000, review_limit = 50000");
        person = WalletOwner.user(user());
        actAs(person.uuid(), "Jane Tester");
        pay(WalletPurse.COURSE_CREATOR, "6000", "600");
        pay(WalletPurse.INSTRUCTOR, "4000", "400");
    }

    @Test
    @DisplayName("a withdrawal from the creator purse holds and pays from it alone, and its check reads that purse")
    void withdrawFromChosenPurse() {
        PayoutMethod method = method(person);
        Withdrawal requested = withdrawals.request(person, WalletPurse.COURSE_CREATOR, new BigDecimal("2000"),
                method.getUuid(), "key-" + UUID.randomUUID()).withdrawal();
        assertThat(requested.getPurse()).isEqualTo(WalletPurse.COURSE_CREATOR);

        jdbc.update("update wallet_payout_settings set require_admin_approval = true");
        Withdrawal held = withdrawals.confirm(person, requested.getUuid(), lastCode());

        assertThat(purse(WalletPurse.COURSE_CREATOR)).isEqualByComparingTo("3400.00");
        assertThat(purse(WalletPurse.INSTRUCTOR)).isEqualByComparingTo("3600.00");
        WalletCheck check = walletChecks.check(List.of(held)).get(held.getUuid());
        assertThat(check.purse()).isEqualTo(WalletPurse.COURSE_CREATOR);
        assertThat(check.availableBalance()).isEqualByComparingTo("3400.00");
        assertThat(check.heldAmount()).isEqualByComparingTo("2000");
        assertThat(check.accountBalance()).isEqualByComparingTo("5400.00");
        assertThat(check.ledgerBalance()).isEqualByComparingTo("3400.00");
        assertThat(check.ledgerMatches()).isTrue();
        assertThat(check.covers()).isTrue();
        assertThat(walletService.pursesOf(person, KES)).filteredOn(p -> p.purse() == WalletPurse.COURSE_CREATOR)
                .singleElement().satisfies(p -> {
                    assertThat(p.heldAmount()).isEqualByComparingTo("2000");
                    assertThat(p.accountBalance()).isEqualByComparingTo("5400.00");
                });

        // Rejected: the money goes back to the purse it left.
        withdrawals.adminReject(held.getUuid(), "The number belongs to someone else.");
        assertThat(purse(WalletPurse.COURSE_CREATOR)).isEqualByComparingTo("5400.00");
        assertThat(purse(WalletPurse.INSTRUCTOR)).isEqualByComparingTo("3600.00");
        assertThat(jdbc.queryForObject("select w.purse from user_wallet_transactions t join user_wallets w "
                + "on w.id = t.wallet_id where t.reference = ?", String.class, held.getReference() + ":reversal"))
                .isEqualTo("COURSE_CREATOR");
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a paid withdrawal leaves its purse and the ledger mirrors every purse")
    void paidFromItsPurse() {
        UUID uuid = confirm(WalletPurse.INSTRUCTOR, "1000");
        withdrawals.send(uuid);

        assertThat(withdrawalRepository.findByUuid(uuid).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.PAID);
        assertThat(purse(WalletPurse.INSTRUCTOR)).isEqualByComparingTo("2600.00");
        assertThat(purse(WalletPurse.COURSE_CREATOR)).isEqualByComparingTo("5400.00");
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a purse cannot be overdrawn, even when the person holds enough over all purses")
    void purseCannotBeOverdrawn() {
        PayoutMethod method = method(person);
        assertThatThrownBy(() -> withdrawals.request(person, WalletPurse.INSTRUCTOR, new BigDecimal("5000"),
                method.getUuid(), "key-" + UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You can withdraw up to KES 3,600 from this purse.");
        assertThatThrownBy(() -> walletService.transferBetweenPurses(person, WalletPurse.INSTRUCTOR,
                WalletPurse.STUDENT, new BigDecimal("3600.01"), KES, null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Insufficient");
        assertThatThrownBy(() -> walletService.holdWithdrawal(person, WalletPurse.STUDENT, new BigDecimal("1"), KES,
                "WD-X", "x")).isInstanceOf(IllegalStateException.class);
        assertThat(purse(WalletPurse.INSTRUCTOR)).isEqualByComparingTo("3600.00");
    }

    @Test
    @DisplayName("with two funded purses a person must say which; with one it is the default; a purse must fit the owner")
    void defaultPurse() {
        PayoutMethod method = method(person);
        assertThatThrownBy(() -> withdrawals.request(person, null, new BigDecimal("500"), method.getUuid(), "k1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Choose a purse: COURSE_CREATOR holds 5400, INSTRUCTOR holds 3600");

        WalletOwner organisation = WalletOwner.organisation(UUID.randomUUID());
        walletService.settleSaleShare(organisation, WalletPurse.EARNINGS, new BigDecimal("1000"), BigDecimal.ZERO,
                KES, "org-" + UUID.randomUUID(), "Class sale share");
        assertThat(walletService.debitPurse(organisation, null, KES)).isEqualTo(WalletPurse.EARNINGS);
        assertThatThrownBy(() -> walletService.debitPurse(organisation, WalletPurse.INSTRUCTOR, KES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.settleSaleShare(person, WalletPurse.EARNINGS, BigDecimal.TEN,
                BigDecimal.ZERO, KES, "x-" + UUID.randomUUID(), "x")).isInstanceOf(IllegalArgumentException.class);

        WalletOwner single = WalletOwner.user(user());
        walletService.settleSaleShare(single, WalletPurse.INSTRUCTOR, new BigDecimal("800"), BigDecimal.ZERO, KES,
                "s-" + UUID.randomUUID(), "Class sale share");
        assertThat(walletService.debitPurse(single, null, KES)).isEqualTo(WalletPurse.INSTRUCTOR);
    }

    @Test
    @DisplayName("the daily limit counts the person across purses, so splitting a day's withdrawals does not lift it")
    void dailyLimitAcrossPurses() {
        jdbc.update("update wallet_payout_settings set max_amount = 3000, daily_limit = 4000");
        confirm(WalletPurse.COURSE_CREATOR, "3000");
        PayoutMethod method = method(person);
        assertThatThrownBy(() -> withdrawals.request(person, WalletPurse.INSTRUCTOR, new BigDecimal("1500"),
                method.getUuid(), "key-" + UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("today's limit of KES 4,000")
                .hasMessageContaining("You can still withdraw KES 1,000 today.");
    }

    @Test
    @DisplayName("money moves between a person's own purses with no fee, balanced in the ledger and shown on both")
    void transferBetweenOwnPurses() {
        PurseTransferResult moved = walletService.transferBetweenPurses(person, WalletPurse.COURSE_CREATOR,
                WalletPurse.STUDENT, new BigDecimal("1500.00"), KES, null, null);

        assertThat(purse(WalletPurse.COURSE_CREATOR)).isEqualByComparingTo("3900.00");
        assertThat(purse(WalletPurse.STUDENT)).isEqualByComparingTo("1500.00");
        assertThat(walletService.balanceOf(person, KES).balanceAmount()).isEqualByComparingTo("9000.00");
        assertThat(jdbc.queryForList("select t.transaction_type || ' ' || w.purse from user_wallet_transactions t "
                        + "join user_wallets w on w.id = t.wallet_id where t.transfer_reference = ? order by t.id",
                String.class, moved.transferReference()))
                .containsExactly("TRANSFER_OUT COURSE_CREATOR", "TRANSFER_IN STUDENT");
        assertThat(jdbc.queryForObject("select count(*) from ledger_transactions where idempotency_key = ?",
                Long.class, "wallet-transfer:" + moved.transferReference())).isOne();
        // A move between one's own purses is not money sent to someone else.
        assertThat(walletService.totalsOf(person, KES).transferredOutTotal()).isEqualByComparingTo("0");
        assertThat(walletService.monthlyStatementsOf(person, KES, 1).getFirst().moneyIn())
                .isEqualByComparingTo("10000.00");
        assertThat(walletService.monthlyStatementsOf(person, WalletPurse.STUDENT, KES, 1).getFirst().moneyIn())
                .isEqualByComparingTo("1500.00");
        assertThat(jdbc.queryForObject("select count(*) from wallet_audit_events where owner_uuid = ? "
                + "and event_type = 'TRANSFER_SENT' and updated_values->>'purse' = 'COURSE_CREATOR'", Long.class,
                person.uuid())).isOne();
        assertThatThrownBy(() -> walletService.transferBetweenPurses(person, WalletPurse.STUDENT, WalletPurse.STUDENT,
                BigDecimal.ONE, KES, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a transfer to someone else leaves the chosen purse and lands in their STUDENT purse")
    void transferToAnotherPerson() {
        UUID friend = user();
        walletService.transfer(person.uuid(), WalletPurse.INSTRUCTOR, friend, new BigDecimal("600.00"), KES,
                "gift-" + UUID.randomUUID(), "Gift");

        assertThat(purse(WalletPurse.INSTRUCTOR)).isEqualByComparingTo("3000.00");
        assertThat(walletService.balanceOf(WalletOwner.user(friend), WalletPurse.STUDENT, KES).balanceAmount())
                .isEqualByComparingTo("600.00");
        assertThat(walletService.totalsOf(person, KES).transferredOutTotal()).isEqualByComparingTo("600.00");
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    private BigDecimal purse(WalletPurse purse) {
        return walletService.balanceOf(person, purse, KES).balanceAmount();
    }

    private void pay(WalletPurse purse, String gross, String fee) {
        walletService.settleSaleShare(person, purse, new BigDecimal(gross), new BigDecimal(fee), KES,
                "sale-" + UUID.randomUUID() + (purse == WalletPurse.COURSE_CREATOR ? ":creator" : ":delivery"),
                "Class sale share");
    }

    private UUID confirm(WalletPurse purse, String amount) {
        Withdrawal requested = withdrawals.request(person, purse, new BigDecimal(amount), method(person).getUuid(),
                "key-" + UUID.randomUUID()).withdrawal();
        return withdrawals.confirm(person, requested.getUuid(), lastCode()).getUuid();
    }

    private void actAs(UUID user, String name) {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user);
        when(domainSecurityService.getCurrentCallerContact())
                .thenReturn(Optional.of(new DomainSecurityService.CallerContact(user + "@example.com", name)));
    }

    /** A verified, unlocked number with an earlier paid withdrawal, so nothing flags it for review. */
    private PayoutMethod method(WalletOwner owner) {
        String msisdn = "2547" + String.format("%08d", SEQ.incrementAndGet() % 100_000_000);
        PayoutMethod method = new PayoutMethod();
        method.setOwnerType(owner.type());
        method.setOwnerUuid(owner.uuid());
        method.setMethodType(PayoutMethodType.MPESA);
        method.setMsisdn(msisdn);
        method.setStatus(PayoutMethodStatus.VERIFIED);
        method.setVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3));
        method.setUnlockAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        method = methodRepository.saveAndFlush(method);
        jdbc.update("insert into withdrawals (reference, owner_type, owner_uuid, currency_code, amount, fee_amount, "
                        + "net_amount, payout_method_uuid, msisdn, status, idempotency_key, created_by, purse, created_date) "
                        + "values (?, ?, ?, 'KES', 100, 0, 100, ?, ?, 'PAID', ?, 'TEST', 'INSTRUCTOR', now() - interval '3 days')",
                "WD-OLD-" + SEQ.incrementAndGet(), owner.type().name(), owner.uuid(), method.getUuid(), msisdn,
                "old-" + UUID.randomUUID());
        return method;
    }

    private UUID user() {
        UUID uuid = UUID.randomUUID();
        String userNo = String.valueOf(SEQ.incrementAndGet());
        jdbc.update("insert into users (uuid, first_name, last_name, email, user_no, created_by) "
                + "values (?, 'Jane', 'Tester', ?, ?, 'TEST')", uuid, "jane-" + userNo + "@test.local", userNo);
        return uuid;
    }

    private static String lastCode() {
        Matcher matcher = DIGITS.matcher(Fakes.SMS.getLast());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
