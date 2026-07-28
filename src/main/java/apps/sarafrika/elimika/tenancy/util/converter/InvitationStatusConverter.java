package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link InvitationStatus}.
 * <p>
 * Follows the project's converter pattern with auto-apply enabled. Reads normalise
 * case-insensitively so legacy or hand-edited data keeps working.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Converter(autoApply = true)
public class InvitationStatusConverter implements AttributeConverter<InvitationStatus, String> {

    @Override
    public String convertToDatabaseColumn(InvitationStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public InvitationStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? InvitationStatus.fromValue(dbData) : null;
    }
}
