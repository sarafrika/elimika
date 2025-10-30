package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClassAssessmentReleaseStrategyConverter implements AttributeConverter<ClassAssessmentReleaseStrategy, String> {

    @Override
    public String convertToDatabaseColumn(ClassAssessmentReleaseStrategy attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ClassAssessmentReleaseStrategy convertToEntityAttribute(String dbData) {
        return dbData != null ? ClassAssessmentReleaseStrategy.fromString(dbData) : null;
    }
}
