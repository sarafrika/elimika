package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.utils.SortAllowList;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingLeg;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingRequest;
import apps.sarafrika.elimika.wallet.ledger.LedgerService;
import apps.sarafrika.elimika.wallet.repository.UserWalletRepository;
import apps.sarafrika.elimika.wallet.repository.UserWalletTransactionRepository;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import apps.sarafrika.elimika.wallet.service.WalletTransactionFilter;
import apps.sarafrika.elimika.wallet.service.WalletTransferResult;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.repository.WalletAuditEventRepository;
import org.springframework.beans.factory.ObjectProvider;
import apps.sarafrika.elimika.wallet.enums.LedgerAccountType;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.ZoneOffset;
import java.time.YearMonth;
import apps.sarafrika.elimika.wallet.service.WalletMonthStatement;
import apps.sarafrika.elimika.wallet.repository.MonthlyWalletActivity;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Locale;
import java.util.TreeMap;
import apps.sarafrika.elimika.wallet.service.PurseTransferResult;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WalletPurseBalance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Wallet balances, one wallet row per owner, purse and currency, plus a dual-write into the double-entry
 * ledger, where each row mirrors the liability account of its purse.
 * <p>
 * {@code user_wallets} is still authoritative: it is the only thing read, the only thing a balance
 * check consults, and the only thing a transfer's sufficiency test looks at. Every mutation is also
 * mirrored into the ledger, but the ledger is written after the fact and read by nobody yet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MAX_STATEMENT_MONTHS = 36;
    /** Transactions sort by when and what; amounts and balances are not ordering keys. */
    private static final Set<String> TRANSACTION_SORTABLE_PROPERTIES = Set.of(
            "createdDate", "lastModifiedDate", "transactionType");
    private static final String CAUSE_WALLET_TRANSACTION = "WALLET_TRANSACTION";
    private static final String CAUSE_WALLET_TRANSFER = "WALLET_TRANSFER";
    private static final String CAUSE_WITHDRAWAL = "WITHDRAWAL";
    private static final String REVERSAL_SUFFIX = ":reversal";

    private final UserWalletRepository userWalletRepository;
    private final UserWalletTransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final LedgerService ledgerService;
    private final WalletAuditEventRepository auditEventRepository;
    private final ObjectProvider<DomainSecurityService> domainSecurityService;
    private final WithdrawalRepository withdrawalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WalletPurseBalance> pursesOf(WalletOwner owner, String currencyCode) {
        String currency = resolveCurrencyCode(currencyCode);
        Map<WalletPurse, UserWallet> rows = new EnumMap<>(WalletPurse.class);
        rowsOf(owner, currency).forEach(wallet -> rows.put(wallet.getPurse(), wallet));
        Map<WalletPurse, BigDecimal> held = heldByPurse(owner, currency);
        List<WalletPurseBalance> purses = new ArrayList<>();
        for (WalletPurse purse : WalletPurse.of(owner.type())) {
            UserWallet wallet = rows.get(purse);
            BigDecimal heldAmount = held.getOrDefault(purse, ZERO);
            purses.add(wallet == null
                    ? new WalletPurseBalance(purse, null, currency, ZERO, heldAmount, WalletTotals.none(), null, null)
                    : new WalletPurseBalance(purse, wallet.getUuid(), currency, wallet.getBalanceAmount(), heldAmount,
                    totals(wallet, false), wallet.getCreatedDate(), wallet.getLastModifiedDate()));
        }
        return purses;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceSummary getBalanceSummary(UUID userUuid, String currencyCode) {
        return balanceOf(WalletOwner.user(userUuid), currencyCode);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceSummary balanceOf(WalletOwner owner, String currencyCode) {
        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        List<UserWallet> rows = rowsOf(owner, resolvedCurrency);
        BigDecimal total = rows.stream().map(UserWallet::getBalanceAmount).reduce(ZERO, BigDecimal::add);
        return new WalletBalanceSummary(rows.isEmpty() ? null : rows.getFirst().getUuid(), resolvedCurrency, total);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceSummary balanceOf(WalletOwner owner, WalletPurse purse, String currencyCode) {
        requireFits(owner, purse);
        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        return userWalletRepository.findByOwnerTypeAndOwnerUuidAndPurseAndCurrencyCode(owner.type(), owner.uuid(),
                        purse, resolvedCurrency)
                .map(wallet -> new WalletBalanceSummary(wallet.getUuid(), resolvedCurrency, wallet.getBalanceAmount(),
                        purse))
                .orElseGet(() -> new WalletBalanceSummary(null, resolvedCurrency, ZERO, purse));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTotals totalsOf(WalletOwner owner, String currencyCode) {
        WalletTotals sum = WalletTotals.none();
        for (UserWallet wallet : rowsOf(owner, resolveCurrencyCode(currencyCode))) {
            WalletTotals one = totals(wallet, true);
            sum = new WalletTotals(sum.sharesTotal().add(one.sharesTotal()),
                    sum.platformFeesTotal().add(one.platformFeesTotal()), sum.salesCount() + one.salesCount(),
                    sum.transferredOutTotal().add(one.transferredOutTotal()));
        }
        return sum;
    }

    /** One purse row's lifetime figures; {@code withoutOwnMoves} leaves out transfers to the owner's other purses. */
    private WalletTotals totals(UserWallet wallet, boolean withoutOwnMoves) {
        BigDecimal shares = ZERO;
        BigDecimal fees = ZERO;
        BigDecimal out = ZERO;
        long sales = 0;
        for (Object[] row : transactionRepository.totalsByType(wallet.getId())) {
            WalletTransactionType type = (WalletTransactionType) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            long count = ((Number) row[2]).longValue();
            switch (type) {
                case SALE -> { shares = shares.add(sum); sales += count; }
                case SALE_REVERSAL -> { shares = shares.subtract(sum); sales -= count; }
                case PLATFORM_FEE -> fees = fees.add(sum);
                case TRANSFER_OUT -> out = out.add(sum);
                default -> { }
            }
        }
        if (withoutOwnMoves && out.signum() > 0) {
            out = out.subtract(transactionRepository.sumOwnMoves(wallet.getId(), WalletTransactionType.TRANSFER_OUT));
        }
        return new WalletTotals(shares, fees, Math.max(sales, 0), out);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode, Pageable pageable) {
        return statementOf(owner, currencyCode, WalletTransactionFilter.all(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode,
                                                   WalletTransactionFilter filter, Pageable pageable) {
        SortAllowList.validate(pageable, TRANSACTION_SORTABLE_PROPERTIES);
        return transactionRepository.findFiltered(owner.type(), owner.uuid(), resolveCurrencyCode(currencyCode),
                filter.pursesFor(owner.type()), filter.typesOrAll(), filter.fromInclusive(), filter.toExclusive(),
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWalletTransaction> exportOf(WalletOwner owner, String currencyCode, WalletTransactionFilter filter) {
        Page<UserWalletTransaction> page = statementOf(owner, currencyCode, filter, PageRequest.of(0, MAX_EXPORT_ROWS));
        if (page.getTotalElements() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("More than " + MAX_EXPORT_ROWS
                    + " rows match; narrow the dates and download again");
        }
        return page.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletAuditEvent> auditOf(WalletOwner owner, Pageable pageable) {
        return auditEventRepository.findByOwner(owner.type(), owner.uuid(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletMonthStatement> monthlyStatementsOf(WalletOwner owner, String currencyCode, int months) {
        return monthlyStatementsOf(owner, null, currencyCode, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletMonthStatement> monthlyStatementsOf(WalletOwner owner, WalletPurse purse, String currencyCode,
                                                          int months) {
        int window = Math.clamp(months, 1, MAX_STATEMENT_MONTHS);
        List<UserWallet> rows = rowsOf(owner, resolveCurrencyCode(currencyCode)).stream()
                .filter(wallet -> purse == null || wallet.getPurse() == purse)
                .toList();
        boolean combined = purse == null;
        // Each purse is a statement of its own; the owner's is their sum, month by month.
        Map<YearMonth, WalletMonthStatement> sum = new TreeMap<>(Comparator.reverseOrder());
        for (UserWallet wallet : rows) {
            for (WalletMonthStatement month : monthlyStatements(
                    transactionRepository.monthlyActivity(wallet.getId(), combined), window)) {
                sum.merge(month.month(), month, (a, b) -> new WalletMonthStatement(a.month(),
                        a.openingBalance().add(b.openingBalance()), a.moneyIn().add(b.moneyIn()),
                        a.moneyOut().add(b.moneyOut()), a.closingBalance().add(b.closingBalance()),
                        a.transactionCount() + b.transactionCount()));
            }
        }
        if (rows.size() > 1) {
            // A purse that started later is at zero, not absent, in the months before its first row.
            YearMonth earliest = sum.keySet().stream().min(Comparator.naturalOrder()).orElse(null);
            YearMonth current = YearMonth.now(ZoneOffset.UTC);
            for (YearMonth month = earliest; month != null && !month.isAfter(current); month = month.plusMonths(1)) {
                sum.putIfAbsent(month, new WalletMonthStatement(month, ZERO, ZERO, ZERO, ZERO, 0));
            }
        }
        return List.copyOf(sum.values());
    }

    private static List<WalletMonthStatement> monthlyStatements(List<MonthlyWalletActivity> newestFirst, int window) {
        if (newestFirst.isEmpty()) {
            return List.of();
        }
        Map<YearMonth, MonthlyWalletActivity> byMonth = new HashMap<>();
        newestFirst.forEach(row -> byMonth.put(YearMonth.parse(row.getMonth()), row));
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth earliest = YearMonth.parse(newestFirst.getLast().getMonth());
        YearMonth windowStart = current.minusMonths(window - 1L);
        YearMonth start = earliest.isAfter(windowStart) ? earliest : windowStart;
        BigDecimal running = newestFirst.stream()
                .filter(row -> YearMonth.parse(row.getMonth()).isBefore(start))
                .findFirst()
                .map(MonthlyWalletActivity::getClosingBalance)
                .orElse(ZERO);

        List<WalletMonthStatement> statements = new ArrayList<>();
        for (YearMonth month = start; !month.isAfter(current); month = month.plusMonths(1)) {
            MonthlyWalletActivity row = byMonth.get(month);
            if (row == null) {
                statements.add(new WalletMonthStatement(month, running, ZERO, ZERO, running, 0));
            } else {
                statements.add(new WalletMonthStatement(month, row.getOpeningBalance(), row.getMoneyIn(),
                        row.getMoneyOut(), row.getClosingBalance(), row.getTransactionCount()));
                running = row.getClosingBalance();
            }
        }
        return statements.reversed();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletStanding standingOf(WalletOwner owner, String currencyCode, boolean canWithdraw) {
        // A confirmed withdrawal has already left the balance, so available is the balance and held is extra.
        WalletBalanceSummary summary = balanceOf(owner, currencyCode);
        BigDecimal held = withdrawalRepository.sumByStatus(owner.type(), owner.uuid(), summary.currencyCode(),
                WithdrawalStatus.HELD);
        return new WalletStanding(held, summary.balanceAmount(), canWithdraw);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletPurse debitPurse(WalletOwner owner, WalletPurse requested, String currencyCode) {
        if (requested != null) {
            requireFits(owner, requested);
            return requested;
        }
        if (owner.type() == WalletOwnerType.ORGANISATION) {
            return WalletPurse.EARNINGS;
        }
        List<UserWallet> funded = rowsOf(owner, resolveCurrencyCode(currencyCode)).stream()
                .filter(wallet -> wallet.getBalanceAmount().signum() > 0)
                .sorted(Comparator.comparing(UserWallet::getPurse))
                .toList();
        if (funded.size() > 1) {
            throw new IllegalArgumentException("Choose a purse: " + funded.stream()
                    .map(wallet -> wallet.getPurse() + " holds " + wallet.getBalanceAmount().stripTrailingZeros()
                            .toPlainString())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        }
        if (funded.size() == 1) {
            return funded.getFirst().getPurse();
        }
        // Nothing funded: name the oldest row so the caller's own balance check explains the refusal.
        return rowsOf(owner, resolveCurrencyCode(currencyCode)).stream().map(UserWallet::getPurse).findFirst()
                .orElse(WalletPurse.COURSE_CREATOR);
    }

    @Override
    @Transactional
    public UserWalletTransaction holdWithdrawal(WalletOwner owner, WalletPurse purse, BigDecimal netAmount,
                                                String currencyCode, String reference, String description) {
        String currency = resolveCurrencyCode(currencyCode);
        UserWallet wallet = lockOrCreateWallet(owner, purse, currency);
        UserWalletTransaction row = applyDebit(wallet, netAmount, WalletTransactionType.WITHDRAWAL, reference,
                description, null, null);
        postRow(row, description, List.of(LedgerPostingLeg.debit(purseAccount(wallet, currency), netAmount),
                LedgerPostingLeg.credit(payoutsInFlight(currency), netAmount)));
        return row;
    }

    @Override
    @Transactional
    public UserWalletTransaction chargePayoutFee(WalletOwner owner, WalletPurse purse, BigDecimal fee,
                                                 String currencyCode, String reference, String description) {
        String currency = resolveCurrencyCode(currencyCode);
        UserWallet wallet = lockOrCreateWallet(owner, purse, currency);
        UserWalletTransaction row = applyDebit(wallet, fee, WalletTransactionType.PAYOUT_FEE, reference + ":fee",
                description, null, null);
        postRow(row, description, List.of(LedgerPostingLeg.debit(purseAccount(wallet, currency), fee),
                LedgerPostingLeg.credit(LedgerAccountRef.platformFeeRevenue(currency), fee)));
        return row;
    }

    @Override
    @Transactional
    public List<UserWalletTransaction> reverseWithdrawal(WalletOwner owner, WalletPurse purse, BigDecimal netAmount,
                                                         BigDecimal fee, String currencyCode, String reference,
                                                         String description) {
        String currency = resolveCurrencyCode(currencyCode);
        BigDecimal feeBack = fee == null ? ZERO : fee;
        // Back to each purse what its rows gave; a withdrawal split over purses by the backfill has rows in two.
        Map<WalletPurse, BigDecimal> back = new EnumMap<>(WalletPurse.class);
        for (UserWalletTransaction debit : transactionRepository.findOwnerRows(owner.type(), owner.uuid(), currency,
                List.of(WalletTransactionType.WITHDRAWAL, WalletTransactionType.PAYOUT_FEE),
                List.of(reference, reference + ":fee"))) {
            back.merge(debit.getWallet().getPurse(), debit.getAmount(), BigDecimal::add);
        }
        BigDecimal total = netAmount.add(feeBack);
        if (back.isEmpty() || back.values().stream().reduce(ZERO, BigDecimal::add).compareTo(total) != 0) {
            back.clear();
            back.put(purse, total);
        }
        List<LedgerPostingLeg> legs = new ArrayList<>();
        legs.add(LedgerPostingLeg.debit(payoutsInFlight(currency), netAmount));
        if (feeBack.signum() > 0) {
            legs.add(LedgerPostingLeg.debit(LedgerAccountRef.platformFeeRevenue(currency), feeBack));
        }
        List<UserWalletTransaction> rows = new ArrayList<>();
        for (Map.Entry<WalletPurse, BigDecimal> entry : back.entrySet()) {
            UserWallet wallet = lockOrCreateWallet(owner, entry.getKey(), currency);
            rows.add(applyCredit(wallet, entry.getValue(), currency, WalletTransactionType.WITHDRAWAL_REVERSAL,
                    reference + ":reversal", description, null, null));
            legs.add(LedgerPostingLeg.credit(purseAccount(wallet, currency), entry.getValue()));
        }
        postRow(rows.getFirst(), description, legs);
        return rows;
    }

    @Override
    @Transactional
    public void settleWithdrawal(BigDecimal netAmount, BigDecimal mpesaCharges, String currencyCode, String reference,
                                 String description) {
        String currency = resolveCurrencyCode(currencyCode);
        List<LedgerPostingLeg> legs = new ArrayList<>();
        legs.add(LedgerPostingLeg.debit(payoutsInFlight(currency), netAmount));
        legs.add(LedgerPostingLeg.credit(LedgerAccountRef.platformCashMpesa(currency), netAmount));
        if (mpesaCharges != null && mpesaCharges.signum() > 0) {
            legs.add(LedgerPostingLeg.debit(LedgerAccountRef.platform(LedgerAccountType.EXPENSE, LedgerPurse.PAYOUT_FEES,
                    currency), mpesaCharges));
            legs.add(LedgerPostingLeg.credit(LedgerAccountRef.platformCashMpesa(currency), mpesaCharges));
        }
        postToLedger(new LedgerPostingRequest("withdrawal-settle:" + reference, null, description,
                CAUSE_WITHDRAWAL, null, legs));
    }

    private void postRow(UserWalletTransaction row, String description, List<LedgerPostingLeg> legs) {
        postToLedger(new LedgerPostingRequest("wallet-txn:" + row.getUuid(), row.getCreatedDate(), description,
                CAUSE_WALLET_TRANSACTION, row.getUuid(), legs));
    }

    private static LedgerAccountRef payoutsInFlight(String currency) {
        return LedgerAccountRef.platform(LedgerAccountType.LIABILITY, LedgerPurse.PAYOUTS_IN_FLIGHT, currency);
    }

    @Override
    @Transactional
    public UserWallet deposit(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode,
                              String reference, String description) {
        return applyCredit(userUuid, purse == null ? WalletPurse.STUDENT : purse, amount, currencyCode,
                WalletTransactionType.DEPOSIT, reference, description, null);
    }

    @Override
    @Transactional
    public UserWallet creditSale(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode,
                                 String reference, String description) {
        if (purse == null) {
            throw new IllegalArgumentException("Say which purse the sale is paid into");
        }
        return applyCredit(userUuid, purse, amount, currencyCode, WalletTransactionType.SALE, reference, description,
                null);
    }

    @Override
    @Transactional
    public boolean creditSaleIdempotent(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode,
                                        String reference, String description) {
        if (StringUtils.hasText(reference) && transactionRepository.existsByReference(reference)) {
            log.debug("Skipping duplicate sale credit for reference {}", reference);
            return false;
        }
        try {
            creditSale(userUuid, purse, amount, currencyCode, reference, description);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate delivery raced past the existsByReference check; the
            // unique index on (reference) for SALE transactions rejected the second insert.
            log.debug("Sale credit for reference {} already recorded concurrently; skipping", reference);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean settleSaleShare(WalletOwner owner, WalletPurse purse, BigDecimal gross, BigDecimal platformFee,
                                   String currencyCode, String reference, String description) {
        if (!StringUtils.hasText(reference)) {
            throw new IllegalArgumentException("A sale share needs a reference so it is paid at most once");
        }
        if (transactionRepository.existsByReference(reference)) {
            log.debug("Sale share {} already settled; skipping", reference);
            return false;
        }
        validateAmount(gross);
        BigDecimal fee = platformFee == null ? ZERO : platformFee;
        if (fee.signum() < 0 || fee.compareTo(gross) > 0) {
            throw new IllegalArgumentException("Platform fee must be between zero and the share");
        }

        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        UserWallet wallet = lockOrCreateWallet(owner, purse, resolvedCurrency);
        UserWalletTransaction sale = applyCredit(
                wallet, gross, resolvedCurrency, WalletTransactionType.SALE, reference, description, null, null);
        recordLedgerCredit(wallet, sale, gross, resolvedCurrency, WalletTransactionType.SALE, description);
        if (fee.signum() > 0) {
            String feeDescription = "Platform fee - " + description;
            UserWalletTransaction feeRow = applyDebit(
                    wallet, fee, WalletTransactionType.PLATFORM_FEE, reference + ":fee", feeDescription, null, null);
            recordLedgerFee(wallet, feeRow, fee, resolvedCurrency, feeDescription);
        }
        return true;
    }

    @Override
    @Transactional
    public UserWalletTransaction reverseSaleShare(String saleReference, String description) {
        UserWalletTransaction sale = transactionRepository
                .findFirstByReferenceAndTransactionTypeOrderByIdAsc(saleReference, WalletTransactionType.SALE)
                .orElseThrow(() -> new IllegalArgumentException("No sale share " + saleReference));
        String reference = saleReference + REVERSAL_SUFFIX;
        if (transactionRepository.existsByReference(reference)) {
            log.debug("Sale share {} already reversed; skipping", saleReference);
            return null;
        }
        BigDecimal fee = transactionRepository
                .findFirstByReferenceAndTransactionTypeOrderByIdAsc(saleReference + ":fee",
                        WalletTransactionType.PLATFORM_FEE)
                .map(UserWalletTransaction::getAmount).orElse(ZERO);
        BigDecimal kept = sale.getAmount().subtract(fee);
        UserWallet original = sale.getWallet();
        String currency = original.getCurrencyCode();
        UserWallet wallet = lockOrCreateWallet(original.owner(), original.getPurse(), currency);
        UserWalletTransaction row = applyDebit(wallet, kept, WalletTransactionType.SALE_REVERSAL, reference,
                description, null, null);
        List<LedgerPostingLeg> legs = new ArrayList<>();
        legs.add(LedgerPostingLeg.debit(purseAccount(wallet, currency), kept));
        if (fee.signum() > 0) {
            legs.add(LedgerPostingLeg.debit(LedgerAccountRef.platformFeeRevenue(currency), fee));
        }
        legs.add(LedgerPostingLeg.credit(LedgerAccountRef.platformUnallocatedRevenue(currency), sale.getAmount()));
        postRow(row, description, legs);
        return row;
    }

    @Override
    @Transactional
    public WalletTransferResult transfer(UUID fromUserUuid, WalletPurse sourcePurse, UUID toUserUuid,
                                         BigDecimal amount, String currencyCode, String reference,
                                         String description) {
        if (fromUserUuid == null || toUserUuid == null) {
            throw new IllegalArgumentException("Both source and target users are required");
        }
        if (fromUserUuid.equals(toUserUuid)) {
            throw new IllegalArgumentException("Source and target users must be different");
        }
        validateAmount(amount);

        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        WalletOwner from = WalletOwner.user(fromUserUuid);
        WalletPurse fromPurse = debitPurse(from, sourcePurse, resolvedCurrency);
        // Rows are locked in uuid order whichever way the money goes, so two opposite transfers cannot deadlock.
        boolean sourceFirst = fromUserUuid.compareTo(toUserUuid) < 0;
        UserWallet first = sourceFirst ? lockOrCreateWallet(from, fromPurse, resolvedCurrency)
                : lockOrCreateWallet(WalletOwner.user(toUserUuid), WalletPurse.STUDENT, resolvedCurrency);
        UserWallet second = sourceFirst
                ? lockOrCreateWallet(WalletOwner.user(toUserUuid), WalletPurse.STUDENT, resolvedCurrency)
                : lockOrCreateWallet(from, fromPurse, resolvedCurrency);
        UserWallet source = sourceFirst ? first : second;
        UserWallet target = sourceFirst ? second : first;

        UUID transferReference = UUID.randomUUID();
        applyDebit(source, amount, WalletTransactionType.TRANSFER_OUT, reference, description, toUserUuid, transferReference);
        applyCredit(target, amount, resolvedCurrency, WalletTransactionType.TRANSFER_IN, reference, description, fromUserUuid, transferReference);
        recordLedgerTransfer(source, target, amount, resolvedCurrency, description, transferReference);

        return new WalletTransferResult(source, target, transferReference);
    }

    @Override
    @Transactional
    public PurseTransferResult transferBetweenPurses(WalletOwner owner, WalletPurse from, WalletPurse to,
                                                     BigDecimal amount, String currencyCode, String reference,
                                                     String description) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Say which purse to move from and which to move to");
        }
        if (from == to) {
            throw new IllegalArgumentException("Choose two different purses");
        }
        requireFits(owner, from);
        requireFits(owner, to);
        validateAmount(amount);
        String currency = resolveCurrencyCode(currencyCode);
        // Locked in purse order, so two opposite moves of one owner cannot deadlock.
        boolean fromFirst = from.compareTo(to) < 0;
        UserWallet first = lockOrCreateWallet(owner, fromFirst ? from : to, currency);
        UserWallet second = lockOrCreateWallet(owner, fromFirst ? to : from, currency);
        UserWallet source = fromFirst ? first : second;
        UserWallet target = fromFirst ? second : first;

        UUID transferReference = UUID.randomUUID();
        String ref = StringUtils.hasText(reference) ? reference : "purse-transfer:" + transferReference;
        String text = StringUtils.hasText(description) ? description
                : "Moved from the " + label(from) + " purse to the " + label(to) + " purse";
        applyDebit(source, amount, WalletTransactionType.TRANSFER_OUT, ref, text, owner.uuid(), transferReference);
        applyCredit(target, amount, currency, WalletTransactionType.TRANSFER_IN, ref, text, owner.uuid(),
                transferReference);
        recordLedgerTransfer(source, target, amount, currency, text, transferReference);
        return new PurseTransferResult(source, target, transferReference);
    }

    private static String label(WalletPurse purse) {
        return purse.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private List<UserWallet> rowsOf(WalletOwner owner, String currency) {
        return userWalletRepository.findByOwnerTypeAndOwnerUuidAndCurrencyCodeOrderByIdAsc(owner.type(), owner.uuid(),
                currency);
    }

    private Map<WalletPurse, BigDecimal> heldByPurse(WalletOwner owner, String currency) {
        Map<WalletPurse, BigDecimal> held = new EnumMap<>(WalletPurse.class);
        for (Object[] row : withdrawalRepository.sumByPurseAndStatus(owner.type(), owner.uuid(), currency,
                WithdrawalStatus.HELD)) {
            held.put((WalletPurse) row[0], (BigDecimal) row[1]);
        }
        return held;
    }

    private static void requireFits(WalletOwner owner, WalletPurse purse) {
        if (purse == null || !purse.isAllowedFor(owner.type())) {
            throw new IllegalArgumentException("A " + owner.type().name().toLowerCase(Locale.ROOT)
                    + " wallet has no " + purse + " purse");
        }
    }

    private UserWallet applyCredit(
            UUID userUuid,
            WalletPurse purse,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid
    ) {
        validateAmount(amount);
        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        UserWallet wallet = lockOrCreateWallet(WalletOwner.user(userUuid), purse, resolvedCurrency);
        UserWalletTransaction transaction =
                applyCredit(wallet, amount, resolvedCurrency, type, reference, description, counterpartyUserUuid, null);
        recordLedgerCredit(wallet, transaction, amount, resolvedCurrency, type, description);
        return wallet;
    }

    private UserWalletTransaction applyCredit(
            UserWallet wallet,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        BigDecimal balanceBefore = wallet.getBalanceAmount();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalanceAmount(balanceAfter);
        userWalletRepository.save(wallet);

        UserWalletTransaction transaction = buildTransaction(
                wallet,
                type,
                amount,
                currencyCode,
                balanceBefore,
                balanceAfter,
                reference,
                description,
                counterpartyUserUuid,
                transferReference
        );
        transactionRepository.save(transaction);
        recordAudit(wallet, transaction);
        return transaction;
    }

    private UserWalletTransaction applyDebit(
            UserWallet wallet,
            BigDecimal amount,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        validateAmount(amount);
        BigDecimal balanceBefore = wallet.getBalanceAmount();
        if (balanceBefore.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalanceAmount(balanceAfter);
        userWalletRepository.save(wallet);

        UserWalletTransaction transaction = buildTransaction(
                wallet,
                type,
                amount,
                wallet.getCurrencyCode(),
                balanceBefore,
                balanceAfter,
                reference,
                description,
                counterpartyUserUuid,
                transferReference
        );
        transactionRepository.save(transaction);
        recordAudit(wallet, transaction);
        return transaction;
    }

    private UserWalletTransaction buildTransaction(
            UserWallet wallet,
            WalletTransactionType type,
            BigDecimal amount,
            String currencyCode,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setReference(reference);
        transaction.setDescription(description);
        transaction.setCounterpartyUserUuid(counterpartyUserUuid);
        transaction.setTransferReference(transferReference);
        return transaction;
    }

    /** Writes the transaction's audit event in the same database transaction, so neither exists without the other. */
    private void recordAudit(UserWallet wallet, UserWalletTransaction transaction) {
        WalletAuditEvent event = new WalletAuditEvent();
        event.setOwnerType(wallet.getOwnerType());
        event.setOwnerUuid(wallet.getOwnerUuid());
        event.setCurrencyCode(transaction.getCurrencyCode());
        event.setEventType(WalletAuditEventType.forTransaction(transaction.getTransactionType()));
        event.setActorUserUuid(currentUserUuid());
        event.setReference(transaction.getReference());
        event.setReason(transaction.getDescription());
        event.setPreviousValues(Map.of("balance_amount", transaction.getBalanceBefore()));
        event.setUpdatedValues(Map.of(
                "balance_amount", transaction.getBalanceAfter(),
                "amount", transaction.getAmount(),
                "purse", wallet.getPurse().name(),
                "transaction_type", transaction.getTransactionType().name(),
                "transaction_uuid", String.valueOf(transaction.getUuid())));
        auditEventRepository.save(event);
    }

    /** The signed-in user behind a change; null for the system (event listeners, jobs, scripts). */
    private UUID currentUserUuid() {
        DomainSecurityService security = domainSecurityService.getIfAvailable();
        if (security == null) {
            return null;
        }
        try {
            return security.getCurrentUserUuid();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Mirrors a wallet credit into the ledger: the earner's balance is a liability of the platform,
     * so a credit increases it and something has to fund it.
     * <p>
     * A deposit is funded by cash the platform actually received; a sale is funded out of revenue
     * that has come in but not yet been attributed to anybody. Which of the two applies is the only
     * choice made here, and it follows from the wallet transaction type alone - no rule is read, no
     * fee is resolved, nothing outside this module is consulted.
     */
    private void recordLedgerCredit(
            UserWallet wallet,
            UserWalletTransaction transaction,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String description
    ) {
        LedgerAccountRef earner = purseAccount(wallet, currencyCode);
        LedgerAccountRef funding = type == WalletTransactionType.DEPOSIT
                ? LedgerAccountRef.platformCashMpesa(currencyCode)
                : LedgerAccountRef.platformUnallocatedRevenue(currencyCode);

        postToLedger(new LedgerPostingRequest(
                "wallet-txn:" + transaction.getUuid(),
                transaction.getCreatedDate(),
                description,
                CAUSE_WALLET_TRANSACTION,
                transaction.getUuid(),
                List.of(LedgerPostingLeg.debit(funding, amount), LedgerPostingLeg.credit(earner, amount))));
    }

    /** The platform's fee moves from the earner's balance to the platform's own revenue. */
    private void recordLedgerFee(
            UserWallet wallet,
            UserWalletTransaction transaction,
            BigDecimal amount,
            String currencyCode,
            String description
    ) {
        postToLedger(new LedgerPostingRequest(
                "wallet-txn:" + transaction.getUuid(),
                transaction.getCreatedDate(),
                description,
                CAUSE_WALLET_TRANSACTION,
                transaction.getUuid(),
                List.of(LedgerPostingLeg.debit(purseAccount(wallet, currencyCode), amount),
                        LedgerPostingLeg.credit(LedgerAccountRef.platformFeeRevenue(currencyCode), amount))));
    }

    /** The ledger account a wallet row mirrors: the organisation's EARNINGS, or the person's purse. */
    private static LedgerAccountRef purseAccount(UserWallet wallet, String currencyCode) {
        return wallet.getOwnerType() == WalletOwnerType.ORGANISATION
                ? LedgerAccountRef.organisationEarnings(wallet.getOwnerUuid(), currencyCode)
                : LedgerAccountRef.userPurse(wallet.getOwnerUuid(), LedgerPurse.valueOf(wallet.getPurse().name()),
                currencyCode);
    }

    /**
     * A transfer is the one movement that needs no platform account at all: one party's liability
     * falls by exactly what the other's rises.
     */
    private void recordLedgerTransfer(
            UserWallet source,
            UserWallet target,
            BigDecimal amount,
            String currencyCode,
            String description,
            UUID transferReference
    ) {
        LedgerAccountRef from = purseAccount(source, currencyCode);
        LedgerAccountRef to = purseAccount(target, currencyCode);

        postToLedger(new LedgerPostingRequest(
                "wallet-transfer:" + transferReference,
                null,
                description,
                CAUSE_WALLET_TRANSFER,
                transferReference,
                List.of(LedgerPostingLeg.debit(from, amount), LedgerPostingLeg.credit(to, amount))));
    }

    /**
     * Posts after the wallet transaction has committed, never inside it.
     * <p>
     * This is the deliberate trade of the dual-write phase. Doing it inline would put the ledger's
     * constraints on the critical path of getting somebody paid - and a constraint violation marks
     * the whole transaction rollback-only, so even catching it would not save the credit. Posting
     * from {@code afterCommit} means a ledger failure cannot reach the wallet operation at all, at
     * the cost of the two writes not being atomic: a crash in the window between them, or a
     * genuinely rejected posting, leaves the ledger behind. That gap is exactly what
     * {@link apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob} exists to find.
     * <p>
     * When there is no transaction to hang off - which in practice means a unit test - the posting
     * happens inline instead, so the behaviour is still exercised.
     */
    private void postToLedger(LedgerPostingRequest request) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            postSwallowingFailures(request);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                postSwallowingFailures(request);
            }
        });
    }

    private void postSwallowingFailures(LedgerPostingRequest request) {
        try {
            ledgerService.post(request);
        } catch (Exception ex) {
            // Nothing above this can be allowed to fail because the ledger did. The wallet has
            // already committed and stays authoritative; reconciliation reports the divergence.
            log.error("Ledger posting {} failed; user_wallets is unchanged and reconciliation will report this: {}",
                    request.idempotencyKey(), ex.getMessage(), ex);
        }
    }

    private UserWallet lockOrCreateWallet(WalletOwner owner, WalletPurse purse, String currencyCode) {
        requireFits(owner, purse);
        Optional<UserWallet> existing = userWalletRepository.findLocked(owner.type(), owner.uuid(), purse, currencyCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserWallet wallet = new UserWallet();
        wallet.setOwnerType(owner.type());
        wallet.setOwnerUuid(owner.uuid());
        wallet.setPurse(purse);
        wallet.setCurrencyCode(currencyCode);
        wallet.setBalanceAmount(ZERO);

        try {
            userWalletRepository.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Wallet already created for {} {} purse and currency {}", owner, purse, currencyCode, ex);
        }

        return userWalletRepository.findLocked(owner.type(), owner.uuid(), purse, currencyCode)
                .orElseThrow(() -> new IllegalStateException("Failed to create wallet for " + owner.type()));
    }

    private String resolveCurrencyCode(String currencyCode) {
        PlatformCurrency currency = currencyService.resolveCurrencyOrDefault(currencyCode);
        return currency.getCode();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
