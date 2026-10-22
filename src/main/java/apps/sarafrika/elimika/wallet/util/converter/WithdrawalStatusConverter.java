package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WithdrawalStatusConverter implements AttributeConverter<WithdrawalStatus, String> {
    @Override
    public String convertToDatabaseColumn(WithdrawalStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WithdrawalStatus convertToEntityAttribute(String dbData) {
        return WithdrawalStatus.fromString(dbData);
    }
}
