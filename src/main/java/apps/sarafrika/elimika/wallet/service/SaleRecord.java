package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;

/** A sale credit as it stands in a wallet: who was paid, how much and in what currency. */
public record SaleRecord(WalletOwner owner, BigDecimal amount, String currencyCode) {
}
