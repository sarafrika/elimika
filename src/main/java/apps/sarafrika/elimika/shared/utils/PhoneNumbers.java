package apps.sarafrika.elimika.shared.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * Normalizes phone numbers to E.164 (e.g. {@code +254712345678}) with libphonenumber.
 * Local numbers are read as Kenyan; values libphonenumber cannot validate are kept as given.
 */
public final class PhoneNumbers {

    public static final String DEFAULT_REGION = "KE";

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private PhoneNumbers() {
    }

    public static String toE164(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            Phonenumber.PhoneNumber parsed = UTIL.parse(trimmed, DEFAULT_REGION);
            return UTIL.isValidNumber(parsed) ? UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164) : trimmed;
        } catch (NumberParseException e) {
            return trimmed;
        }
    }
}
