package apps.sarafrika.elimika.shared.utils.converter;

import apps.sarafrika.elimika.shared.utils.PhoneNumbers;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Writes phone numbers as E.164 whatever path saved them; reads stored values unchanged.
 */
@Converter(autoApply = false)
public class E164PhoneNumberConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return PhoneNumbers.toE164(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}
