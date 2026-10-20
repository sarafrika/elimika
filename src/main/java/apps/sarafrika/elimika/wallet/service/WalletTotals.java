package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;

/** A wallet's lifetime figures, summed in the database: sale shares, fees on them, sales, money sent out. */
public record WalletTotals(BigDecimal sharesTotal, BigDecimal platformFeesTotal, long salesCount,
                           BigDecimal transferredOutTotal) {

    public static WalletTotals none() {
        return new WalletTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }
}
