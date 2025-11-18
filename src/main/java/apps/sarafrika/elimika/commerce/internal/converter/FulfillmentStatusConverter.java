package apps.sarafrika.elimika.commerce.internal.converter;

import apps.sarafrika.elimika.commerce.internal.enums.FulfillmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * Persists fulfillment status values as uppercase strings for compatibility.
 */
@Converter(autoApply = false)
public class FulfillmentStatusConverter implements AttributeConverter<FulfillmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(FulfillmentStatus attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public FulfillmentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        for (FulfillmentStatus status : FulfillmentStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown fulfillment status: " + dbData);
    }
}
