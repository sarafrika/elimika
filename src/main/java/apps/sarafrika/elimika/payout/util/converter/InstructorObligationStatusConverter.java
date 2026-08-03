package apps.sarafrika.elimika.payout.util.converter;

import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link InstructorObligationStatus} as its uppercase name, matching the
 * {@code chk_instructor_obligations_status} check constraint. Reads are case-insensitive so a row
 * written by hand during an incident still loads.
 */
@Converter(autoApply = false)
public class InstructorObligationStatusConverter
        implements AttributeConverter<InstructorObligationStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstructorObligationStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public InstructorObligationStatus convertToEntityAttribute(String dbData) {
        return InstructorObligationStatus.fromString(dbData);
    }
}
