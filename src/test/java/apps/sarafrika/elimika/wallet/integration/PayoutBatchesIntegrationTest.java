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
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutState;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.repository.PayoutBatchRepository;
import apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutBatchSender;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WithdrawalNotifier;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import apps.sarafrika.elimika.wallet.service.impl.PayoutBatchServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WithdrawalServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WithdrawalWalletChecksImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Admin-approved payout batches, from the review queue to M-Pesa, with a gateway the test controls. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({apps.sarafrika.elimika.shared.sms.template.SmsTemplateService.class, WalletServiceImpl.class, LedgerServiceImpl.class, WalletLedgerReconciliationJob.class, JpaConfig.class,
        WithdrawalServiceImpl.class, PayoutSettingsService.class, OneTimeCodeService.class, WithdrawalNotifier.class,
        PayoutBatchServiceImpl.class, PayoutBatchSender.class, WithdrawalWalletChecksImpl.class,
        PayoutBatchesIntegrationTest.Fakes.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Payout batches")
class PayoutBatchesIntegrationTest {

    private static final String KES = "KES";
    private static final int MAX_IN_FLIGHT = 3;
    private static final AtomicInteger SEQ = new AtomicInteger(500_000_000);
    private static final Pattern DIGITS = Pattern.compile("is (\\d{6})");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("otp.hmac-secret", () -> "batch-test-secret");
        registry.add("otp.resend-cooldown", () -> "PT0S");
        registry.add("otp.max-per-hour", () -> "500");
        registry.add("wallet.payout.batch.max-in-flight", () -> String.valueOf(MAX_IN_FLIGHT));
        registry.add("wallet.payout.batch.per-tick", () -> "10");
    }

    /** A gateway that records every reference it is sent, and answers as each test sets. */
    @TestConfiguration
    static class Fakes {
        static final List<String> SMS = new ArrayList<>();
        static final List<String> SUBMITTED = new ArrayList<>();
        static final Map<String, PayoutState> RESULTS = new HashMap<>();
        static boolean submitThrows;
        static MobileMoneyPayoutGateway.PayoutFloat payoutFloat;

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
                    SUBMITTED.add(reference);
                    if (submitThrows) {
                        throw new IllegalStateException("gateway down");
                    }
                    return outcome(PayoutState.PENDING);
                }

                @Override
                public Optional<PayoutOutcome> status(String reference) {
                    return Optional.of(outcome(RESULTS.getOrDefault(reference, PayoutState.PENDING)));
                }

                @Override
                public Optional<PayoutFloat> currentFloat() {
                    return Optional.ofNullable(payoutFloat);
                }
            };
        }

        static MobileMoneyPayoutGateway.PayoutOutcome outcome(PayoutState state) {
            boolean ok = state == PayoutState.SUCCESS;
            return new MobileMoneyPayoutGateway.PayoutOutcome("gw-" + UUID.randomUUID(), state, ok ? "TJE4K9L2QX" : null,
                    null, ok ? new BigDecimal("15.00") : null,
                    state == PayoutState.FAILED ? "The balance is insufficient for the transaction." : null, false,
                    ok ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        }
    }

    @Autowired
    private WithdrawalService withdrawals;
    @Autowired
    private PayoutBatchService batches;
    @Autowired
    private PayoutBatchSender sender;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private WalletLedgerReconciliationJob reconciliationJob;
    @Autowired
    private PayoutMethodRepository methodRepository;
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private PayoutBatchRepository batchRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrencyService currencyService;
    @MockitoBean
    private DomainSecurityService domainSecurityService;

    private final UUID maker = UUID.randomUUID();
    private final UUID checker = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        Fakes.SMS.clear();
        Fakes.SUBMITTED.clear();
        Fakes.RESULTS.clear();
        Fakes.submitThrows = false;
        Fakes.payoutFloat = floatOf("1000000");
        jdbc.update("update wallet_payout_settings set require_admin_approval = true");
        // Each test starts with no batch rows in flight, so the sender's limit is the test's own.
        jdbc.update("update withdrawals set payout_batch_uuid = null, status = 'FAILED' where payout_batch_uuid is not null "
                + "and status in ('SENDING', 'PROCESSING')");
        jdbc.update("update payout_batches set status = 'PARTLY_FAILED' where status = 'RELEASED'");
    }

    @Test
    @DisplayName("with admin approval required, a clean confirmed withdrawal waits for review and is not sent")
    void confirmWaitsForAdmin() {
        UUID uuid = queued("1500");

        Withdrawal withdrawal = load(uuid);
        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
        assertThat(withdrawal.getReviewFlags()).isNull();
        assertThat(withdrawal.getPayoutBatchUuid()).isNull();
        assertThat(Fakes.SUBMITTED).isEmpty();
        assertThat(withdrawals.queue(List.of(WithdrawalStatus.NEEDS_REVIEW), true,
                Pageable.unpaged()).map(Withdrawal::getUuid).getContent()).contains(uuid);
    }

    @Test
    @DisplayName("a draft takes and drops rows, totals their net amounts, and cannot change once submitted")
    void buildAndSubmit() {
        UUID a = queued("1000");
        UUID b = queued("2500");
        UUID c = queued("700");

        actAs(maker, "Maker Admin");
        PayoutBatch batch = batches.create(List.of());
        assertThat(batch.getStatus()).isEqualTo(PayoutBatchStatus.DRAFT);
        assertThat(batch.getRowCount()).isZero();

        batch = batches.addWithdrawals(batch.getUuid(), List.of(a, b, c, a));
        assertThat(batch.getRowCount()).isEqualTo(3);
        assertThat(batch.getTotalAmount()).isEqualByComparingTo("4200");

        batch = batches.removeWithdrawal(batch.getUuid(), c);
        assertThat(batch.getRowCount()).isEqualTo(2);
        assertThat(batch.getTotalAmount()).isEqualByComparingTo("3500");
        assertThat(load(c).getPayoutBatchUuid()).isNull();
        assertThat(load(a).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);

        UUID batchUuid = batch.getUuid();
        assertThatThrownBy(() -> batches.submit(batches.create(List.of()).getUuid()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least one");

        batch = batches.submit(batchUuid);
        assertThat(batch.getStatus()).isEqualTo(PayoutBatchStatus.AWAITING_APPROVAL);
        assertThat(batch.getSubmittedByUserUuid()).isEqualTo(maker);
        assertThat(batch.getSubmittedByName()).isEqualTo("Maker Admin");
        assertThatThrownBy(() -> batches.addWithdrawals(batchUuid, List.of(c))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> batches.removeWithdrawal(batchUuid, a)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> batches.cancel(batchUuid)).isInstanceOf(IllegalStateException.class);
        assertThat(audits("PAYOUT_BATCH_SUBMITTED", batchUuid)).isEqualTo(2);
    }

    @Test
    @DisplayName("neither the admin who built a batch nor the one who submitted it may approve it")
    void makerChecker() {
        UUID a = queued("1000");
        actAs(maker, "Maker Admin");
        PayoutBatch batch = batches.create(List.of(a));

        UUID submitter = UUID.randomUUID();
        actAs(submitter, "Submitting Admin");
        batches.submit(batch.getUuid());
        assertThat(batches.get(batch.getUuid()).canBeApprovedBy(submitter)).isFalse();
        assertThat(batches.get(batch.getUuid()).canBeApprovedBy(maker)).isFalse();
        assertThat(batches.get(batch.getUuid()).canBeApprovedBy(checker)).isTrue();

        assertThatThrownBy(() -> batches.approve(batch.getUuid(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different admin");
        actAs(maker, "Maker Admin");
        assertThatThrownBy(() -> batches.approve(batch.getUuid(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different admin");
        assertThat(load(a).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);

        actAs(checker, "Checker Admin");
        assertThat(batches.approve(batch.getUuid(), false).getStatus()).isEqualTo(PayoutBatchStatus.RELEASED);
    }

    @Test
    @DisplayName("approval is refused when the float is below the total, and when it is unknown unless overridden")
    void floatChecks() {
        PayoutBatch batch = submitted(queued("3000"), queued("2000"));

        actAs(checker, "Checker Admin");
        Fakes.payoutFloat = floatOf("4999");
        assertThatThrownBy(() -> batches.approve(batch.getUuid(), true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("KES 4,999").hasMessageContaining("KES 5,000");

        Fakes.payoutFloat = null;
        assertThatThrownBy(() -> batches.approve(batch.getUuid(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("unknown");
        assertThat(batches.get(batch.getUuid()).getStatus()).isEqualTo(PayoutBatchStatus.AWAITING_APPROVAL);

        PayoutBatch released = batches.approve(batch.getUuid(), true);
        assertThat(released.getStatus()).isEqualTo(PayoutBatchStatus.RELEASED);
        assertThat(released.getFloatAtApproval()).isNull();
        assertThat(released.isFloatOverride()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from wallet_audit_events where event_type = 'PAYOUT_BATCH_APPROVED' "
                + "and updated_values->>'batch_uuid' = ? and updated_values->>'override_unknown_float' = 'true'",
                Long.class, batch.getUuid().toString())).isEqualTo(2);

        PayoutBatch covered = submitted(queued("1000"));
        actAs(checker, "Checker Admin");
        Fakes.payoutFloat = floatOf("1000");
        PayoutBatch approved = batches.approve(covered.getUuid(), false);
        assertThat(approved.getFloatAtApproval()).isEqualByComparingTo("1000");
        assertThat(approved.isFloatOverride()).isFalse();
    }

    @Test
    @DisplayName("approval sends rows a few at a time, and the batch completes when every row is paid")
    void releaseAndComplete() {
        List<UUID> rows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rows.add(queued("1000"));
        }
        PayoutBatch batch = submitted(rows.toArray(UUID[]::new));
        actAs(checker, "Checker Admin");
        batches.approve(batch.getUuid(), false);

        assertThat(rows).allSatisfy(uuid -> assertThat(load(uuid).getStatus()).isEqualTo(WithdrawalStatus.SENDING));
        assertThat(rows).allSatisfy(uuid -> assertThat(load(uuid).getReviewedByUserUuid()).isEqualTo(checker));
        assertThat(Fakes.SUBMITTED).isEmpty();

        sender.tick();
        assertThat(Fakes.SUBMITTED).hasSize(MAX_IN_FLIGHT);
        assertThat(count(rows, WithdrawalStatus.PROCESSING)).isEqualTo(MAX_IN_FLIGHT);
        assertThat(count(rows, WithdrawalStatus.SENDING)).isEqualTo(2);
        assertThat(Fakes.SUBMITTED).containsExactlyElementsOf(rows.subList(0, MAX_IN_FLIGHT).stream().map(UUID::toString).toList());

        sender.tick();
        assertThat(Fakes.SUBMITTED).as("nothing more while the limit is in flight").hasSize(MAX_IN_FLIGHT);

        rows.forEach(uuid -> Fakes.RESULTS.put(uuid.toString(), PayoutState.SUCCESS));
        rows.forEach(withdrawals::refresh);
        sender.tick();
        assertThat(Fakes.SUBMITTED).hasSize(5);
        assertThat(batches.get(batch.getUuid()).getStatus()).isEqualTo(PayoutBatchStatus.RELEASED);

        rows.forEach(withdrawals::refresh);
        sender.tick();
        assertThat(count(rows, WithdrawalStatus.PAID)).isEqualTo(5);
        PayoutBatch done = batches.get(batch.getUuid());
        assertThat(done.getStatus()).isEqualTo(PayoutBatchStatus.COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();
        assertThat(batches.counts(List.of(batch.getUuid())).get(batch.getUuid()))
                .isEqualTo(new PayoutBatchService.RowCounts(5, 0, 0));
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a row M-Pesa fails is reversed and the batch ends partly failed")
    void partlyFailed() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        UUID failing = confirm(earner, method(earner), "2000");
        UUID paying = queued("1000");
        PayoutBatch batch = submitted(failing, paying);
        actAs(checker, "Checker Admin");
        batches.approve(batch.getUuid(), false);
        assertThat(balance(earner)).isEqualByComparingTo("3000");

        Fakes.RESULTS.put(failing.toString(), PayoutState.FAILED);
        Fakes.RESULTS.put(paying.toString(), PayoutState.SUCCESS);
        sender.tick();
        withdrawals.refresh(failing);
        withdrawals.refresh(paying);
        sender.tick();

        assertThat(load(failing).getStatus()).isEqualTo(WithdrawalStatus.FAILED);
        assertThat(balance(earner)).isEqualByComparingTo("5000");
        assertThat(load(paying).getStatus()).isEqualTo(WithdrawalStatus.PAID);
        assertThat(batches.get(batch.getUuid()).getStatus()).isEqualTo(PayoutBatchStatus.PARTLY_FAILED);
        assertThat(batches.counts(List.of(batch.getUuid())).get(batch.getUuid()))
                .isEqualTo(new PayoutBatchService.RowCounts(1, 1, 0));
        assertThat(reconciliationJob.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("rejecting a batch returns its rows to the queue, still held, ready for another batch")
    void rejectBatch() {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "5000");
        UUID row = confirm(earner, method(earner), "1500");
        PayoutBatch batch = submitted(row);

        actAs(checker, "Checker Admin");
        assertThatThrownBy(() -> batches.reject(batch.getUuid(), " ")).isInstanceOf(IllegalArgumentException.class);
        PayoutBatch rejected = batches.reject(batch.getUuid(), "Amounts need a second look.");

        assertThat(rejected.getStatus()).isEqualTo(PayoutBatchStatus.REJECTED);
        assertThat(rejected.getRejectedByUserUuid()).isEqualTo(checker);
        assertThat(rejected.getRowCount()).isEqualTo(1);
        assertThat(load(row).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
        assertThat(load(row).getPayoutBatchUuid()).isNull();
        assertThat(balance(earner)).isEqualByComparingTo("3500");
        assertThat(Fakes.SUBMITTED).isEmpty();

        actAs(maker, "Maker Admin");
        PayoutBatch again = batches.create(List.of(row));
        assertThat(load(row).getPayoutBatchUuid()).isEqualTo(again.getUuid());
    }

    @Test
    @DisplayName("return to draft needs a fresh submit; cancel lets the rows go")
    void returnToDraftAndCancel() {
        UUID row = queued("1000");
        PayoutBatch batch = submitted(row);

        actAs(checker, "Checker Admin");
        PayoutBatch draft = batches.returnToDraft(batch.getUuid());
        assertThat(draft.getStatus()).isEqualTo(PayoutBatchStatus.DRAFT);
        assertThat(draft.getSubmittedByUserUuid()).isNull();
        assertThatThrownBy(() -> batches.approve(batch.getUuid(), false)).isInstanceOf(IllegalStateException.class);

        PayoutBatch cancelled = batches.cancel(batch.getUuid());
        assertThat(cancelled.getStatus()).isEqualTo(PayoutBatchStatus.CANCELLED);
        assertThat(load(row).getPayoutBatchUuid()).isNull();
        assertThat(load(row).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
    }

    @Test
    @DisplayName("the single approve endpoint's path makes a one-row batch a second admin must approve")
    void singleApprove() {
        UUID row = queued("1200");
        actAs(maker, "Maker Admin");

        Withdrawal withdrawal = batches.submitSingle(row);

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);
        assertThat(withdrawal.getPayoutBatchUuid()).isNotNull();
        PayoutBatch batch = batches.get(withdrawal.getPayoutBatchUuid());
        assertThat(batch.getStatus()).isEqualTo(PayoutBatchStatus.AWAITING_APPROVAL);
        assertThat(batch.getRowCount()).isEqualTo(1);
        assertThat(batch.getCreatedByUserUuid()).isEqualTo(maker);
        assertThat(batch.getSubmittedByUserUuid()).isEqualTo(maker);
        assertThat(Fakes.SUBMITTED).isEmpty();
        assertThatThrownBy(() -> batches.approve(batch.getUuid(), false)).hasMessageContaining("different admin");
        assertThatThrownBy(() -> batches.submitSingle(row)).hasMessageContaining("already in another payout batch");

        actAs(checker, "Checker Admin");
        batches.approve(batch.getUuid(), false);
        sender.tick();
        assertThat(Fakes.SUBMITTED).containsExactly(row.toString());
    }

    @Test
    @DisplayName("a withdrawal sits in at most one open batch, and single rejection respects the batch")
    void oneOpenBatch() {
        UUID row = queued("1000");
        UUID other = queued("800");
        actAs(maker, "Maker Admin");
        PayoutBatch first = batches.create(List.of(row, other));
        PayoutBatch second = batches.create(List.of());

        assertThatThrownBy(() -> batches.addWithdrawals(second.getUuid(), List.of(row)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already in another payout batch");
        assertThat(load(row).getPayoutBatchUuid()).isEqualTo(first.getUuid());

        withdrawals.adminReject(other, "The number belongs to someone else.");
        assertThat(load(other).getStatus()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(load(other).getPayoutBatchUuid()).isNull();
        assertThat(batches.get(first.getUuid()).getRowCount()).isEqualTo(1);
        assertThat(batches.get(first.getUuid()).getTotalAmount()).isEqualByComparingTo("1000");

        batches.submit(first.getUuid());
        assertThatThrownBy(() -> withdrawals.adminReject(row, "Changed my mind about this payout."))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("awaiting approval");
        assertThat(load(row).getStatus()).isEqualTo(WithdrawalStatus.NEEDS_REVIEW);

        assertThatThrownBy(() -> jdbc.update("update withdrawals set status = 'REJECTED' where uuid = ?", row))
                .as("a batched row can only hold a status a batch allows")
                .hasMessageContaining("chk_withdrawal_batch_status");
    }

    @Test
    @DisplayName("running the sender again, or after a restart, never resubmits under a new reference")
    void restartSafe() {
        UUID a = queued("1000");
        UUID b = queued("1000");
        PayoutBatch batch = submitted(a, b);
        actAs(checker, "Checker Admin");
        batches.approve(batch.getUuid(), false);

        Fakes.submitThrows = true;
        sender.tick();
        assertThat(load(a).getStatus()).isEqualTo(WithdrawalStatus.SENDING);

        Fakes.submitThrows = false;
        sender.tick();
        sender.tick();
        new PayoutBatchSender(withdrawalRepository, batchRepository, withdrawals, batches, MAX_IN_FLIGHT, 10).tick();

        assertThat(load(a).getStatus()).isEqualTo(WithdrawalStatus.PROCESSING);
        assertThat(load(b).getStatus()).isEqualTo(WithdrawalStatus.PROCESSING);
        assertThat(Fakes.SUBMITTED).containsOnly(a.toString(), b.toString());
        assertThat(Fakes.SUBMITTED.stream().filter(a.toString()::equals).count())
                .as("one failed hand-over, then one that got through").isEqualTo(2);
    }

    private PayoutBatch submitted(UUID... rows) {
        actAs(maker, "Maker Admin");
        PayoutBatch batch = batches.create(List.of(rows));
        return batches.submit(batch.getUuid());
    }

    private UUID queued(String amount) {
        WalletOwner earner = WalletOwner.user(user());
        fund(earner, "20000");
        return confirm(earner, method(earner), amount);
    }

    private UUID confirm(WalletOwner owner, PayoutMethod method, String amount) {
        actAs(owner.uuid(), "Jane Tester");
        Withdrawal requested = withdrawals.request(owner, new BigDecimal(amount), method.getUuid(),
                "key-" + UUID.randomUUID()).withdrawal();
        return withdrawals.confirm(owner, requested.getUuid(), lastCode()).getUuid();
    }

    private long count(List<UUID> rows, WithdrawalStatus status) {
        return rows.stream().filter(uuid -> load(uuid).getStatus() == status).count();
    }

    private long audits(String type, UUID batchUuid) {
        return jdbc.queryForObject("select count(*) from wallet_audit_events where event_type = ? "
                + "and updated_values->>'batch_uuid' = ?", Long.class, type, batchUuid.toString());
    }

    private void actAs(UUID user, String name) {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user);
        when(domainSecurityService.getCurrentCallerContact())
                .thenReturn(Optional.of(new DomainSecurityService.CallerContact(user + "@example.com", name)));
    }

    private void fund(WalletOwner owner, String amount) {
        walletService.settleSaleShare(owner, new BigDecimal(amount), BigDecimal.ZERO, KES,
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
                        + "net_amount, payout_method_uuid, msisdn, status, idempotency_key, created_by) "
                        + "values (?, ?, ?, 'KES', 100, 0, 100, ?, ?, 'PAID', ?, 'TEST')",
                "WD-OLD-" + SEQ.incrementAndGet(), owner.type().name(), owner.uuid(), method.getUuid(), msisdn,
                "old-" + UUID.randomUUID());
        return method;
    }

    private BigDecimal balance(WalletOwner owner) {
        return walletService.balanceOf(owner, KES).balanceAmount();
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

    private static MobileMoneyPayoutGateway.PayoutFloat floatOf(String utility) {
        return new MobileMoneyPayoutGateway.PayoutFloat(new BigDecimal(utility), BigDecimal.ZERO,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static String lastCode() {
        Matcher matcher = DIGITS.matcher(Fakes.SMS.getLast());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
