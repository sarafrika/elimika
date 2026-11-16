package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.shared.enums.JobMembershipPreference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class JobMembershipPreferenceConverter implements AttributeConverter<JobMembershipPreference, String> {

    @Override
    public String convertToDatabaseColumn(JobMembershipPreference attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public JobMembershipPreference convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank()
                ? null
                : JobMembershipPreference.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
