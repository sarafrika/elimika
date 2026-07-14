package apps.sarafrika.elimika.resourcing.util.converter;

import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResourceTypeConverter implements AttributeConverter<ResourceType, String> {

    @Override
    public String convertToDatabaseColumn(ResourceType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ResourceType convertToEntityAttribute(String dbData) {
        return dbData != null ? ResourceType.fromValue(dbData) : null;
    }
}
