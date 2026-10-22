package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PayoutMethodStatusConverter implements AttributeConverter<PayoutMethodStatus, String> {
    @Override
    public String convertToDatabaseColumn(PayoutMethodStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PayoutMethodStatus convertToEntityAttribute(String dbData) {
        return PayoutMethodStatus.fromString(dbData);
    }
}
