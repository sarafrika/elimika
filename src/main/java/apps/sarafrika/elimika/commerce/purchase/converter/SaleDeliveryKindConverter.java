package apps.sarafrika.elimika.commerce.purchase.converter;

import apps.sarafrika.elimika.shared.spi.revenue.SaleDeliveryKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SaleDeliveryKindConverter implements AttributeConverter<SaleDeliveryKind, String> {

    @Override
    public String convertToDatabaseColumn(SaleDeliveryKind attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SaleDeliveryKind convertToEntityAttribute(String dbData) {
        return SaleDeliveryKind.fromString(dbData);
    }
}
