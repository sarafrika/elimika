package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Feeds the {@code people} index: one document per user, with the domains and organisation
 * memberships that people-search scopes filter on.
 * <p>
 * {@code is_platform_admin} and {@code is_org_admin} follow {@code AdminService#isAdmin} and
 * {@code UserRepository#findAdminEligibleUsers} exactly: the global {@code admin} domain, and an
 * active, non-deleted {@code organisation_user} organisation mapping.
 */
@Component
@RequiredArgsConstructor
class PeopleSearchSource implements SearchDocumentSource<PeopleSearchDocument> {

    static final String INDEX = "people";

    static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 2,
                    List.of("full_name", "first_name", "last_name", "email", "username", "user_no"),
                    List.of("domains", "organisation_uuids", "branch_uuids", "active", "is_platform_admin",
                            "is_org_admin", "uuid", "created_at", "email_normalized"),
                    List.of("full_name", "created_at"))
            .withTypoDisabledAttributes(List.of("email", "user_no", "username"));

    private static final String PLATFORM_ADMIN_DOMAIN = "admin";
    private static final String ORG_ADMIN_DOMAIN = "organisation_user";

    private static final String USER_COLUMNS =
            "id, uuid, first_name, middle_name, last_name, email, username, user_no, active, created_date";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<PeopleSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        List<UserRow> users = jdbc.query("SELECT " + USER_COLUMNS + " FROM users WHERE uuid IN (:uuids)",
                new MapSqlParameterSource("uuids", uuids), (rs, row) -> userRow(rs));
        return toDocuments(users);
    }

    @Override
    public SearchBatch<PeopleSearchDocument> loadAfter(long lastId, int batchSize) {
        List<UserRow> users = jdbc.query(
                "SELECT " + USER_COLUMNS + " FROM users WHERE id > :lastId ORDER BY id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)),
                (rs, row) -> userRow(rs));
        if (users.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        return new SearchBatch<>(toDocuments(users), users.getLast().id());
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(User.class, User::getUuid),
                SearchIndexTrigger.direct(UserDomainMapping.class, UserDomainMapping::getUserUuid),
                SearchIndexTrigger.direct(UserOrganisationDomainMapping.class, UserOrganisationDomainMapping::getUserUuid));
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject("SELECT count(*) FROM users", Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    private List<PeopleSearchDocument> toDocuments(List<UserRow> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<UUID> uuids = users.stream().map(UserRow::uuid).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("uuids", uuids);

        Map<UUID, Memberships> memberships = new LinkedHashMap<>();
        jdbc.query("""
                        SELECT udm.user_uuid, ud.domain_name
                        FROM user_domain_mapping udm
                        JOIN user_domain ud ON ud.uuid = udm.domain_uuid
                        WHERE udm.user_uuid IN (:uuids)
                        """, params,
                rs -> {
                    Memberships member = memberships.computeIfAbsent(rs.getObject("user_uuid", UUID.class),
                            ignored -> new Memberships());
                    String domain = rs.getString("domain_name");
                    if (domain != null) {
                        member.domains.add(domain);
                        member.platformAdmin |= PLATFORM_ADMIN_DOMAIN.equals(domain);
                    }
                });
        jdbc.query("""
                        SELECT m.user_uuid, m.organisation_uuid, m.branch_uuid, ud.domain_name
                        FROM user_organisation_domain_mapping m
                        LEFT JOIN user_domain ud ON ud.uuid = m.domain_uuid
                        WHERE m.user_uuid IN (:uuids)
                          AND m.active = true
                          AND m.deleted = false
                        """, params,
                rs -> {
                    Memberships member = memberships.computeIfAbsent(rs.getObject("user_uuid", UUID.class),
                            ignored -> new Memberships());
                    UUID organisationUuid = rs.getObject("organisation_uuid", UUID.class);
                    UUID branchUuid = rs.getObject("branch_uuid", UUID.class);
                    String domain = rs.getString("domain_name");
                    if (organisationUuid != null) {
                        member.organisations.add(organisationUuid);
                    }
                    if (branchUuid != null) {
                        member.branches.add(branchUuid);
                    }
                    if (domain != null) {
                        member.domains.add(domain);
                        member.orgAdmin |= ORG_ADMIN_DOMAIN.equals(domain);
                    }
                });

        List<PeopleSearchDocument> documents = new ArrayList<>(users.size());
        for (UserRow user : users) {
            Memberships member = memberships.getOrDefault(user.uuid(), new Memberships());
            documents.add(new PeopleSearchDocument(
                    user.uuid(),
                    user.firstName(),
                    user.middleName(),
                    user.lastName(),
                    PeopleSearchDocument.fullName(user.firstName(), user.middleName(), user.lastName()),
                    user.email() == null ? null : user.email().trim().toLowerCase(Locale.ROOT),
                    user.username(),
                    user.userNo(),
                    List.copyOf(member.domains),
                    List.copyOf(member.organisations),
                    List.copyOf(member.branches),
                    user.active(),
                    member.platformAdmin,
                    member.orgAdmin,
                    user.createdAt()));
        }
        return documents;
    }

    private static UserRow userRow(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getObject("created_date", LocalDateTime.class);
        return new UserRow(
                rs.getLong("id"),
                rs.getObject("uuid", UUID.class),
                rs.getString("first_name"),
                rs.getString("middle_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("user_no"),
                rs.getBoolean("active"),
                created == null ? null : created.toEpochSecond(ZoneOffset.UTC));
    }

    private record UserRow(
            long id,
            UUID uuid,
            String firstName,
            String middleName,
            String lastName,
            String email,
            String username,
            String userNo,
            boolean active,
            Long createdAt
    ) {
    }

    private static final class Memberships {
        private final Set<String> domains = new LinkedHashSet<>();
        private final Set<UUID> organisations = new LinkedHashSet<>();
        private final Set<UUID> branches = new LinkedHashSet<>();
        private boolean platformAdmin;
        private boolean orgAdmin;
    }
}
