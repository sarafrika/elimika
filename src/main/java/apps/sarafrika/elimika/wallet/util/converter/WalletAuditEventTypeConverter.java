package apps.sarafrika.elimika.wallet.util.converter;

import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WalletAuditEventTypeConverter implements AttributeConverter<WalletAuditEventType, String> {
    @Override
    public String convertToDatabaseColumn(WalletAuditEventType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WalletAuditEventType convertToEntityAttribute(String dbData) {
        return WalletAuditEventType.fromString(dbData);
    }
}
