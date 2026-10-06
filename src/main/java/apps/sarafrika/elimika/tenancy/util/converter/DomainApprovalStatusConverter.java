package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link DomainApprovalStatus}. Reads normalise case-insensitively so
 * legacy or hand-edited rows keep working.
 */
@Converter(autoApply = true)
public class DomainApprovalStatusConverter implements AttributeConverter<DomainApprovalStatus, String> {

    @Override
    public String convertToDatabaseColumn(DomainApprovalStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public DomainApprovalStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? DomainApprovalStatus.fromValue(dbData) : null;
    }
}
