package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.QuizScope;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuizScopeConverter implements AttributeConverter<QuizScope, String> {

    @Override
    public String convertToDatabaseColumn(QuizScope attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public QuizScope convertToEntityAttribute(String dbData) {
        return dbData != null ? QuizScope.fromString(dbData) : null;
    }
}
