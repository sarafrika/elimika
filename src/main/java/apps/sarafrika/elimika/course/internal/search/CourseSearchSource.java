package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.CoursePrerequisite;
import apps.sarafrika.elimika.course.model.CourseReview;
import apps.sarafrika.elimika.course.model.CourseSkill;
import apps.sarafrika.elimika.course.model.DifficultyLevel;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
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
 * Feeds the {@code courses} index from the {@code courses} table.
 * <p>
 * Only root courses are indexed: a shadow draft ({@code parent_course_uuid} set) is a pending edit,
 * never listed, so it is left out of every load and therefore deleted from the index if it ever got
 * there. Drafts, in-review and archived root courses are indexed, because their authors and the
 * people tied to them may list them; the scope decides who sees what.
 * <p>
 * Course-creator names come through the course creator SPI. A creator renaming themselves is a change
 * in another module whose entity this module cannot see, so it is not a trigger: the next change to
 * the course or the nightly full rebuild ({@code search.full-rebuild-cron}) picks it up.
 */
@Component
public class CourseSearchSource implements SearchDocumentSource<CourseSearchDocument> {

    public static final String INDEX = "courses";
    static final String UUID_ATTRIBUTE = "uuid";
    static final String IS_PUBLIC = "is_public";
    static final String COURSE_CREATOR_UUID = "course_creator_uuid";

    /**
     * Schema 2 adds the nightly aggregates from {@code course_learning_stats} ({@code completion_rate},
     * {@code popularity_30d}, {@code rating_bayes}), {@code level_order}, {@code prerequisite_uuids} and the
     * age band, and ranks ties by the Bayesian rating instead of the raw average. Schema 3 adds the
     * owner-tagged {@code skill_uuids}. Schema 4 adds the searchable, filterable {@code course_code}.
     */
    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 4,
                    List.of("name", "course_code", "category_names", "creator_name", "difficulty_name", "description",
                            "objectives"),
                    List.of("status", "active", "admin_approved", IS_PUBLIC, COURSE_CREATOR_UUID, "category_uuids", "course_code",
                            "difficulty_uuid", "is_free", "price", UUID_ATTRIBUTE, "created_at", "level_order",
                            "prerequisite_uuids", "age_lower_limit", "age_upper_limit", "skill_uuids"),
                    List.of("name", "created_at", "price", "rating_avg", "enrolment_count", "completion_rate",
                            "popularity_30d", "rating_bayes"))
            .withRankingRules(List.of("words", "typo", "proximity", "attribute", "sort", "exactness", "rating_bayes:desc"))
            .withTypoDisabledAttributes(List.of("status"))
            .withSynonymSources(List.of(SearchSynonymSource.SKILLS));

    private static final String COURSE_COLUMNS = """
            SELECT c.id, c.uuid, c.parent_course_uuid, c.name, c.course_code, c.description, c.objectives, c.difficulty_uuid,
                   d.name AS difficulty_name, c.course_creator_uuid, c.status, c.active, c.admin_approved,
                   c.price, c.thumbnail_url, d.level_order, c.age_lower_limit, c.age_upper_limit,
                   EXTRACT(EPOCH FROM c.created_date)::bigint AS created_at,
                   s.completion_rate::float8 AS completion_rate, s.enrolments_30d, s.rating_bayes::float8 AS rating_bayes
            FROM courses c
            LEFT JOIN course_difficulty_levels d ON d.uuid = c.difficulty_uuid
            LEFT JOIN course_learning_stats s ON s.course_uuid = c.uuid
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CourseCreatorLookupService courseCreatorLookupService;

    public CourseSearchSource(NamedParameterJdbcTemplate jdbc, CourseCreatorLookupService courseCreatorLookupService) {
        this.jdbc = jdbc;
        this.courseCreatorLookupService = courseCreatorLookupService;
    }

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<CourseSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        List<CourseRow> rows = jdbc.query(COURSE_COLUMNS + " WHERE c.uuid IN (:uuids) AND c.parent_course_uuid IS NULL",
                new MapSqlParameterSource("uuids", uuids), (rs, rowNum) -> row(rs));
        return toDocuments(rows);
    }

    @Override
    public SearchBatch<CourseSearchDocument> loadAfter(long lastId, int batchSize) {
        List<CourseRow> rows = jdbc.query(COURSE_COLUMNS + " WHERE c.id > :lastId ORDER BY c.id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)), (rs, rowNum) -> row(rs));
        if (rows.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        long highestId = rows.getLast().id();
        List<CourseRow> roots = rows.stream().filter(row -> row.parentCourseUuid() == null).toList();
        return new SearchBatch<>(toDocuments(roots), highestId);
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE parent_course_uuid IS NULL",
                Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(Course.class, Course::getUuid),
                SearchIndexTrigger.direct(CourseCategoryMapping.class, CourseCategoryMapping::getCourseUuid),
                SearchIndexTrigger.direct(CourseReview.class, CourseReview::getCourseUuid),
                // Keeps enrolment_count live between nightly rebuilds.
                SearchIndexTrigger.direct(CourseEnrollment.class, CourseEnrollment::getCourseUuid),
                // A draft's rows name the draft course, which loadByUuids ignores; promotion rewrites the live rows.
                SearchIndexTrigger.direct(CoursePrerequisite.class, CoursePrerequisite::getCourseUuid),
                SearchIndexTrigger.direct(CourseSkill.class, CourseSkill::getCourseUuid),
                SearchIndexTrigger.fanOut(Category.class, category -> keyOf("category", category.getUuid())),
                SearchIndexTrigger.fanOut(DifficultyLevel.class, level -> keyOf("difficulty", level.getUuid())));
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
            case "category" -> "SELECT course_uuid FROM course_category_mappings WHERE category_uuid = :uuid";
            case "difficulty" -> "SELECT uuid FROM courses WHERE difficulty_uuid = :uuid AND parent_course_uuid IS NULL";
            default -> null;
        };
        if (sql == null) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(sql, Map.of("uuid", uuid), UUID.class));
    }

    private static String keyOf(String kind, UUID uuid) {
        return uuid == null ? null : kind + ":" + uuid;
    }

    private List<CourseSearchDocument> toDocuments(List<CourseRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> courseUuids = rows.stream().map(CourseRow::uuid).toList();
        MapSqlParameterSource byCourse = new MapSqlParameterSource("uuids", courseUuids);

        Map<UUID, List<UUID>> categoryUuids = new HashMap<>();
        Map<UUID, List<String>> categoryNames = new HashMap<>();
        jdbc.query("""
                SELECT m.course_uuid, cat.uuid AS category_uuid, cat.name
                FROM course_category_mappings m
                JOIN course_categories cat ON cat.uuid = m.category_uuid
                WHERE m.course_uuid IN (:uuids)
                ORDER BY cat.name
                """, byCourse, rs -> {
            UUID courseUuid = SearchRows.uuid(rs, "course_uuid");
            categoryUuids.computeIfAbsent(courseUuid, key -> new ArrayList<>()).add(SearchRows.uuid(rs, "category_uuid"));
            categoryNames.computeIfAbsent(courseUuid, key -> new ArrayList<>()).add(rs.getString("name"));
        });

        Map<UUID, double[]> reviews = new HashMap<>();
        jdbc.query("""
                SELECT course_uuid, AVG(rating)::float8 AS rating_avg, COUNT(*) AS review_count
                FROM course_reviews WHERE course_uuid IN (:uuids) GROUP BY course_uuid
                """, byCourse, rs -> {
            reviews.put(SearchRows.uuid(rs, "course_uuid"),
                    new double[]{rs.getDouble("rating_avg"), rs.getLong("review_count")});
        });

        Map<UUID, List<UUID>> skillUuids = new HashMap<>();
        jdbc.query("""
                SELECT course_uuid, skill_uuid FROM course_skills
                WHERE course_uuid IN (:uuids) ORDER BY course_uuid, weight DESC, id
                """, byCourse, rs -> {
            skillUuids.computeIfAbsent(SearchRows.uuid(rs, "course_uuid"), key -> new ArrayList<>())
                    .add(SearchRows.uuid(rs, "skill_uuid"));
        });

        Map<UUID, Long> enrolments = new HashMap<>();
        jdbc.query("""
                SELECT course_uuid, COUNT(*) AS enrolment_count
                FROM course_enrollments WHERE course_uuid IN (:uuids) GROUP BY course_uuid
                """, byCourse, rs -> {
            enrolments.put(SearchRows.uuid(rs, "course_uuid"), rs.getLong("enrolment_count"));
        });

        Map<UUID, List<UUID>> prerequisites = new HashMap<>();
        jdbc.query("""
                SELECT course_uuid, prerequisite_course_uuid FROM course_prerequisites
                WHERE course_uuid IN (:uuids) ORDER BY id
                """, byCourse, rs -> {
            prerequisites.computeIfAbsent(SearchRows.uuid(rs, "course_uuid"), key -> new ArrayList<>())
                    .add(SearchRows.uuid(rs, "prerequisite_course_uuid"));
        });

        Set<UUID> creatorUuids = new LinkedHashSet<>();
        rows.stream().map(CourseRow::courseCreatorUuid).filter(Objects::nonNull).forEach(creatorUuids::add);
        Map<UUID, String> creatorNames = creatorUuids.isEmpty()
                ? Map.of() : courseCreatorLookupService.findFullNamesByUuids(creatorUuids);

        List<CourseSearchDocument> documents = new ArrayList<>(rows.size());
        for (CourseRow row : rows) {
            double[] review = reviews.get(row.uuid());
            boolean isPublic = "published".equals(row.status()) && row.adminApproved() && row.active();
            documents.add(new CourseSearchDocument(
                    row.uuid(),
                    row.name(),
                    row.courseCode(),
                    SearchRows.truncate(row.description()),
                    SearchRows.truncate(row.objectives()),
                    categoryUuids.getOrDefault(row.uuid(), List.of()),
                    categoryNames.getOrDefault(row.uuid(), List.of()),
                    row.difficultyUuid(),
                    row.difficultyName(),
                    row.courseCreatorUuid(),
                    row.courseCreatorUuid() == null ? null : creatorNames.get(row.courseCreatorUuid()),
                    row.status(),
                    row.active(),
                    row.adminApproved(),
                    isPublic,
                    SearchRows.isFree(row.price()),
                    row.price(),
                    row.thumbnailUrl(),
                    review == null ? null : review[0],
                    review == null ? 0 : (long) review[1],
                    enrolments.getOrDefault(row.uuid(), 0L),
                    row.createdAt(),
                    row.completionRate(),
                    row.enrolments30d(),
                    row.ratingBayes(),
                    row.levelOrder(),
                    prerequisites.getOrDefault(row.uuid(), List.of()),
                    row.ageLowerLimit(),
                    row.ageUpperLimit(),
                    skillUuids.getOrDefault(row.uuid(), List.of())));
        }
        return documents;
    }

    private static CourseRow row(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new CourseRow(
                rs.getLong("id"),
                SearchRows.uuid(rs, "uuid"),
                SearchRows.uuid(rs, "parent_course_uuid"),
                rs.getString("name"),
                rs.getString("course_code"),
                rs.getString("description"),
                rs.getString("objectives"),
                SearchRows.uuid(rs, "difficulty_uuid"),
                rs.getString("difficulty_name"),
                SearchRows.uuid(rs, "course_creator_uuid"),
                status == null ? null : status.toLowerCase(Locale.ROOT),
                SearchRows.flag(rs, "active"),
                SearchRows.flag(rs, "admin_approved"),
                rs.getBigDecimal("price"),
                rs.getString("thumbnail_url"),
                SearchRows.nullableLong(rs, "created_at"),
                nullableInt(rs, "level_order"),
                nullableInt(rs, "age_lower_limit"),
                nullableInt(rs, "age_upper_limit"),
                nullableDouble(rs, "completion_rate"),
                rs.getLong("enrolments_30d"),
                nullableDouble(rs, "rating_bayes"));
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private record CourseRow(
            long id,
            UUID uuid,
            UUID parentCourseUuid,
            String name,
            String courseCode,
            String description,
            String objectives,
            UUID difficultyUuid,
            String difficultyName,
            UUID courseCreatorUuid,
            String status,
            boolean active,
            boolean adminApproved,
            BigDecimal price,
            String thumbnailUrl,
            Long createdAt,
            Integer levelOrder,
            Integer ageLowerLimit,
            Integer ageUpperLimit,
            Double completionRate,
            long enrolments30d,
            Double ratingBayes
    ) {
    }
}
