package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * One organisation as the {@code organisations} index holds it. The licence number, coordinates
 * and contact details are never indexed.
 */
public record OrganisationSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @JsonProperty("slug") String slug,
        @JsonProperty("description") String description,
        @JsonProperty("location") String location,
        @JsonProperty("country") String country,
        @JsonProperty("active") boolean active,
        @JsonProperty("admin_verified") boolean adminVerified,
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {
}
