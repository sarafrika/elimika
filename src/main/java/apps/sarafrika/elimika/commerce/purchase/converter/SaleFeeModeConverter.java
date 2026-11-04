package apps.sarafrika.elimika.commerce.purchase.converter;

import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SaleFeeModeConverter implements AttributeConverter<SaleFeeMode, String> {

    @Override
    public String convertToDatabaseColumn(SaleFeeMode attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SaleFeeMode convertToEntityAttribute(String dbData) {
        return SaleFeeMode.fromString(dbData);
    }
}
