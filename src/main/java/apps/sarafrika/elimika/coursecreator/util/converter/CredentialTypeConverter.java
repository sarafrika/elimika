package apps.sarafrika.elimika.coursecreator.util.converter;

import apps.sarafrika.elimika.coursecreator.util.enums.CredentialType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CredentialTypeConverter implements AttributeConverter<CredentialType, String> {

    @Override
    public String convertToDatabaseColumn(CredentialType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CredentialType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CredentialType.fromValue(dbData);
    }
}
