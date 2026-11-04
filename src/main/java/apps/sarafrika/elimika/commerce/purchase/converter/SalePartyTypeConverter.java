package apps.sarafrika.elimika.commerce.purchase.converter;

import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SalePartyTypeConverter implements AttributeConverter<SalePartyType, String> {

    @Override
    public String convertToDatabaseColumn(SalePartyType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SalePartyType convertToEntityAttribute(String dbData) {
        return SalePartyType.fromString(dbData);
    }
}
