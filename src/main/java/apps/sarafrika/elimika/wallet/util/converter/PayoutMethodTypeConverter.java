package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PayoutMethodTypeConverter implements AttributeConverter<PayoutMethodType, String> {
    @Override
    public String convertToDatabaseColumn(PayoutMethodType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PayoutMethodType convertToEntityAttribute(String dbData) {
        return PayoutMethodType.fromString(dbData);
    }
}
