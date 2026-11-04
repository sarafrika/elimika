package apps.sarafrika.elimika.commerce.purchase.converter;

import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SaleLegStatusConverter implements AttributeConverter<SaleLegStatus, String> {

    @Override
    public String convertToDatabaseColumn(SaleLegStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SaleLegStatus convertToEntityAttribute(String dbData) {
        return SaleLegStatus.fromString(dbData);
    }
}
