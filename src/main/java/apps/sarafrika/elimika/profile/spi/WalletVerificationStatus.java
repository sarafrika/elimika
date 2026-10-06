package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Admin verification of a skills wallet item; only admins move it off PENDING. */
public enum WalletVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static WalletVerificationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WalletVerificationStatus.valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown WalletVerificationStatus: " + value);
        }
    }
}
