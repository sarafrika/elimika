package apps.sarafrika.elimika.wallet.ledger;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What one reconciliation sweep found.
 *
 * @param scanned    wallets compared in this sweep
 * @param truncated  true when the sweep hit its batch cap before reaching the end of the table, so
 *                   the wallets past {@code lastWalletId} were not looked at this time
 * @param divergences wallets whose balance does not match their derived ledger balance
 */
public record WalletLedgerReconciliationReport(
        int scanned,
        long lastWalletId,
        boolean truncated,
        List<Divergence> divergences
) {

    public WalletLedgerReconciliationReport {
        divergences = List.copyOf(divergences);
    }

    public boolean isClean() {
        return divergences.isEmpty();
    }

    public record Divergence(
            UUID walletUuid,
            UUID userUuid,
            String currencyCode,
            BigDecimal walletBalance,
            BigDecimal ledgerBalance
    ) {
        public BigDecimal difference() {
            return walletBalance.subtract(ledgerBalance);
        }
    }
}
