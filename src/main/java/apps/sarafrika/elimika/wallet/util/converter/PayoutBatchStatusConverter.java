package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PayoutBatchStatusConverter implements AttributeConverter<PayoutBatchStatus, String> {
    @Override
    public String convertToDatabaseColumn(PayoutBatchStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PayoutBatchStatus convertToEntityAttribute(String dbData) {
        return PayoutBatchStatus.fromString(dbData);
    }
}
