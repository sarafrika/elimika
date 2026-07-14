package apps.sarafrika.elimika.resourcing.util.converter;

import apps.sarafrika.elimika.resourcing.spi.AvailabilityRuleType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AvailabilityRuleTypeConverter implements AttributeConverter<AvailabilityRuleType, String> {

    @Override
    public String convertToDatabaseColumn(AvailabilityRuleType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public AvailabilityRuleType convertToEntityAttribute(String dbData) {
        return dbData != null ? AvailabilityRuleType.fromValue(dbData) : null;
    }
}
