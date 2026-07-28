package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link ConsentSource}.
 * <p>
 * Follows the project's converter pattern with auto-apply enabled. Reads normalise
 * case-insensitively so legacy or hand-edited data keeps working.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Converter(autoApply = true)
public class ConsentSourceConverter implements AttributeConverter<ConsentSource, String> {

    @Override
    public String convertToDatabaseColumn(ConsentSource attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ConsentSource convertToEntityAttribute(String dbData) {
        return dbData != null ? ConsentSource.fromValue(dbData) : null;
    }
}
