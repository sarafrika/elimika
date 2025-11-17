package apps.sarafrika.elimika.instructor.util.converter;

import apps.sarafrika.elimika.instructor.util.enums.ProficiencyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Attribute converter for ProficiencyLevel.
 *
 * Currently not auto-applied because InstructorSkill is mapped directly as a
 * PostgreSQL named enum using @Enumerated and @JdbcTypeCode. This converter is
 * retained for potential future use where ProficiencyLevel needs to be stored
 * as a normalized string column.
 */
@Converter(autoApply = false)
public class ProficiencyLevelConverter implements AttributeConverter<ProficiencyLevel, String> {

    @Override
    public String convertToDatabaseColumn(ProficiencyLevel attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProficiencyLevel convertToEntityAttribute(String dbData) {
        return dbData != null ? ProficiencyLevel.fromValue(dbData) : null;
    }
}
