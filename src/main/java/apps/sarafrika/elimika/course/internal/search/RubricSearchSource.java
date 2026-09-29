package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Feeds the {@code rubrics} index from {@code assessment_rubrics}. Every rubric is indexed; the scope
 * limits non-admins to public rubrics and their own, and discovery to public, active ones.
 */
@Component
public class RubricSearchSource implements SearchDocumentSource<RubricSearchDocument> {

    public static final String INDEX = "rubrics";
    static final String IS_PUBLIC = "is_public";
    static final String IS_ACTIVE = "is_active";
    static final String COURSE_CREATOR_UUID = "course_creator_uuid";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 2,
                    List.of("title", "rubric_type", "description"),
                    List.of(IS_PUBLIC, IS_ACTIVE, "status", COURSE_CREATOR_UUID, "rubric_type", "usage_count",
                            "uuid", "created_at"),
                    List.of("title", "created_at", "usage_count"))
            .withTypoDisabledAttributes(List.of("status"));

    private static final String RUBRIC_COLUMNS = """
            SELECT r.id, r.uuid, r.title, r.description, r.rubric_type, r.is_public, r.is_active, r.status,
                   r.course_creator_uuid, EXTRACT(EPOCH FROM r.created_date)::bigint AS created_at
            FROM assessment_rubrics r
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RubricSearchSource(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<RubricSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return toDocuments(jdbc.query(RUBRIC_COLUMNS + " WHERE r.uuid IN (:uuids)",
                new MapSqlParameterSource("uuids", uuids), (rs, rowNum) -> row(rs)));
    }

    @Override
    public SearchBatch<RubricSearchDocument> loadAfter(long lastId, int batchSize) {
        List<RubricRow> rows = jdbc.query(RUBRIC_COLUMNS + " WHERE r.id > :lastId ORDER BY r.id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)), (rs, rowNum) -> row(rs));
        return rows.isEmpty() ? SearchBatch.end(lastId) : new SearchBatch<>(toDocuments(rows), rows.getLast().id());
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM assessment_rubrics", Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(AssessmentRubric.class, AssessmentRubric::getUuid),
                SearchIndexTrigger.direct(CourseRubricAssociation.class, CourseRubricAssociation::getRubricUuid));
    }

    private List<RubricSearchDocument> toDocuments(List<RubricRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> usage = new HashMap<>();
        jdbc.query("""
                SELECT rubric_uuid, COUNT(*) AS usage_count
                FROM course_rubric_associations WHERE rubric_uuid IN (:uuids) GROUP BY rubric_uuid
                """, new MapSqlParameterSource("uuids", rows.stream().map(RubricRow::uuid).toList()), rs -> {
            usage.put(SearchRows.uuid(rs, "rubric_uuid"), rs.getLong("usage_count"));
        });
        List<RubricSearchDocument> documents = new ArrayList<>(rows.size());
        for (RubricRow row : rows) {
            documents.add(new RubricSearchDocument(row.uuid(), row.title(), SearchRows.truncate(row.description()),
                    row.rubricType(), row.isPublic(), row.isActive(), row.status(), row.courseCreatorUuid(),
                    usage.getOrDefault(row.uuid(), 0L), row.createdAt()));
        }
        return documents;
    }

    /** Enum-like values are stored lower case, so the engine's exact filters match case-insensitively. */
    private static String lowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static RubricRow row(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new RubricRow(
                rs.getLong("id"),
                SearchRows.uuid(rs, "uuid"),
                rs.getString("title"),
                rs.getString("description"),
                lowerCase(rs.getString("rubric_type")),
                SearchRows.flag(rs, "is_public"),
                SearchRows.flag(rs, "is_active"),
                lowerCase(status),
                SearchRows.uuid(rs, "course_creator_uuid"),
                SearchRows.nullableLong(rs, "created_at"));
    }

    private record RubricRow(
            long id,
            UUID uuid,
            String title,
            String description,
            String rubricType,
            boolean isPublic,
            boolean isActive,
            String status,
            UUID courseCreatorUuid,
            Long createdAt
    ) {
    }
}
