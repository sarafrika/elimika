package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PayoutFeePayerConverter implements AttributeConverter<PayoutFeePayer, String> {
    @Override
    public String convertToDatabaseColumn(PayoutFeePayer attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PayoutFeePayer convertToEntityAttribute(String dbData) {
        return PayoutFeePayer.fromString(dbData);
    }
}
