package apps.sarafrika.elimika.profile.internal.converter;

import apps.sarafrika.elimika.profile.spi.PortfolioItemType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PortfolioItemTypeConverter implements AttributeConverter<PortfolioItemType, String> {

    @Override
    public String convertToDatabaseColumn(PortfolioItemType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PortfolioItemType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PortfolioItemType.fromValue(dbData);
    }
}
