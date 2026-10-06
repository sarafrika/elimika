package apps.sarafrika.elimika.coursecreator.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Kind of credential held in the credentials vault. */
public enum CredentialType {
    CERTIFICATE,
    BADGE,
    AWARD,
    EXTERNAL_CREDENTIAL;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static CredentialType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CredentialType.valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CredentialType: " + value);
        }
    }
}
