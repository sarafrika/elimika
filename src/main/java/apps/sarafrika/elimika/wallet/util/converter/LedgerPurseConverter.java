package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LedgerPurseConverter implements AttributeConverter<LedgerPurse, String> {
    @Override
    public String convertToDatabaseColumn(LedgerPurse attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LedgerPurse convertToEntityAttribute(String dbData) {
        return LedgerPurse.fromString(dbData);
    }
}
