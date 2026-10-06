package apps.sarafrika.elimika.coursecreator.util.converter;

import apps.sarafrika.elimika.coursecreator.util.enums.WalletVerificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WalletVerificationStatusConverter implements AttributeConverter<WalletVerificationStatus, String> {

    @Override
    public String convertToDatabaseColumn(WalletVerificationStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public WalletVerificationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : WalletVerificationStatus.fromValue(dbData);
    }
}
