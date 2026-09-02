package apps.sarafrika.elimika.payout.repository;

import java.math.BigDecimal;

/**
 * One month's settled-payout total for an organisation, in a single currency.
 * Year and month come back as integers from {@code year()}/{@code month()} HQL
 * functions; the service formats them into a {@code YYYY-MM} label.
 *
 * @param year         calendar year the obligations were settled in
 * @param month        calendar month (1-12) the obligations were settled in
 * @param currencyCode ISO-4217 currency the amount is denominated in
 * @param amount       sum of settled {@code rate_amount} for that month and currency
 */
public record MonthlyPayoutAggregate(
        Integer year,
        Integer month,
        String currencyCode,
        BigDecimal amount
) {
}
