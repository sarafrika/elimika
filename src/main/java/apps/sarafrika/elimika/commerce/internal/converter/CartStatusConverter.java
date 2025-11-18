package apps.sarafrika.elimika.commerce.internal.converter;

import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * Persists cart status values as uppercase strings for compatibility.
 */
@Converter(autoApply = false)
public class CartStatusConverter implements AttributeConverter<CartStatus, String> {

    @Override
    public String convertToDatabaseColumn(CartStatus attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public CartStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        for (CartStatus status : CartStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown cart status: " + dbData);
    }
}
