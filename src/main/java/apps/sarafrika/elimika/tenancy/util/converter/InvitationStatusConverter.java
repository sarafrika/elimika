package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.entity.Invitation;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InvitationStatusConverter implements AttributeConverter<Invitation.InvitationStatus, String> {

    @Override
    public String convertToDatabaseColumn(Invitation.InvitationStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public Invitation.InvitationStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? Invitation.InvitationStatus.fromValue(dbData) : null;
    }
}