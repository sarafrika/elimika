package apps.sarafrika.elimika.shared.otp;

import java.util.Locale;

/** What a code unlocks; a code issued for one purpose never verifies another. Listed in chk_one_time_code_purpose. */
public enum OneTimeCodePurpose {
    PAYOUT_METHOD_VERIFICATION,
    WITHDRAWAL_CONFIRMATION;

    public static OneTimeCodePurpose fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OneTimeCodePurpose.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
