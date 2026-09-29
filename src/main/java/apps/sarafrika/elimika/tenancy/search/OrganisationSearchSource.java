package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Feeds the {@code organisations} index. Soft-deleted organisations are never indexed: when one is
 * deleted its trigger fires, {@link #loadByUuids} no longer returns it, and the document is removed.
 */
@Component
@RequiredArgsConstructor
class OrganisationSearchSource implements SearchDocumentSource<OrganisationSearchDocument> {

    static final String INDEX = "organisations";

    static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 1,
            List.of("name", "slug", "location", "description"),
            List.of("active", "admin_verified", "country", "uuid", "created_at"),
            List.of("name", "created_at"));

    private static final String COLUMNS =
            "id, uuid, name, slug, description, location, country, active, admin_verified, deleted, created_date";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<OrganisationSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM organisation "
                        + "WHERE uuid IN (:uuids) AND COALESCE(deleted, false) = false",
                new MapSqlParameterSource("uuids", uuids), (rs, row) -> toDocument(rs));
    }

    @Override
    public SearchBatch<OrganisationSearchDocument> loadAfter(long lastId, int batchSize) {
        List<Long> ids = new ArrayList<>();
        List<OrganisationSearchDocument> documents = new ArrayList<>();
        jdbc.query("SELECT " + COLUMNS + " FROM organisation WHERE id > :lastId ORDER BY id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)),
                rs -> {
                    ids.add(rs.getLong("id"));
                    if (!rs.getBoolean("deleted")) {
                        documents.add(toDocument(rs));
                    }
                });
        return ids.isEmpty() ? SearchBatch.end(lastId) : new SearchBatch<>(documents, ids.getLast());
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(SearchIndexTrigger.direct(Organisation.class, Organisation::getUuid));
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM organisation WHERE COALESCE(deleted, false) = false", Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    private static OrganisationSearchDocument toDocument(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getObject("created_date", LocalDateTime.class);
        return new OrganisationSearchDocument(
                rs.getObject("uuid", UUID.class),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getString("location"),
                rs.getString("country"),
                rs.getBoolean("active"),
                rs.getBoolean("admin_verified"),
                created == null ? null : created.toEpochSecond(ZoneOffset.UTC));
    }
}
