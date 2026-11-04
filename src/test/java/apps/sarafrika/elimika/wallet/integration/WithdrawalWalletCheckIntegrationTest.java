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
import apps.sarafrika.elimika.wallet.service.WalletOwner;
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

/** The wallet check admins see beside each withdrawal they review, and the batch approval it gates. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({apps.sarafrika.elimika.shared.sms.template.SmsTemplateService.class, WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class,
        WithdrawalServiceImpl.class, PayoutSettingsService.class, OneTimeCodeService.class, WithdrawalNotifier.class,
        PayoutBatchServiceImpl.class, PayoutBatchSender.class, WithdrawalWalletChecksImpl.class,
        WithdrawalWalletCheckIntegrationTest.Fakes.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Withdrawal wallet checks")
class WithdrawalWalletCheckIntegrationTest {

    private static final String KES = "KES";
    private static final AtomicInteger SEQ = new AtomicInteger(600_000_000);
    private static final Pattern DIGITS = Pattern.compile("is (\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "wallet-check-secret");
        registry.add("otp.resend-cooldown", () -> "PT0S");
        registry.add("otp.max-per-hour", () -> "500");
    }

    @TestConfiguration
    static class Fakes {
        static final List<String> SMS = new ArrayList<>();

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
                    return new PayoutOutcome("gw-" + UUID.randomUUID(), PayoutState.PENDING, null, null, null, null,
                            false, null);
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
    private PayoutBatchService batches;
    @Autowired
    private WithdrawalWalletChecks walletChecks;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private PayoutSettingsService settingsService;
    @Autowired
    private MobileMoneyPayoutGateway gateway;
    @Autowired
    private apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository methodRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrencyService currencyService;
    @MockitoBean
    private DomainSecurityService domainSecurityService;

    private final UUID maker = UUID.randomUUID();
    private final UUID checker = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        Fakes.SMS.clear();
        jdbc.update("update wallet_payout_settings set require_admin_approval = true, fee_payer = 'PLATFORM', earner_fee = 0");
        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminPayoutController(withdrawals, withdrawalRepository, settingsService, batches, gateway,
                                walletChecks),
                        new AdminPayoutBatchController(batches, domainSecurityService, gateway, walletChecks),
                        new WithdrawalController(withdrawals))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("one held withdrawal: account is available plus held, and after is what remains once it pays")
    void oneHeld() throws Exception {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "189465.50");
        UUID uuid = confirm(earner, method(earner), "100");

        WalletCheck check = checkOf(uuid);
        assertThat(check.accountBalance()).isEqualByComparingTo("189465.50");
        assertThat(check.heldAmount()).isEqualByComparingTo("100");
        assertThat(check.heldCount()).isEqualTo(1);
        assertThat(check.heldDebitedAmount()).isEqualByComparingTo("100");
        assertThat(check.availableBalance()).isEqualByComparingTo("189365.50");
        assertThat(check.availableBalance())
                .isEqualByComparingTo(walletService.standingOf(earner, KES, true).availableAmount());
        assertThat(check.balanceAfter()).isEqualByComparingTo("189365.50");
        assertThat(check.ledgerBalance()).isEqualByComparingTo("189365.50");
        assertThat(check.ledgerMatches()).isTrue();
        assertThat(check.covers()).isTrue();
        assertThat(check.flags()).isEmpty();
        assertThat(check.checkedAt()).isNotNull();

        mvc.perform(get("/api/v1/admin/payouts/withdrawals").param("status", "NEEDS_REVIEW").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + uuid + "')].wallet_check.held_count")
                        .value(contains(1)))
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + uuid + "')].wallet_check.available_balance")
                        .value(contains(189365.5)))
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + uuid + "')].wallet_check.balance_after")
                        .value(contains(189365.5)))
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + uuid + "')].wallet_flags")
                        .value(contains(empty())));
    }

    @Test
    @DisplayName("the wallet debit is the gross amount, the earner's M-Pesa charge included")
    void grossDebit() {
        jdbc.update("update wallet_payout_settings set fee_payer = 'EARNER', earner_fee = 30");
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        UUID uuid = confirm(earner, method(earner), "1000");

        WalletCheck check = checkOf(uuid);
        assertThat(load(uuid).getNetAmount()).isEqualByComparingTo("970");
        assertThat(check.availableBalance()).isEqualByComparingTo("4000");
        assertThat(check.heldAmount()).isEqualByComparingTo("1000");
        assertThat(check.heldDebitedAmount()).isEqualByComparingTo("1000");
        assertThat(check.accountBalance()).isEqualByComparingTo("5000");
        assertThat(check.balanceAfter()).isEqualByComparingTo("4000");
        assertThat(check.covers()).isTrue();
        assertThat(check.ledgerMatches()).isTrue();
    }

    @Test
    @DisplayName("two held withdrawals of one organisation both report the sum held")
    void twoHeldSameOwner() {
        WalletOwner organisation = WalletOwner.organisation(UUID.randomUUID());
        fund(organisation, "10000");
        UUID first = confirm(organisation, method(organisation), "1500");
        UUID second = confirm(organisation, method(organisation), "2500");

        Map<UUID, WalletCheck> checks = walletChecks.check(List.of(load(first), load(second)));
        for (WalletCheck check : checks.values()) {
            assertThat(check.heldAmount()).isEqualByComparingTo("4000");
            assertThat(check.heldCount()).isEqualTo(2);
            assertThat(check.availableBalance()).isEqualByComparingTo("6000");
            assertThat(check.accountBalance()).isEqualByComparingTo("10000");
            assertThat(check.ledgerMatches()).isTrue();
            assertThat(check.flags()).isEmpty();
        }
        assertThat(checks.get(first).balanceAfter()).isEqualByComparingTo("8500");
        assertThat(checks.get(second).balanceAfter()).isEqualByComparingTo("7500");
    }

    @Test
    @DisplayName("a tampered ledger shows LEDGER_MISMATCH and blocks approval until acknowledged")
    void ledgerMismatchBlocksApproval() throws Exception {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        UUID tampered = confirm(earner, method(earner), "1000");
        UUID clean = queued("700");
        String account = jdbc.queryForObject("select uuid::text from ledger_accounts where owner_type = 'USER' "
                + "and owner_uuid = ? and purse = 'INSTRUCTOR' and currency_code = 'KES'", String.class, earner.uuid());
        jdbc.update("update ledger_account_balances set posted_amount = posted_amount + 250 where account_uuid = ?::uuid",
                account);
        try {
            WalletCheck check = checkOf(tampered);
            assertThat(check.ledgerBalance()).isEqualByComparingTo("4250");
            assertThat(check.availableBalance()).isEqualByComparingTo("4000");
            assertThat(check.ledgerMatches()).isFalse();
            assertThat(check.covers()).isTrue();
            assertThat(check.flags()).containsExactly(WalletCheck.LEDGER_MISMATCH);

            actAs(maker, "Maker Admin");
            PayoutBatch batch = batches.submit(batches.create(List.of(tampered, clean)).getUuid());
            assertThat(batches.walletIssues(List.of(batch.getUuid())).get(batch.getUuid()))
                    .isEqualTo(new PayoutBatchService.WalletIssues(2, 1));

            mvc.perform(get("/api/v1/admin/payouts/batches/{uuid}", batch.getUuid()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.batch.wallet_issue_count").value(1))
                    .andExpect(jsonPath("$.data.batch.wallets_checked").value(2))
                    .andExpect(jsonPath("$.data.withdrawals[0].wallet_check.ledger_matches").value(false))
                    .andExpect(jsonPath("$.data.withdrawals[0].wallet_flags").value(contains("LEDGER_MISMATCH")))
                    .andExpect(jsonPath("$.data.withdrawals[0].review_flags").value(empty()))
                    .andExpect(jsonPath("$.data.withdrawals[1].wallet_check.ledger_matches").value(true))
                    .andExpect(jsonPath("$.data.withdrawals[1].wallet_flags").value(empty()));
            mvc.perform(get("/api/v1/admin/payouts/batches").param("status", "AWAITING_APPROVAL").param("size", "200"))
                    .andExpect(jsonPath("$.data.content[?(@.uuid == '" + batch.getUuid() + "')].wallet_issue_count")
                            .value(contains(1)));

            actAs(checker, "Checker Admin");
            assertThatThrownBy(() -> batches.approve(batch.getUuid(), false, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("1 of 2 rows have a wallet issue")
                    .hasMessageContaining("acknowledge_wallet_issues");
            assertThat(batches.get(batch.getUuid()).getStatus()).isEqualTo(PayoutBatchStatus.AWAITING_APPROVAL);
            assertThat(load(tampered).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);

            PayoutBatch released = batches.approve(batch.getUuid(), false, true);
            assertThat(released.getStatus()).isEqualTo(PayoutBatchStatus.RELEASED);
            assertThat(jdbc.queryForObject("select count(*) from wallet_audit_events where event_type = 'PAYOUT_BATCH_APPROVED' "
                            + "and updated_values->>'batch_uuid' = ? and updated_values->>'acknowledge_wallet_issues' = 'true' "
                            + "and updated_values->>'wallet_issue_count' = '1'",
                    Long.class, batch.getUuid().toString())).isEqualTo(2);
            assertThat(jdbc.queryForObject("select updated_values->'wallet_flags'->>0 from wallet_audit_events "
                    + "where event_type = 'PAYOUT_BATCH_APPROVED' and reference = ?", String.class,
                    load(tampered).getReference())).isEqualTo("LEDGER_MISMATCH");
        } finally {
            jdbc.update("update ledger_account_balances set posted_amount = posted_amount - 250 where account_uuid = ?::uuid",
                    account);
            jdbc.update("update withdrawals set payout_batch_uuid = null, status = 'FAILED' where uuid in (?, ?)",
                    tampered, clean);
        }
    }

    @Test
    @DisplayName("a clean batch approves without acknowledging anything, and records no wallet issues")
    void cleanBatchApproves() {
        UUID row = queued("900");
        actAs(maker, "Maker Admin");
        PayoutBatch batch = batches.submit(batches.create(List.of(row)).getUuid());
        actAs(checker, "Checker Admin");
        assertThat(batches.approve(batch.getUuid(), false).getStatus()).isEqualTo(PayoutBatchStatus.RELEASED);
        assertThat(jdbc.queryForObject("select updated_values->>'acknowledge_wallet_issues' from wallet_audit_events "
                + "where event_type = 'PAYOUT_BATCH_APPROVED' and updated_values->>'batch_uuid' = ?", String.class,
                batch.getUuid().toString())).isEqualTo("false");
        jdbc.update("update withdrawals set payout_batch_uuid = null, status = 'FAILED' where uuid = ?", row);
    }

    @Test
    @DisplayName("a held withdrawal the wallet never debited shows WALLET_SHORT")
    void walletShort() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "3000");
        UUID real = confirm(earner, method(earner), "1000");
        PayoutMethod other = method(earner);
        UUID forged = UUID.randomUUID();
        jdbc.update("insert into withdrawals (uuid, reference, owner_type, owner_uuid, currency_code, amount, fee_amount, "
                        + "net_amount, payout_method_uuid, msisdn, status, idempotency_key, created_by, purse) "
                        + "values (?, ?, 'USER', ?, 'KES', 1500, 0, 1500, ?, ?, 'NEEDS_REVIEW', ?, 'TEST', 'INSTRUCTOR')",
                forged, "WD-FORGED-" + SEQ.incrementAndGet(), earner.uuid(), other.getUuid(), other.getMsisdn(),
                "forged-" + UUID.randomUUID());
        try {
            Map<UUID, WalletCheck> checks = walletChecks.check(List.of(load(real), load(forged)));
            for (WalletCheck check : checks.values()) {
                assertThat(check.heldAmount()).isEqualByComparingTo("2500");
                assertThat(check.heldDebitedAmount()).isEqualByComparingTo("1000");
                assertThat(check.covers()).isFalse();
                assertThat(check.ledgerMatches()).isTrue();
                assertThat(check.flags()).containsExactly(WalletCheck.WALLET_SHORT);
            }
        } finally {
            jdbc.update("delete from withdrawals where uuid = ?", forged);
        }
    }

    @Test
    @DisplayName("earner-facing withdrawal endpoints carry no wallet check")
    void earnerViewsHaveNoCheck() throws Exception {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "2000");
        UUID uuid = confirm(earner, method(earner), "500");

        mvc.perform(get("/api/v1/wallets/{user}/withdrawals", earner.uuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.data.content[0].wallet_check").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].wallet_flags").doesNotExist());
        mvc.perform(get("/api/v1/wallets/{user}/withdrawals/{uuid}", earner.uuid(), uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallet_check").doesNotExist())
                .andExpect(jsonPath("$.data.wallet_flags").doesNotExist());
    }

    @Test
    @DisplayName("a page of 20 rows across 3 owners costs two statements, the same as one row")
    void noNPlusOne() {
        List<WalletOwner> owners = List.of(WalletOwner.user(user()), WalletOwner.user(user()),
                WalletOwner.organisation(UUID.randomUUID()));
        owners.forEach(o -> fund(o, "50000"));
        List<UUID> rows = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            WalletOwner owner = owners.get(i % 3);
            rows.add(confirm(owner, method(owner), "100"));
        }
        List<Withdrawal> page = rows.stream().map(this::load).toList();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        walletChecks.check(page.subList(0, 1));
        long one = statistics.getPrepareStatementCount();

        statistics.clear();
        Map<UUID, WalletCheck> checks = walletChecks.check(page);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2).isEqualTo(one);

        assertThat(checks).hasSize(20);
        assertThat(checks.get(rows.get(0)).heldCount()).isEqualTo(7);
        assertThat(checks.get(rows.get(0)).heldAmount()).isEqualByComparingTo("700");
        assertThat(checks.get(rows.get(0)).availableBalance()).isEqualByComparingTo("49300");
        assertThat(checks.get(rows.get(2)).heldCount()).isEqualTo(6);
        assertThat(checks.get(rows.get(2)).availableBalance()).isEqualByComparingTo("49400");
        assertThat(checks.values()).allSatisfy(c -> assertThat(c.flags()).isEmpty());
    }

    @Test
    @DisplayName("a finished withdrawal has no balance after: its debit is no longer to come")
    void finishedHasNoAfter() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "2000");
        UUID uuid = confirm(earner, method(earner), "500");
        withdrawals.adminReject(uuid, "The number belongs to someone else.");

        WalletCheck check = checkOf(uuid);
        assertThat(check.balanceAfter()).isNull();
        assertThat(check.heldAmount()).isEqualByComparingTo("0");
        assertThat(check.availableBalance()).isEqualByComparingTo("2000");
        assertThat(check.accountBalance()).isEqualByComparingTo("2000");
        assertThat(check.flags()).isEmpty();
    }

    private WalletCheck checkOf(UUID uuid) {
        return walletChecks.check(List.of(load(uuid))).get(uuid);
    }

    private UUID queued(String amount) {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "20000");
        return confirm(earner, method(earner), amount);
    }

    private UUID confirm(WalletOwner owner, PayoutMethod method, String amount) {
        actAs(UUID.randomUUID(), "Jane Tester");
        Withdrawal requested = withdrawals.request(owner, null, new BigDecimal(amount), method.getUuid(),
                "key-" + UUID.randomUUID()).withdrawal();
        return withdrawals.confirm(owner, requested.getUuid(), lastCode()).getUuid();
    }

    private void actAs(UUID user, String name) {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user);
        when(domainSecurityService.getCurrentCallerContact())
                .thenReturn(Optional.of(new DomainSecurityService.CallerContact(user + "@example.com", name)));
    }

    private void fund(WalletOwner owner, String amount) {
        walletService.settleSaleShare(owner, WalletPurse.deliveryLegOf(owner.type()), new BigDecimal(amount), BigDecimal.ZERO, KES,
                "sale-" + UUID.randomUUID(), "Class sale share");
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
                        + "net_amount, payout_method_uuid, msisdn, status, idempotency_key, created_by, purse) "
                        + "values (?, ?, ?, 'KES', 100, 0, 100, ?, ?, 'PAID', ?, 'TEST', "
                        + "case when ? = 'ORGANISATION' then 'EARNINGS' else 'INSTRUCTOR' end)",
                "WD-OLD-" + SEQ.incrementAndGet(), owner.type().name(), owner.uuid(), method.getUuid(), msisdn,
                "old-" + UUID.randomUUID(), owner.type().name());
        return method;
    }

    private Withdrawal load(UUID uuid) {
        return withdrawalRepository.findByUuid(uuid).orElseThrow();
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
