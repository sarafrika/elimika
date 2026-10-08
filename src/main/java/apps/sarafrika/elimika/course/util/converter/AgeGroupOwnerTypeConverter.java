package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AgeGroupOwnerTypeConverter implements AttributeConverter<AgeGroupOwnerType, String> {

    @Override
    public String convertToDatabaseColumn(AgeGroupOwnerType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AgeGroupOwnerType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AgeGroupOwnerType.fromValue(dbData);
    }
}
