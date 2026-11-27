package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.tenancy.util.enums.OrganisationPayoutChannel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA AttributeConverter for {@link OrganisationPayoutChannel}; reads normalise case-insensitively. */
@Converter(autoApply = true)
public class OrganisationPayoutChannelConverter implements AttributeConverter<OrganisationPayoutChannel, String> {

    @Override
    public String convertToDatabaseColumn(OrganisationPayoutChannel attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public OrganisationPayoutChannel convertToEntityAttribute(String dbData) {
        return dbData != null ? OrganisationPayoutChannel.fromValue(dbData) : null;
    }
}
