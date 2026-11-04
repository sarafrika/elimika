package apps.sarafrika.elimika.commerce.internal.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.sqids.Sqids;

/**
 * Turns an order-number sequence value into a short code such as {@code ELM-K7Q9PX}.
 * <p>
 * Sqids maps each sequence value to exactly one code, so codes never collide. The shuffled alphabet
 * hides the sequence and must never change once orders exist. The code is the M-Pesa account
 * reference, so it stays within Daraja's 12-character limit.
 */
public final class OrderNumbers {

    public static final String PREFIX = "ELM-";
    /** Upper-case letters and digits without 0, O, 1, I, L and U, which read alike or spell words. */
    public static final String CHARACTERS = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
    public static final String DEFAULT_ALPHABET = "JH5EV3YQK9BNTP6GDMA2SZWR84X7FC";
    public static final String ALPHABET_ENV = "COMMERCE_ORDER_NUMBER_ALPHABET";
    static final int MIN_CODE_LENGTH = 6;

    private final Sqids sqids;

    private OrderNumbers(Sqids sqids) {
        this.sqids = sqids;
    }

    /** A codec over {@code alphabet}, which must be a reordering of {@link #CHARACTERS}. */
    public static OrderNumbers of(String alphabet) {
        String normalised = alphabet == null ? "" : alphabet.trim().toUpperCase(Locale.ROOT);
        if (!sorted(normalised).equals(sorted(CHARACTERS))) {
            throw new IllegalArgumentException(
                    "The order number alphabet must use each of " + CHARACTERS + " exactly once");
        }
        return new OrderNumbers(Sqids.builder().alphabet(normalised).minLength(MIN_CODE_LENGTH).build());
    }

    /** The codec configured by {@value #ALPHABET_ENV}, for code that runs outside Spring. */
    public static OrderNumbers fromEnvironment() {
        String configured = System.getenv(ALPHABET_ENV);
        return of(configured == null || configured.isBlank() ? DEFAULT_ALPHABET : configured);
    }

    public String format(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Order number sequence must not be negative");
        }
        return PREFIX + sqids.encode(List.of(sequence));
    }

    private static String sorted(String value) {
        char[] chars = value.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
