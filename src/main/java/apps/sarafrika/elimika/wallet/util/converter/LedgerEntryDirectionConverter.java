package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.LedgerEntryDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LedgerEntryDirectionConverter implements AttributeConverter<LedgerEntryDirection, String> {
    @Override
    public String convertToDatabaseColumn(LedgerEntryDirection attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LedgerEntryDirection convertToEntityAttribute(String dbData) {
        return LedgerEntryDirection.fromString(dbData);
    }
}
