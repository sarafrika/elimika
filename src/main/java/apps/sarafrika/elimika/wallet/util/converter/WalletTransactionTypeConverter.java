package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WalletTransactionTypeConverter implements AttributeConverter<WalletTransactionType, String> {
    @Override
    public String convertToDatabaseColumn(WalletTransactionType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WalletTransactionType convertToEntityAttribute(String dbData) {
        return WalletTransactionType.fromString(dbData);
    }
}
