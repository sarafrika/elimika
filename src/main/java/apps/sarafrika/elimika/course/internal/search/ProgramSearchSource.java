package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseSkill;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.DifficultyLevel;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.ProgramRequirement;
import apps.sarafrika.elimika.course.model.ProgramReview;
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

    /**
     * Schema 2 adds the catalogue card fields and the sort attributes shared with {@code courses}
     * ({@code rating_bayes}, {@code popularity_30d}), makes {@code difficulty_uuids} filterable, and
     * ranks with the course index's rules so the two can be merged into one federated ranking.
     * Schema 3 adds {@code program_code} and prefers the program's own thumbnail over its first course's.
     * Schema 4 adds the apply-to-train fields: filterable {@code skill_uuids}, the member courses' age-band
     * intersection, {@code lesson_count} and {@code requirement_count}.
     */
    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 4,
                    List.of("title", "program_code", "course_names", "category_name", "creator_name", "description"),
                    List.of("status", "is_published", "admin_approved", "active", IS_PUBLIC, COURSE_CREATOR_UUID,
                            "category_uuid", "is_free", "uuid", "created_at", "difficulty_uuids", "program_code",
                            "skill_uuids"),
                    List.of("title", "created_at", "rating_avg", "rating_bayes", "popularity_30d", "enrolment_count"))
            .withRankingRules(CourseSearchSource.DEFINITION.rankingRules())
            .withTypoDisabledAttributes(List.of("status"));

    private static final String PROGRAM_COLUMNS = """
            SELECT p.id, p.uuid, p.title, p.program_code, p.thumbnail_url, p.description, p.category_uuid, cat.name AS category_name,
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
                SearchIndexTrigger.direct(ProgramReview.class, ProgramReview::getProgramUuid),
                SearchIndexTrigger.direct(ProgramEnrollment.class, ProgramEnrollment::getProgramUuid),
                SearchIndexTrigger.direct(ProgramRequirement.class, ProgramRequirement::getProgramUuid),
                // A member course's name, age band, lessons and skills are part of the document.
                SearchIndexTrigger.fanOut(Course.class, course -> course.getUuid() == null
                        || course.getParentCourseUuid() != null ? null : "course:" + course.getUuid()),
                SearchIndexTrigger.fanOut(Lesson.class, lesson -> lesson.getCourseUuid() == null
                        ? null : "course:" + lesson.getCourseUuid()),
                SearchIndexTrigger.fanOut(CourseSkill.class, skill -> skill.getCourseUuid() == null
                        ? null : "course:" + skill.getCourseUuid()),
                SearchIndexTrigger.fanOut(Category.class, category -> category.getUuid() == null
                        ? null : "category:" + category.getUuid()),
                SearchIndexTrigger.fanOut(DifficultyLevel.class, level -> level.getUuid() == null
                        ? null : "difficulty:" + level.getUuid()));
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
            case "difficulty" -> """
                    SELECT DISTINCT pc.program_uuid FROM program_courses pc
                    JOIN courses c ON c.uuid = pc.course_uuid WHERE c.difficulty_uuid = :uuid
                    """;
            default -> null;
        };
        return sql == null ? Set.of() : new HashSet<>(jdbc.queryForList(sql, Map.of("uuid", uuid), UUID.class));
    }

    private List<ProgramSearchDocument> toDocuments(List<ProgramRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> programUuids = rows.stream().map(ProgramRow::uuid).toList();
        MapSqlParameterSource byProgram = new MapSqlParameterSource("uuids", programUuids);
        Map<UUID, List<String>> courseNames = new HashMap<>();
        Map<UUID, String> thumbnails = new HashMap<>();
        Map<UUID, Set<UUID>> difficultyUuids = new HashMap<>();
        Map<UUID, Level[]> levelRange = new HashMap<>();
        jdbc.query("""
                SELECT pc.program_uuid, c.name, c.thumbnail_url, c.difficulty_uuid, d.name AS difficulty_name,
                       d.level_order
                FROM program_courses pc
                JOIN courses c ON c.uuid = pc.course_uuid
                LEFT JOIN course_difficulty_levels d ON d.uuid = c.difficulty_uuid
                WHERE pc.program_uuid IN (:uuids)
                ORDER BY pc.sequence_order NULLS LAST, c.name
                """, byProgram, rs -> {
            UUID programUuid = SearchRows.uuid(rs, "program_uuid");
            courseNames.computeIfAbsent(programUuid, key -> new ArrayList<>()).add(rs.getString("name"));
            String thumbnail = rs.getString("thumbnail_url");
            if (thumbnail != null && !thumbnail.isBlank()) {
                thumbnails.putIfAbsent(programUuid, thumbnail);
            }
            UUID difficultyUuid = SearchRows.uuid(rs, "difficulty_uuid");
            if (difficultyUuid != null) {
                difficultyUuids.computeIfAbsent(programUuid, key -> new LinkedHashSet<>()).add(difficultyUuid);
                int order = rs.getInt("level_order");
                Level level = new Level(rs.wasNull() ? Integer.MAX_VALUE : order, rs.getString("difficulty_name"));
                Level[] range = levelRange.computeIfAbsent(programUuid, key -> new Level[]{level, level});
                if (level.order() < range[0].order()) {
                    range[0] = level;
                }
                if (level.order() > range[1].order()) {
                    range[1] = level;
                }
            }
        });

        // Apply-to-train: the union of member skills, the intersection of member age bands, active lessons.
        Map<UUID, Set<UUID>> skillUuids = new HashMap<>();
        jdbc.query("""
                SELECT DISTINCT pc.program_uuid, cs.skill_uuid
                FROM program_courses pc JOIN course_skills cs ON cs.course_uuid = pc.course_uuid
                WHERE pc.program_uuid IN (:uuids) AND cs.skill_uuid IS NOT NULL
                """, byProgram, rs -> {
            skillUuids.computeIfAbsent(SearchRows.uuid(rs, "program_uuid"), key -> new LinkedHashSet<>())
                    .add(SearchRows.uuid(rs, "skill_uuid"));
        });
        Map<UUID, Integer[]> ageBands = new HashMap<>();
        Map<UUID, Long> lessonCounts = new HashMap<>();
        jdbc.query("""
                SELECT pc.program_uuid, MAX(c.age_lower_limit) AS age_lower_limit,
                       MIN(c.age_upper_limit) AS age_upper_limit,
                       (SELECT COUNT(*) FROM program_courses pc2
                        JOIN lessons l ON l.course_uuid = pc2.course_uuid AND l.active = true
                        WHERE pc2.program_uuid = pc.program_uuid) AS lesson_count
                FROM program_courses pc JOIN courses c ON c.uuid = pc.course_uuid
                WHERE pc.program_uuid IN (:uuids) GROUP BY pc.program_uuid
                """, byProgram, rs -> {
            UUID programUuid = SearchRows.uuid(rs, "program_uuid");
            int lower = rs.getInt("age_lower_limit");
            Integer lowerLimit = rs.wasNull() ? null : lower;
            int upper = rs.getInt("age_upper_limit");
            Integer upperLimit = rs.wasNull() ? null : upper;
            ageBands.put(programUuid, new Integer[]{lowerLimit, upperLimit});
            lessonCounts.put(programUuid, rs.getLong("lesson_count"));
        });
        Map<UUID, Long> requirementCounts = new HashMap<>();
        jdbc.query("""
                SELECT program_uuid, COUNT(*) AS requirement_count FROM program_requirements
                WHERE program_uuid IN (:uuids) GROUP BY program_uuid
                """, byProgram, rs -> {
            requirementCounts.put(SearchRows.uuid(rs, "program_uuid"), rs.getLong("requirement_count"));
        });

        // Same Bayesian shrinkage as course_learning_stats.rating_bayes: (C*m + sum) / (C + n), C = 5,
        // m = the mean of every program review.
        Map<UUID, double[]> reviews = new HashMap<>();
        jdbc.query("""
                SELECT r.program_uuid, AVG(r.rating)::float8 AS rating_avg, COUNT(*) AS review_count,
                       ((5 * (SELECT AVG(rating) FROM program_reviews) + SUM(r.rating)) / (5 + COUNT(*)))::float8
                           AS rating_bayes
                FROM program_reviews r WHERE r.program_uuid IN (:uuids) GROUP BY r.program_uuid
                """, byProgram, rs -> {
            reviews.put(SearchRows.uuid(rs, "program_uuid"), new double[]{
                    rs.getDouble("rating_avg"), rs.getLong("review_count"), rs.getDouble("rating_bayes")});
        });

        Map<UUID, long[]> enrolments = new HashMap<>();
        jdbc.query("""
                SELECT program_uuid, COUNT(*) AS enrolment_count,
                       COUNT(*) FILTER (WHERE enrollment_date >= NOW() - INTERVAL '30 days') AS enrolments_30d
                FROM program_enrollments WHERE program_uuid IN (:uuids) GROUP BY program_uuid
                """, byProgram, rs -> {
            enrolments.put(SearchRows.uuid(rs, "program_uuid"),
                    new long[]{rs.getLong("enrolment_count"), rs.getLong("enrolments_30d")});
        });

        Set<UUID> creatorUuids = new LinkedHashSet<>();
        rows.stream().map(ProgramRow::courseCreatorUuid).filter(Objects::nonNull).forEach(creatorUuids::add);
        Map<UUID, String> creatorNames = creatorUuids.isEmpty()
                ? Map.of() : courseCreatorLookupService.findFullNamesByUuids(creatorUuids);

        List<ProgramSearchDocument> documents = new ArrayList<>(rows.size());
        for (ProgramRow row : rows) {
            boolean isPublic = "published".equals(row.status()) && row.adminApproved() && row.active();
            List<String> members = courseNames.getOrDefault(row.uuid(), List.of());
            double[] review = reviews.get(row.uuid());
            long[] enrolment = enrolments.getOrDefault(row.uuid(), new long[]{0, 0});
            Level[] range = levelRange.get(row.uuid());
            Integer[] ageBand = ageBands.getOrDefault(row.uuid(), new Integer[]{null, null});
            documents.add(new ProgramSearchDocument(
                    row.uuid(),
                    row.title(),
                    row.programCode(),
                    SearchRows.truncate(row.description()),
                    row.categoryUuid(),
                    row.categoryName(),
                    row.courseCreatorUuid(),
                    row.courseCreatorUuid() == null ? null : creatorNames.get(row.courseCreatorUuid()),
                    members,
                    row.status(),
                    row.isPublished(),
                    row.adminApproved(),
                    row.active(),
                    isPublic,
                    SearchRows.isFree(row.price()),
                    row.price(),
                    row.createdAt(),
                    row.categoryUuid() == null ? List.of() : List.of(row.categoryUuid()),
                    row.categoryName() == null ? List.of() : List.of(row.categoryName()),
                    row.thumbnailUrl() != null && !row.thumbnailUrl().isBlank()
                            ? row.thumbnailUrl() : thumbnails.get(row.uuid()),
                    members.size(),
                    List.copyOf(difficultyUuids.getOrDefault(row.uuid(), Set.of())),
                    range == null ? null : range[0].name(),
                    range == null ? null : range[1].name(),
                    review == null ? null : review[0],
                    review == null ? 0 : (long) review[1],
                    review == null ? null : review[2],
                    enrolment[0],
                    enrolment[1],
                    List.copyOf(skillUuids.getOrDefault(row.uuid(), Set.of())),
                    ageBand[0],
                    ageBand[1],
                    lessonCounts.getOrDefault(row.uuid(), 0L),
                    requirementCounts.getOrDefault(row.uuid(), 0L)));
        }
        return documents;
    }

    /** One member course's difficulty, for the program's level range. */
    private record Level(int order, String name) {
    }

    private static ProgramRow row(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new ProgramRow(
                rs.getLong("id"),
                SearchRows.uuid(rs, "uuid"),
                rs.getString("title"),
                rs.getString("program_code"),
                rs.getString("thumbnail_url"),
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
            String programCode,
            String thumbnailUrl,
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
