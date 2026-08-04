package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.LedgerAccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LedgerAccountStatusConverter implements AttributeConverter<LedgerAccountStatus, String> {
    @Override
    public String convertToDatabaseColumn(LedgerAccountStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LedgerAccountStatus convertToEntityAttribute(String dbData) {
        return LedgerAccountStatus.fromString(dbData);
    }
}
