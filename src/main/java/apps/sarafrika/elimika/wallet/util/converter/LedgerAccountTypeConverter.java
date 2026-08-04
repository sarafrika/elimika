package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.LedgerAccountType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LedgerAccountTypeConverter implements AttributeConverter<LedgerAccountType, String> {
    @Override
    public String convertToDatabaseColumn(LedgerAccountType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LedgerAccountType convertToEntityAttribute(String dbData) {
        return LedgerAccountType.fromString(dbData);
    }
}
