package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** How an organisation receives money; M-Pesa (Kenya) only for now. */
public enum OrganisationPayoutChannel {

    MPESA_PAYBILL("MPESA_PAYBILL", "M-Pesa Paybill"),
    MPESA_TILL("MPESA_TILL", "M-Pesa Till (Buy Goods)");

    private final String value;
    private final String description;

    OrganisationPayoutChannel(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static OrganisationPayoutChannel fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OrganisationPayoutChannel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown OrganisationPayoutChannel: " + value);
        }
    }
}
