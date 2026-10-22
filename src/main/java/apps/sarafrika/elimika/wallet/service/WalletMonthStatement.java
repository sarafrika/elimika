package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;
import java.time.YearMonth;

/** One calendar month (UTC) of a wallet: where it started, what came in and went out, where it ended. */
public record WalletMonthStatement(YearMonth month, BigDecimal openingBalance, BigDecimal moneyIn,
                                   BigDecimal moneyOut, BigDecimal closingBalance, long transactionCount) {
}
