package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * One user as the {@code people} index holds them.
 * <p>
 * Deliberately narrow: identity and the attributes a scope needs, nothing more. Phone number, date
 * of birth, gender, Keycloak id, guardian links and the profile image are never indexed, so a search
 * engine compromise or a mis-scoped query cannot disclose them.
 */
public record PeopleSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("middle_name") String middleName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("email") String email,
        @JsonProperty("username") String username,
        @JsonProperty("user_no") String userNo,
        @JsonProperty("domains") List<String> domains,
        @JsonProperty("organisation_uuids") List<UUID> organisationUuids,
        @JsonProperty("branch_uuids") List<UUID> branchUuids,
        @JsonProperty("active") boolean active,
        @JsonProperty("is_platform_admin") boolean isPlatformAdmin,
        @JsonProperty("is_org_admin") boolean isOrgAdmin,
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {

    public PeopleSearchDocument {
        domains = domains == null ? List.of() : List.copyOf(domains);
        organisationUuids = organisationUuids == null ? List.of() : List.copyOf(organisationUuids);
        branchUuids = branchUuids == null ? List.of() : List.copyOf(branchUuids);
    }

    /** First, middle and last name joined by single spaces, skipping blank parts. */
    static String fullName(String firstName, String middleName, String lastName) {
        StringBuilder name = new StringBuilder();
        for (String part : new String[]{firstName, middleName, lastName}) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(part.trim().replaceAll("\\s+", " "));
        }
        return name.toString();
    }
}
