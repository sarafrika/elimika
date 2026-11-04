package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.service.WalletPurse;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WalletPurseConverter implements AttributeConverter<WalletPurse, String> {

    @Override
    public String convertToDatabaseColumn(WalletPurse attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WalletPurse convertToEntityAttribute(String dbData) {
        return WalletPurse.fromString(dbData);
    }
}
