package apps.sarafrika.elimika.commerce.internal.converter;

import apps.sarafrika.elimika.commerce.internal.enums.VariantStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * Persists variant status values as uppercase strings for compatibility.
 */
@Converter(autoApply = false)
public class VariantStatusConverter implements AttributeConverter<VariantStatus, String> {

    @Override
    public String convertToDatabaseColumn(VariantStatus attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public VariantStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        for (VariantStatus status : VariantStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown variant status: " + dbData);
    }
}
