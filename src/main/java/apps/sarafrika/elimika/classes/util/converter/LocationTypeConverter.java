package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.shared.enums.LocationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the LocationType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the LocationType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class LocationTypeConverter implements AttributeConverter<LocationType, String> {

    /**
     * Converts the LocationType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., LocationType.ONLINE).
     * @return The string value for the database (e.g., "ONLINE"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(LocationType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding LocationType enum.
     *
     * @param dbData The string value from the database (e.g., "ONLINE").
     * @return The matching LocationType enum, or null if the database value is null.
     */
    @Override
    public LocationType convertToEntityAttribute(String dbData) {
        return dbData != null ? LocationType.fromValue(dbData) : null;
    }
}