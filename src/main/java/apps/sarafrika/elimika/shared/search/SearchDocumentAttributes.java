package apps.sarafrika.elimika.shared.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * The attribute names a document record writes, read from its {@code @JsonProperty} annotations.
 * <p>
 * Used to build an explicit {@code displayedAttributes} list that leaves {@code _geo} out: an index
 * that stores a location for geo filtering and distance sorting must never hand the coordinates
 * back in a hit. The engine still reports {@code _geoDistance}, which callers turn into a band.
 */
public final class SearchDocumentAttributes {

    private SearchDocumentAttributes() {
    }

    /** Every attribute of {@code documentType} except {@link SearchIndexDefinition#GEO_ATTRIBUTE}. */
    public static List<String> displayedWithoutGeo(Class<? extends Record> documentType) {
        List<String> attributes = new ArrayList<>();
        for (RecordComponent component : documentType.getRecordComponents()) {
            // @JsonProperty targets fields, methods and parameters, so it is read from the accessor.
            JsonProperty property = component.getAccessor().getAnnotation(JsonProperty.class);
            String name = property == null || property.value().isEmpty() ? component.getName() : property.value();
            if (!SearchIndexDefinition.GEO_ATTRIBUTE.equals(name)) {
                attributes.add(name);
            }
        }
        return List.copyOf(attributes);
    }
}
