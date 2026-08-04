package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link SkillsFundTransactionStatus}.
 * <p>
 * Follows the project's converter pattern with auto-apply enabled. Reads normalise case-insensitively
 * and fold the legacy {@code Completed} synonym, so a row written before the enum existed — or edited
 * by hand since — still loads.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-08-04
 */
@Converter(autoApply = true)
public class SkillsFundTransactionStatusConverter
        implements AttributeConverter<SkillsFundTransactionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SkillsFundTransactionStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public SkillsFundTransactionStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SkillsFundTransactionStatus.fromValue(dbData) : null;
    }
}
