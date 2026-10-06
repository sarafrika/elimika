package apps.sarafrika.elimika.student.util.converter;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Applied explicitly: student_guardian_links still maps the same enum with {@code @Enumerated}. */
@Converter
public class GuardianRelationshipTypeConverter implements AttributeConverter<GuardianRelationshipType, String> {

    @Override
    public String convertToDatabaseColumn(GuardianRelationshipType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public GuardianRelationshipType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GuardianRelationshipType.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
