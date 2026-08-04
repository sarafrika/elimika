package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.LedgerOwnerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LedgerOwnerTypeConverter implements AttributeConverter<LedgerOwnerType, String> {
    @Override
    public String convertToDatabaseColumn(LedgerOwnerType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LedgerOwnerType convertToEntityAttribute(String dbData) {
        return LedgerOwnerType.fromString(dbData);
    }
}
