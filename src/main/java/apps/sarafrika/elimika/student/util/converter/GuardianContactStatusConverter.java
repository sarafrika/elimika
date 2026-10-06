package apps.sarafrika.elimika.student.util.converter;

import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GuardianContactStatusConverter implements AttributeConverter<GuardianContactStatus, String> {

    @Override
    public String convertToDatabaseColumn(GuardianContactStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public GuardianContactStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GuardianContactStatus.fromValue(dbData);
    }
}
