package apps.sarafrika.elimika.shared.otp;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OneTimeCodePurposeConverter implements AttributeConverter<OneTimeCodePurpose, String> {
    @Override
    public String convertToDatabaseColumn(OneTimeCodePurpose attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OneTimeCodePurpose convertToEntityAttribute(String dbData) {
        return OneTimeCodePurpose.fromString(dbData);
    }
}
