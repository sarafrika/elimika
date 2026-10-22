package apps.sarafrika.elimika.wallet.repository;

import java.math.BigDecimal;

/** A month of one wallet's activity, as the monthly statement query returns it. */
public interface MonthlyWalletActivity {

    String getMonth();

    BigDecimal getMoneyIn();

    BigDecimal getMoneyOut();

    long getTransactionCount();

    BigDecimal getOpeningBalance();

    BigDecimal getClosingBalance();
}
