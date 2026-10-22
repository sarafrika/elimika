package apps.sarafrika.elimika.shared.sms;

import java.util.Optional;
import java.util.regex.Pattern;

/** Kenyan mobile numbers: one stored shape, 2547XXXXXXXX or 2541XXXXXXXX, and a masked one for display. */
public final class Msisdn {

    private static final Pattern NATIONAL = Pattern.compile("[17]\\d{8}");

    private Msisdn() {
    }

    /** Accepts 07…, 01…, 2547…, +254 7… with spaces or dashes; empty when it is not a Kenyan mobile number. */
    public static Optional<String> normalise(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-()+]", "");
        if (!digits.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (digits.startsWith("254")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return NATIONAL.matcher(digits).matches() ? Optional.of("254" + digits) : Optional.empty();
    }

    /** "+254 7•• ••• 678": enough to recognise a number without exposing it. */
    public static String mask(String msisdn) {
        if (msisdn == null || msisdn.length() != 12) {
            return "••••";
        }
        return "+254 " + msisdn.charAt(3) + "•• ••• " + msisdn.substring(9);
    }

    /** "+254 712 345 678". */
    public static String pretty(String msisdn) {
        if (msisdn == null || msisdn.length() != 12) {
            return msisdn;
        }
        return "+254 " + msisdn.substring(3, 6) + " " + msisdn.substring(6, 9) + " " + msisdn.substring(9);
    }
}
