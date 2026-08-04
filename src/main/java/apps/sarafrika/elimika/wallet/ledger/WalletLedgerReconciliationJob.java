package apps.sarafrika.elimika.wallet.ledger;

import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationReport.Divergence;
import apps.sarafrika.elimika.wallet.repository.UserWalletRepository;
import apps.sarafrika.elimika.wallet.repository.WalletLedgerComparison;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Compares every wallet's {@code balance_amount} against the balance derived from its ledger
 * entries, and says so loudly when they differ.
 * <p>
 * This is the safety net for the dual-write: a ledger write that fails is swallowed so it cannot
 * take a wallet credit down with it, which means divergence is possible by design. Divergence that
 * nobody notices is the thing this whole phase exists to eliminate, so it is this job - not the
 * write path - that has to be reliable.
 * <p>
 * The sweep is bounded on purpose. It walks the wallet table by keyset on {@code id}, in batches
 * of {@code batch-size}, and stops after {@code max-batches-per-run} batches; each batch is one
 * query that computes the derived balances alongside the wallets. A table that outgrows one sweep
 * is reported as truncated rather than allowed to hold a transaction open for minutes at a time.
 * The next sweep starts again from the beginning, so nothing is skipped permanently.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletLedgerReconciliationJob {

    private final UserWalletRepository userWalletRepository;

    /** Wallets compared per query. */
    @Value("${wallet.ledger.reconciliation.batch-size:500}")
    private int batchSize;

    /** Ceiling on how much of the table one sweep will walk. */
    @Value("${wallet.ledger.reconciliation.max-batches-per-run:20}")
    private int maxBatchesPerRun;

    @Scheduled(
            initialDelayString = "${wallet.ledger.reconciliation.initial-delay:PT5M}",
            fixedDelayString = "${wallet.ledger.reconciliation.interval:PT1H}")
    public void reconcile() {
        try {
            WalletLedgerReconciliationReport report = runOnce();
            if (report.isClean()) {
                log.info("Wallet/ledger reconciliation clean: {} wallets scanned{}",
                        report.scanned(), report.truncated() ? " (sweep truncated at the batch cap)" : "");
                return;
            }
            log.error("Wallet/ledger reconciliation found {} divergent wallet(s) out of {} scanned",
                    report.divergences().size(), report.scanned());
            for (Divergence divergence : report.divergences()) {
                log.error("Wallet {} (user {}, {}) holds {} but its ledger derives {} - difference {}",
                        divergence.walletUuid(), divergence.userUuid(), divergence.currencyCode(),
                        divergence.walletBalance().toPlainString(), divergence.ledgerBalance().toPlainString(),
                        divergence.difference().toPlainString());
            }
        } catch (Exception ex) {
            // A failed sweep must never kill the scheduler; the next one runs from scratch.
            log.error("Wallet/ledger reconciliation sweep failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Runs one bounded sweep and returns what it found. Separated from the scheduled entry point so
     * the outcome can be asserted on rather than scraped out of a log.
     * <p>
     * Deliberately not wrapped in one transaction: each batch query runs in its own short read-only
     * transaction, so a sweep over a large table never holds a snapshot open across the whole walk.
     */
    public WalletLedgerReconciliationReport runOnce() {
        List<Divergence> divergences = new ArrayList<>();
        long cursor = 0L;
        int scanned = 0;
        boolean truncated = false;

        for (int batch = 0; batch < maxBatchesPerRun; batch++) {
            List<WalletLedgerComparison> page = userWalletRepository.compareWalletsAgainstLedger(cursor, batchSize);
            if (page.isEmpty()) {
                return new WalletLedgerReconciliationReport(scanned, cursor, false, divergences);
            }

            for (WalletLedgerComparison row : page) {
                scanned++;
                cursor = row.getWalletId();
                BigDecimal wallet = row.getWalletBalance();
                BigDecimal ledger = row.getLedgerBalance();
                if (wallet.compareTo(ledger) != 0) {
                    divergences.add(new Divergence(
                            row.getWalletUuid(), row.getUserUuid(), row.getCurrencyCode(), wallet, ledger));
                }
            }

            if (page.size() < batchSize) {
                return new WalletLedgerReconciliationReport(scanned, cursor, false, divergences);
            }
            truncated = true;
        }

        return new WalletLedgerReconciliationReport(scanned, cursor, truncated, divergences);
    }
}
