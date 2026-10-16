package apps.sarafrika.elimika.timetabling.util.converter;

import apps.sarafrika.elimika.timetabling.spi.InstrumentCondition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InstrumentConditionConverter implements AttributeConverter<InstrumentCondition, String> {

    @Override
    public String convertToDatabaseColumn(InstrumentCondition attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public InstrumentCondition convertToEntityAttribute(String dbData) {
        return dbData != null ? InstrumentCondition.fromValue(dbData) : null;
    }
}
