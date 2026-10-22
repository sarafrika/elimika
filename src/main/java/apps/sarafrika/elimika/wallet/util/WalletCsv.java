package apps.sarafrika.elimika.wallet.util;

import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import java.util.List;
import java.util.Objects;

/** Wallet rows as CSV; cells that a spreadsheet would run as a formula are quoted as text. */
public final class WalletCsv {

    private static final String HEADER =
            "Date (UTC),Type,Amount,Currency,Balance before,Balance after,Reference,Description";

    private WalletCsv() {
    }

    public static String transactions(List<UserWalletTransaction> rows) {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (UserWalletTransaction row : rows) {
            csv.append(cell(row.getCreatedDate())).append(',')
                    .append(cell(row.getTransactionType())).append(',')
                    .append(cell(row.getAmount())).append(',')
                    .append(cell(row.getCurrencyCode())).append(',')
                    .append(cell(row.getBalanceBefore())).append(',')
                    .append(cell(row.getBalanceAfter())).append(',')
                    .append(cell(row.getReference())).append(',')
                    .append(cell(row.getDescription())).append('\n');
        }
        return csv.toString();
    }

    private static String cell(Object value) {
        String text = Objects.toString(value, "");
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 && !(value instanceof Number)) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
