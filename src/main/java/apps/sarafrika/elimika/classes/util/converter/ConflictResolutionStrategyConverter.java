package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ConflictResolutionStrategyConverter implements AttributeConverter<ConflictResolutionStrategy, String> {

    @Override
    public String convertToDatabaseColumn(ConflictResolutionStrategy attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public ConflictResolutionStrategy convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ConflictResolutionStrategy.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
