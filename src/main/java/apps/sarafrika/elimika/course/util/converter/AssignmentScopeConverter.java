package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.AssignmentScope;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AssignmentScopeConverter implements AttributeConverter<AssignmentScope, String> {

    @Override
    public String convertToDatabaseColumn(AssignmentScope attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public AssignmentScope convertToEntityAttribute(String dbData) {
        return dbData != null ? AssignmentScope.fromString(dbData) : null;
    }
}
