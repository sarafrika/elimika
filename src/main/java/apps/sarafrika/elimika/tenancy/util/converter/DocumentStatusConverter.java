package apps.sarafrika.elimika.tenancy.util.converter;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Maps {@link DocumentStatus} to the plain VARCHAR column used by organisation_documents.
 * Not auto-applied: instructor_documents still stores the value in a Postgres enum type.
 */
@Converter
public class DocumentStatusConverter implements AttributeConverter<DocumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(DocumentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DocumentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return DocumentStatus.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
