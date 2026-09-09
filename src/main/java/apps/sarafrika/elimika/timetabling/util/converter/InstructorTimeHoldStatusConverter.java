package apps.sarafrika.elimika.timetabling.util.converter;

import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link InstructorTimeHoldStatus} to its column value, reading case-insensitively so
 * legacy or hand-written lower-case rows keep loading.
 */
@Converter(autoApply = true)
public class InstructorTimeHoldStatusConverter implements AttributeConverter<InstructorTimeHoldStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstructorTimeHoldStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public InstructorTimeHoldStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? InstructorTimeHoldStatus.fromValue(dbData) : null;
    }
}
