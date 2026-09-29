package apps.sarafrika.elimika.search.internal.state;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = false)
public class SearchIndexStatusConverter implements AttributeConverter<SearchIndexStatus, String> {

    @Override
    public String convertToDatabaseColumn(SearchIndexStatus attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public SearchIndexStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SearchIndexStatus.fromValue(dbData);
    }
}
