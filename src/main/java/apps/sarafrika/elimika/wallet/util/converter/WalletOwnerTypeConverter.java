package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WalletOwnerTypeConverter implements AttributeConverter<WalletOwnerType, String> {

    @Override
    public String convertToDatabaseColumn(WalletOwnerType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WalletOwnerType convertToEntityAttribute(String dbData) {
        return WalletOwnerType.fromString(dbData);
    }
}
