package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.OrganisationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA AttributeConverter for {@link OrganisationType}; reads normalise case-insensitively. */
@Converter(autoApply = true)
public class OrganisationTypeConverter implements AttributeConverter<OrganisationType, String> {

    @Override
    public String convertToDatabaseColumn(OrganisationType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public OrganisationType convertToEntityAttribute(String dbData) {
        return dbData != null ? OrganisationType.fromValue(dbData) : null;
    }
}
