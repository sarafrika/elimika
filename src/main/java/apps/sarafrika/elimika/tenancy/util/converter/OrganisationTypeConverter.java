package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.OrganisationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link OrganisationType}; reads normalise case-insensitively. The column has no CHECK, so
 * a value since removed from the enum reads as null rather than failing the whole organisation.
 */
@Converter(autoApply = true)
public class OrganisationTypeConverter implements AttributeConverter<OrganisationType, String> {

    @Override
    public String convertToDatabaseColumn(OrganisationType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public OrganisationType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OrganisationType.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
