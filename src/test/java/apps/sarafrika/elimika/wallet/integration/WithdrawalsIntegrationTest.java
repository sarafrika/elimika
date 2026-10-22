package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.SmsDelivery;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WithdrawalNotifier;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WithdrawalServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Every money path of a withdrawal, with M-Pesa replaced by a gateway the test controls. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class,
        WithdrawalServiceImpl.class, PayoutSettingsService.class, OneTimeCodeService.class, WithdrawalNotifier.class,
        WithdrawalsIntegrationTest.Fakes.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Withdrawals")
class WithdrawalsIntegrationTest {

    private static final String KES = "KES";
    private static final AtomicInteger USER_NO = new AtomicInteger(400_000_000);
    private static final Pattern DIGITS = Pattern.compile("is (\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "withdrawal-test-secret");
        registry.add("otp.resend-cooldown", () -> "PT0S");
        registry.add("otp.max-per-hour", () -> "50");
    }

    /** A gateway whose answers each test sets, and an SMS sender that keeps every message. */
    @TestConfiguration
    static class Fakes {
        static final List<String> SMS = new ArrayList<>();
        static MobileMoneyPayoutGateway.PayoutState submitState = MobileMoneyPayoutGateway.PayoutState.PENDING;
        static MobileMoneyPayoutGateway.PayoutState statusState = MobileMoneyPayoutGateway.PayoutState.SUCCESS;
        static boolean submitThrows;
        static int submits;

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
                    submits++;
                    if (submitThrows) {
                        throw new IllegalStateException("gateway down");
                    }
                    return outcome(submitState);
                }

                @Override
                public Optional<PayoutOutcome> status(String reference) {
                    return Optional.of(outcome(statusState));
                }

                @Override
                public Optional<PayoutFloat> currentFloat() {
                    return Optional.empty();
                }
            };
        }

        static MobileMoneyPayoutGateway.PayoutOutcome outcome(MobileMoneyPayoutGateway.PayoutState state) {
            return new MobileMoneyPayoutGateway.PayoutOutcome("gw-1", state,
                    state == MobileMoneyPayoutGateway.PayoutState.SUCCESS ? "TJE4K9L2QX" : null,
                    state == MobileMoneyPayoutGateway.PayoutState.SUCCESS ? "JANE W KAMAU" : null,
                    state == MobileMoneyPayoutGateway.PayoutState.SUCCESS ? new BigDecimal("15.00") : null,
                    state == MobileMoneyPayoutGateway.PayoutState.FAILED ? "The initiator information is invalid." : null,
                    false, null);
        }
    }

    @Autowired
    private WithdrawalService withdrawals;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private WalletLedgerReconciliationJob reconciliationJob;
    @Autowired
    private PayoutMethodRepository methodRepository;
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PayoutSettingsService settingsService;

    @MockitoBean
    private CurrencyService currencyService;
    @MockitoBean
    private DomainSecurityService domainSecurityService;

    private UUID actor;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        Fakes.SMS.clear();
        Fakes.submitState = MobileMoneyPayoutGateway.PayoutState.PENDING;
        Fakes.statusState = MobileMoneyPayoutGateway.PayoutState.SUCCESS;
        Fakes.submitThrows = false;
        Fakes.submits = 0;
        actAs(UUID.randomUUID(), "Jane Wanjiru Kamau");
    }

    @Test
    @DisplayName("a confirmed withdrawal leaves the balance, is paid by M-Pesa, and the ledger stays balanced")
    void paidEndToEnd() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        PayoutMethod method = method(earner, "254712345678", true);

        WithdrawalService.RequestedWithdrawal requested = withdrawals.request(earner, new BigDecimal("1500"),
                method.getUuid(), "key-1");
        assertThat(requested.withdrawal().getStatus()).isEqualTo(WithdrawalStatus.AWAITING_CONFIRMATION);
        assertThat(balance(earner)).isEqualByComparingTo("5000");

        Withdrawal confirmed = withdrawals.confirm(earner, requested.withdrawal().getUuid(), lastCode());

        assertThat(confirmed.getReviewFlags()).isNull();
        assertThat(balance(earner)).isEqualByComparingTo("3500");
        assertThat(reload(confirmed).getStatus()).isEqualTo(WithdrawalStatus.PROCESSING);
        assertThat(walletService.standingOf(earner, KES, true).heldAmount()).isEqualByComparingTo("1500");

        withdrawals.refresh(confirmed.getUuid());

        Withdrawal paid = reload(confirmed);
        assertThat(paid.getStatus()).isEqualTo(WithdrawalStatus.PAID);
        assertThat(paid.getMpesaReceipt()).isEqualTo("TJE4K9L2QX");
        assertThat(walletService.standingOf(earner, KES, true).heldAmount()).isZero();
        assertThat(methodRepository.findById(method.getId()).orElseThrow().getMpesaRegisteredName()).isEqualTo("JANE W KAMAU");
        assertThat(methodRepository.findById(method.getId()).orElseThrow().isNameMismatch()).isFalse();
        assertThat(rows(earner)).first().extracting(UserWalletTransaction::getTransactionType)
                .isEqualTo(WalletTransactionType.WITHDRAWAL);
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("an M-Pesa failure puts the money back with a reversal row")
    void failedIsReversed() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        PayoutMethod method = method(earner, "254712345678", true);
        Fakes.statusState = MobileMoneyPayoutGateway.PayoutState.FAILED;

        UUID uuid = confirm(earner, method, "2000", "key-2");
        withdrawals.refresh(uuid);

        Withdrawal failed = withdrawalRepository.findByUuid(uuid).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(WithdrawalStatus.FAILED);
        assertThat(failed.getFailureReason()).contains("initiator");
        assertThat(balance(earner)).isEqualByComparingTo("5000");
        assertThat(rows(earner)).first().extracting(UserWalletTransaction::getTransactionType)
                .isEqualTo(WalletTransactionType.WITHDRAWAL_REVERSAL);
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a first payout to a number waits for an admin; rejection refunds, approval pays")
    void reviewRoute() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "9000");
        PayoutMethod method = method(earner, "254722418903", false);

        UUID first = confirm(earner, method, "1000", "key-3");
        assertThat(withdrawalRepository.findByUuid(first).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
        assertThat(withdrawalRepository.findByUuid(first).orElseThrow().getReviewFlags()).contains("FIRST_PAYOUT_TO_NUMBER");
        assertThat(Fakes.submits).isZero();
        assertThatThrownBy(() -> withdrawals.adminReject(first, "no")).isInstanceOf(IllegalArgumentException.class);

        withdrawals.adminReject(first, "The number is not registered to you.");
        assertThat(balance(earner)).isEqualByComparingTo("9000");

        UUID second = confirm(earner, method, "1000", "key-4");
        withdrawals.adminApprove(second);
        assertThat(withdrawalRepository.findByUuid(second).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.PROCESSING);
        assertThat(Fakes.submits).isEqualTo(1);
    }

    @Test
    @DisplayName("limits, available money, whole shillings and the hold on new numbers are enforced")
    void limits() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "2000");
        PayoutMethod method = method(earner, "254712345678", true);

        assertThatThrownBy(() -> withdrawals.request(earner, new BigDecimal("50"), method.getUuid(), "a"))
                .hasMessageContaining("minimum");
        assertThatThrownBy(() -> withdrawals.request(earner, new BigDecimal("2500"), method.getUuid(), "b"))
                .hasMessageContaining("up to KES 2,000");
        assertThatThrownBy(() -> withdrawals.request(earner, new BigDecimal("150.50"), method.getUuid(), "c"))
                .hasMessageContaining("whole number");

        PayoutMethod held = method(earner, "254110552871", true);
        held.setUnlockAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(5));
        methodRepository.save(held);
        assertThatThrownBy(() -> withdrawals.request(earner, new BigDecimal("500"), held.getUuid(), "d"))
                .hasMessageContaining("on hold");
    }

    @Test
    @DisplayName("repeating a request returns the first withdrawal and sends one code")
    void idempotent() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "2000");
        PayoutMethod method = method(earner, "254712345678", true);

        Withdrawal first = withdrawals.request(earner, new BigDecimal("500"), method.getUuid(), "same").withdrawal();
        WithdrawalService.RequestedWithdrawal again = withdrawals.request(earner, new BigDecimal("500"), method.getUuid(), "same");

        assertThat(again.withdrawal().getUuid()).isEqualTo(first.getUuid());
        assertThat(again.verification()).isNull();
        assertThat(Fakes.SMS).hasSize(1);
        assertThatThrownBy(() -> withdrawals.request(earner, new BigDecimal("400"), method.getUuid(), "other"))
                .hasMessageContaining("already in progress");
    }

    @Test
    @DisplayName("a large organisation withdrawal needs a second, different approver")
    void secondApproval() {
        WalletOwner organisation = WalletOwner.organisation(UUID.randomUUID());
        fund(organisation, "200000");
        PayoutMethod method = method(organisation, "254712345678", true);
        UUID requester = actor;

        UUID uuid = confirm(organisation, method, "120000", "org-1");
        assertThat(withdrawalRepository.findByUuid(uuid).orElseThrow().getStatus())
                .isEqualTo(WithdrawalStatus.AWAITING_SECOND_APPROVAL);
        assertThatThrownBy(() -> withdrawals.approveAsSecond(organisation, uuid)).hasMessageContaining("different");

        actAs(UUID.randomUUID(), "Second Approver");
        Withdrawal approved = withdrawals.approveAsSecond(organisation, uuid);

        assertThat(approved.getRequestedByUserUuid()).isEqualTo(requester);
        assertThat(withdrawalRepository.findByUuid(uuid).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
    }

    @Test
    @DisplayName("an unknown gateway result keeps it sending, and an unconfirmed request expires")
    void retriesAndExpiry() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        PayoutMethod method = method(earner, "254712345678", true);
        Fakes.submitThrows = true;

        UUID uuid = confirm(earner, method, "1000", "key-5");
        assertThat(withdrawalRepository.findByUuid(uuid).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.SENDING);

        Fakes.submitThrows = false;
        withdrawals.send(uuid);
        assertThat(withdrawalRepository.findByUuid(uuid).orElseThrow().getStatus()).isEqualTo(WithdrawalStatus.PROCESSING);

        PayoutMethod other = method(earner, "254110552871", true);
        Withdrawal stale = withdrawals.request(earner, new BigDecimal("300"), other.getUuid(), "key-6").withdrawal();
        jdbc.update("update withdrawals set created_date = ? where uuid = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), stale.getUuid());
        withdrawals.expire(stale.getUuid());
        assertThat(reload(stale).getStatus()).isEqualTo(WithdrawalStatus.EXPIRED);
    }

    @Test
    @DisplayName("admins change the rules, inconsistent rules are refused, and new withdrawals follow them")
    void settings() {
        assertThatThrownBy(() -> settingsService.update(new BigDecimal("500"), new BigDecimal("100"),
                new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("50"), PayoutFeePayer.PLATFORM,
                BigDecimal.ZERO, "admin")).isInstanceOf(IllegalArgumentException.class);
        try {
            settingsService.update(new BigDecimal("100"), new BigDecimal("150000"), new BigDecimal("300000"),
                    new BigDecimal("800"), new BigDecimal("100000"), PayoutFeePayer.EARNER, new BigDecimal("30"), "admin");
            WalletOwner earner = WalletOwner.user(user());
            fund(earner, "5000");
            PayoutMethod method = method(earner, "254712345678", true);

            UUID uuid = confirm(earner, method, "1000", "rules-1");

            Withdrawal withdrawal = withdrawalRepository.findByUuid(uuid).orElseThrow();
            assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
            assertThat(withdrawal.getReviewFlags()).contains("ABOVE_REVIEW_LIMIT");
            assertThat(withdrawal.getNetAmount()).isEqualByComparingTo("970");
            assertThat(balance(earner)).isEqualByComparingTo("4000");
            withdrawals.adminReject(uuid, "Testing the earner fee path end to end.");
            assertThat(balance(earner)).isEqualByComparingTo("5000");
            assertThat(reconciliationJob.runOnce().isClean()).isTrue();
        } finally {
            settingsService.update(new BigDecimal("100"), new BigDecimal("150000"), new BigDecimal("300000"),
                    new BigDecimal("50000"), new BigDecimal("100000"), PayoutFeePayer.PLATFORM, BigDecimal.ZERO, "admin");
        }
    }

    private UUID confirm(WalletOwner owner, PayoutMethod method, String amount, String key) {
        Withdrawal requested = withdrawals.request(owner, new BigDecimal(amount), method.getUuid(), key).withdrawal();
        return withdrawals.confirm(owner, requested.getUuid(), lastCode()).getUuid();
    }

    private void actAs(UUID user, String name) {
        actor = user;
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user);
        when(domainSecurityService.getCurrentCallerContact())
                .thenReturn(Optional.of(new DomainSecurityService.CallerContact(user + "@example.com", name)));
    }

    private void fund(WalletOwner owner, String amount) {
        walletService.settleSaleShare(owner, new BigDecimal(amount), BigDecimal.ZERO, KES,
                "sale-" + UUID.randomUUID(), "Class sale share");
    }

    /** A verified, unlocked number; {@code paidBefore} adds an earlier paid withdrawal so it skips review. */
    private PayoutMethod method(WalletOwner owner, String msisdn, boolean paidBefore) {
        PayoutMethod method = new PayoutMethod();
        method.setOwnerType(owner.type());
        method.setOwnerUuid(owner.uuid());
        method.setMethodType(PayoutMethodType.MPESA);
        method.setMsisdn(msisdn);
        method.setStatus(PayoutMethodStatus.VERIFIED);
        method.setVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3));
        method.setUnlockAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        method = methodRepository.saveAndFlush(method);
        if (paidBefore) {
            jdbc.update("insert into withdrawals (reference, owner_type, owner_uuid, currency_code, amount, fee_amount, "
                            + "net_amount, payout_method_uuid, msisdn, status, idempotency_key, created_by) "
                            + "values (?, ?, ?, 'KES', 100, 0, 100, ?, ?, 'PAID', ?, 'TEST')",
                    "WD-OLD-" + USER_NO.incrementAndGet(), owner.type().name(), owner.uuid(), method.getUuid(), msisdn,
                    "old-" + UUID.randomUUID());
        }
        return method;
    }

    private BigDecimal balance(WalletOwner owner) {
        return walletService.balanceOf(owner, KES).balanceAmount();
    }

    private List<UserWalletTransaction> rows(WalletOwner owner) {
        return walletService.statementOf(owner, KES, PageRequest.of(0, 20)).getContent();
    }

    private Withdrawal reload(Withdrawal withdrawal) {
        return withdrawalRepository.findByUuid(withdrawal.getUuid()).orElseThrow();
    }

    private UUID user() {
        UUID uuid = UUID.randomUUID();
        String userNo = String.valueOf(USER_NO.incrementAndGet());
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
