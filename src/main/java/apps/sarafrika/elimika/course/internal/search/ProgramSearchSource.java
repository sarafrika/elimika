package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Feeds the {@code programs} index from {@code training_programs}. Every program is indexed; the
 * scope limits non-admins to live programs and their own.
 * <p>
 * The price is stored for display only: like the SQL search, it is not a filter or a sort (use
 * {@code is_free}).
 */
@Component
public class ProgramSearchSource implements SearchDocumentSource<ProgramSearchDocument> {

    public static final String INDEX = "programs";
    static final String IS_PUBLIC = "is_public";
    static final String COURSE_CREATOR_UUID = "course_creator_uuid";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 1,
                    List.of("title", "course_names", "category_name", "creator_name", "description"),
                    List.of("status", "is_published", "admin_approved", "active", IS_PUBLIC, COURSE_CREATOR_UUID,
                            "category_uuid", "is_free", "uuid", "created_at"),
                    List.of("title", "created_at"))
            .withTypoDisabledAttributes(List.of("status"));

    private static final String PROGRAM_COLUMNS = """
            SELECT p.id, p.uuid, p.title, p.description, p.category_uuid, cat.name AS category_name,
                   p.course_creator_uuid, p.status, p.is_published, p.admin_approved, p.is_active, p.price,
                   EXTRACT(EPOCH FROM p.created_date)::bigint AS created_at
            FROM training_programs p
            LEFT JOIN course_categories cat ON cat.uuid = p.category_uuid
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CourseCreatorLookupService courseCreatorLookupService;

    public ProgramSearchSource(NamedParameterJdbcTemplate jdbc, CourseCreatorLookupService courseCreatorLookupService) {
        this.jdbc = jdbc;
        this.courseCreatorLookupService = courseCreatorLookupService;
    }

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<ProgramSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return toDocuments(jdbc.query(PROGRAM_COLUMNS + " WHERE p.uuid IN (:uuids)",
                new MapSqlParameterSource("uuids", uuids), (rs, rowNum) -> row(rs)));
    }

    @Override
    public SearchBatch<ProgramSearchDocument> loadAfter(long lastId, int batchSize) {
        List<ProgramRow> rows = jdbc.query(PROGRAM_COLUMNS + " WHERE p.id > :lastId ORDER BY p.id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)), (rs, rowNum) -> row(rs));
        return rows.isEmpty() ? SearchBatch.end(lastId) : new SearchBatch<>(toDocuments(rows), rows.getLast().id());
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM training_programs", Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(TrainingProgram.class, TrainingProgram::getUuid),
                SearchIndexTrigger.direct(ProgramCourse.class, ProgramCourse::getProgramUuid),
                // A member course's name is part of the document.
                SearchIndexTrigger.fanOut(Course.class, course -> course.getUuid() == null
                        || course.getParentCourseUuid() != null ? null : "course:" + course.getUuid()),
                SearchIndexTrigger.fanOut(Category.class, category -> category.getUuid() == null
                        ? null : "category:" + category.getUuid()));
    }

    @Override
    public Set<UUID> resolveFanOut(String key) {
        int separator = key.indexOf(':');
        if (separator < 0) {
            return Set.of();
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(key.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return Set.of();
        }
        String sql = switch (key.substring(0, separator)) {
            case "course" -> "SELECT DISTINCT program_uuid FROM program_courses WHERE course_uuid = :uuid";
            case "category" -> "SELECT uuid FROM training_programs WHERE category_uuid = :uuid";
            default -> null;
        };
        return sql == null ? Set.of() : new HashSet<>(jdbc.queryForList(sql, Map.of("uuid", uuid), UUID.class));
    }

    private List<ProgramSearchDocument> toDocuments(List<ProgramRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> programUuids = rows.stream().map(ProgramRow::uuid).toList();
        Map<UUID, List<String>> courseNames = new HashMap<>();
        jdbc.query("""
                SELECT pc.program_uuid, c.name
                FROM program_courses pc
                JOIN courses c ON c.uuid = pc.course_uuid
                WHERE pc.program_uuid IN (:uuids)
                ORDER BY pc.sequence_order NULLS LAST, c.name
                """, new MapSqlParameterSource("uuids", programUuids), rs -> {
            courseNames.computeIfAbsent(SearchRows.uuid(rs, "program_uuid"), key -> new ArrayList<>())
                    .add(rs.getString("name"));
        });

        Set<UUID> creatorUuids = new LinkedHashSet<>();
        rows.stream().map(ProgramRow::courseCreatorUuid).filter(Objects::nonNull).forEach(creatorUuids::add);
        Map<UUID, String> creatorNames = creatorUuids.isEmpty()
                ? Map.of() : courseCreatorLookupService.findFullNamesByUuids(creatorUuids);

        List<ProgramSearchDocument> documents = new ArrayList<>(rows.size());
        for (ProgramRow row : rows) {
            boolean isPublic = "published".equals(row.status()) && row.adminApproved() && row.active();
            documents.add(new ProgramSearchDocument(
                    row.uuid(),
                    row.title(),
                    SearchRows.truncate(row.description()),
                    row.categoryUuid(),
                    row.categoryName(),
                    row.courseCreatorUuid(),
                    row.courseCreatorUuid() == null ? null : creatorNames.get(row.courseCreatorUuid()),
                    courseNames.getOrDefault(row.uuid(), List.of()),
                    row.status(),
                    row.isPublished(),
                    row.adminApproved(),
                    row.active(),
                    isPublic,
                    SearchRows.isFree(row.price()),
                    row.price(),
                    row.createdAt()));
        }
        return documents;
    }

    private static ProgramRow row(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new ProgramRow(
                rs.getLong("id"),
                SearchRows.uuid(rs, "uuid"),
                rs.getString("title"),
                rs.getString("description"),
                SearchRows.uuid(rs, "category_uuid"),
                rs.getString("category_name"),
                SearchRows.uuid(rs, "course_creator_uuid"),
                status == null ? null : status.toLowerCase(Locale.ROOT),
                SearchRows.flag(rs, "is_published"),
                SearchRows.flag(rs, "admin_approved"),
                SearchRows.flag(rs, "is_active"),
                rs.getBigDecimal("price"),
                SearchRows.nullableLong(rs, "created_at"));
    }

    private record ProgramRow(
            long id,
            UUID uuid,
            String title,
            String description,
            UUID categoryUuid,
            String categoryName,
            UUID courseCreatorUuid,
            String status,
            boolean isPublished,
            boolean adminApproved,
            boolean active,
            BigDecimal price,
            Long createdAt
    ) {
    }
}
