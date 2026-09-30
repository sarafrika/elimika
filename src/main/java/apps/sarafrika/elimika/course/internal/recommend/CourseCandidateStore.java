package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse.CategoryRef;
import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse.PrerequisiteRef;
import apps.sarafrika.elimika.course.internal.recommend.LearnerProfile.Enrolment;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.AffiliationOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The recommender's SQL. Every candidate is loaded here, whatever retrieved it, and every load re-applies
 * the public-catalogue rule ({@code CourseSpecificationBuilder#visibleTo}'s public clause: a published,
 * admin-approved, active root course), the learner's audience and the exclusions. A search hit whose
 * document is stale therefore simply drops out. Each method is one statement, whatever the number of rows.
 */
@Component
@RequiredArgsConstructor
public class CourseCandidateStore {

    /** Keeps IN lists well inside PostgreSQL's bind-parameter limit. */
    static final int MAX_IN = 1000;

    private static final String PUBLIC_EXPRESSION = "(c.parent_course_uuid IS NULL AND LOWER(c.status) = 'published' "
            + "AND c.admin_approved = true AND c.active = true)";

    /** Features of a course, as columns; {@code c} is the course row. */
    private static final String FEATURE_COLUMNS = """
            c.uuid, c.name, c.description, c.thumbnail_url, c.created_date, d.level_order,
            s.rating_bayes::float8 AS rating_bayes, COALESCE(s.enrolments_30d, 0) AS popularity_30d,
            ARRAY(SELECT m.category_uuid::text || '|' || COALESCE(cat.parent_uuid::text, '') || '|' || COALESCE(cat.name, '')
                  FROM course_category_mappings m JOIN course_categories cat ON cat.uuid = m.category_uuid
                  WHERE m.course_uuid = c.uuid ORDER BY cat.name, m.category_uuid) AS categories,
            ARRAY(SELECT sk.skill_uuid FROM course_skills sk
                  WHERE sk.course_uuid = c.uuid ORDER BY sk.weight DESC, sk.id) AS skills,
            ARRAY(SELECT p.prerequisite_course_uuid::text || '|' || CASE WHEN p.is_mandatory THEN '1' ELSE '0' END
                         || '|' || COALESCE(pc.name, '')
                  FROM course_prerequisites p LEFT JOIN courses pc ON pc.uuid = p.prerequisite_course_uuid
                  WHERE p.course_uuid = c.uuid ORDER BY p.id) AS prerequisites
            """;

    private static final String CANDIDATE_SELECT = "SELECT " + FEATURE_COLUMNS + ", " + PUBLIC_EXPRESSION + " AS publicly_listed "
            + """
            FROM courses c
            LEFT JOIN course_difficulty_levels d ON d.uuid = c.difficulty_uuid
            LEFT JOIN course_learning_stats s ON s.course_uuid = c.uuid
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Who may see age-limited courses. A learner without a date of birth on record sees only courses
     * with no age limit: enrolling in a limited one needs a date of birth anyway.
     */
    public record Audience(Integer age, boolean restricted) {
        public static Audience anyone() {
            return new Audience(null, false);
        }

        public static Audience ofAge(int age) {
            return new Audience(age, true);
        }

        public static Audience unknownAge() {
            return new Audience(null, true);
        }
    }

    // ================================================================== candidates

    /** The given courses that are publicly listed, within the audience and not excluded. */
    public List<CandidateCourse> loadPublic(Collection<UUID> uuids, Audience audience, Collection<UUID> excluded) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("uuids", cap(uuids));
        String sql = CANDIDATE_SELECT + " WHERE " + PUBLIC_EXPRESSION + " AND c.uuid IN (:uuids)"
                + audienceClause(audience, params) + exclusionClause(excluded, params);
        return jdbc.query(sql, params, (rs, rowNum) -> candidate(rs));
    }

    /**
     * The relational fallback when search cannot answer: public courses in the audience matching any of
     * the candidate sets, most popular first, topped up with the most popular courses overall.
     */
    public List<CandidateCourse> findRelationalCandidates(Collection<UUID> categoryUuids, Collection<UUID> courseUuids,
                                                          Collection<UUID> skillUuids, Collection<UUID> followOnOf,
                                                          Audience audience,
                                                          Collection<UUID> excluded, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        List<String> matches = new ArrayList<>();
        if (categoryUuids != null && !categoryUuids.isEmpty()) {
            params.addValue("categoryUuids", cap(categoryUuids));
            matches.add("EXISTS (SELECT 1 FROM course_category_mappings m WHERE m.course_uuid = c.uuid "
                    + "AND m.category_uuid IN (:categoryUuids))");
        }
        if (courseUuids != null && !courseUuids.isEmpty()) {
            params.addValue("courseUuids", cap(courseUuids));
            matches.add("c.uuid IN (:courseUuids)");
        }
        if (skillUuids != null && !skillUuids.isEmpty()) {
            params.addValue("skillUuids", cap(skillUuids));
            matches.add("EXISTS (SELECT 1 FROM course_skills sk WHERE sk.course_uuid = c.uuid "
                    + "AND sk.skill_uuid IN (:skillUuids))");
        }
        if (followOnOf != null && !followOnOf.isEmpty()) {
            params.addValue("followOnOf", cap(followOnOf));
            matches.add("EXISTS (SELECT 1 FROM course_prerequisites p WHERE p.course_uuid = c.uuid "
                    + "AND p.prerequisite_course_uuid IN (:followOnOf))");
        }
        String match = matches.isEmpty() ? "false" : "(" + String.join(" OR ", matches) + ")";
        String sql = CANDIDATE_SELECT + " WHERE " + PUBLIC_EXPRESSION + audienceClause(audience, params)
                + exclusionClause(excluded, params)
                + " ORDER BY " + match + " DESC, COALESCE(s.enrolments_30d, 0) DESC, s.rating_bayes DESC NULLS LAST, c.uuid"
                + " LIMIT :limit";
        return jdbc.query(sql, params, (rs, rowNum) -> candidate(rs));
    }

    /** Every root course, listed or not, for the offline evaluator. */
    public List<CandidateCourse> loadAllRootCourses() {
        return jdbc.query(CANDIDATE_SELECT + " WHERE c.parent_course_uuid IS NULL", new MapSqlParameterSource(),
                (rs, rowNum) -> candidate(rs));
    }

    // ================================================================== learner rows

    /** The learner's course enrolments with the features of each course, one statement. */
    public List<Enrolment> loadEnrolments(UUID studentUuid) {
        return jdbc.query("SELECT e.status AS enrolment_status, e.progress_percentage, e.enrollment_date, e.completion_date, "
                        + FEATURE_COLUMNS + """
                        FROM course_enrollments e
                        JOIN courses c ON c.uuid = e.course_uuid
                        LEFT JOIN course_difficulty_levels d ON d.uuid = c.difficulty_uuid
                        LEFT JOIN course_learning_stats s ON s.course_uuid = c.uuid
                        WHERE e.student_uuid = :studentUuid
                        ORDER BY e.enrollment_date, e.id
                        """, new MapSqlParameterSource("studentUuid", studentUuid), (rs, rowNum) -> {
                    BigDecimal progress = rs.getBigDecimal("progress_percentage");
                    String status = rs.getString("enrolment_status");
                    return new Enrolment(
                            rs.getObject("uuid", UUID.class),
                            rs.getString("name"),
                            status == null ? "active" : status.toLowerCase(java.util.Locale.ROOT),
                            progress == null ? 0 : progress.doubleValue(),
                            utc(rs, "enrollment_date"),
                            utc(rs, "completion_date"),
                            nullableInt(rs, "level_order"),
                            categories(rs),
                            uuids(rs.getArray("skills")));
                });
    }

    /**
     * Co-enrolment neighbours of the given courses with their lift. The nightly job only stores pairs that
     * cleared the privacy threshold; the same threshold is re-checked here so a stale row can never leak.
     */
    public Map<UUID, Map<UUID, Double>> findNeighbourLifts(Collection<UUID> courseUuids) {
        Map<UUID, Map<UUID, Double>> lifts = new HashMap<>();
        if (courseUuids == null || courseUuids.isEmpty()) {
            return lifts;
        }
        MapSqlParameterSource params = new MapSqlParameterSource("uuids", cap(courseUuids))
                .addValue("minShared", CourseFeatureRefresher.MIN_SHARED_LEARNERS)
                .addValue("minSharedWithMinors", CourseFeatureRefresher.MIN_SHARED_LEARNERS_WITH_MINORS);
        jdbc.query("""
                SELECT course_uuid, neighbour_course_uuid, lift::float8 AS lift
                FROM course_co_enrolments
                WHERE course_uuid IN (:uuids)
                  AND shared_learners >= CASE WHEN includes_minors THEN :minSharedWithMinors ELSE :minShared END
                ORDER BY lift DESC
                LIMIT 500
                """, params, rs -> {
            lifts.computeIfAbsent(rs.getObject("course_uuid", UUID.class), key -> new HashMap<>())
                    .put(rs.getObject("neighbour_course_uuid", UUID.class), rs.getDouble("lift"));
        });
        return lifts;
    }

    /**
     * Courses approved for training by the given organisations or instructors, and who offers each
     * (an organisation wins over an instructor).
     */
    public Map<UUID, AffiliationOffer> findApprovedOffers(Set<UUID> organisationUuids, Set<UUID> instructorUuids) {
        Map<UUID, AffiliationOffer> offers = new LinkedHashMap<>();
        if ((organisationUuids == null || organisationUuids.isEmpty()) && (instructorUuids == null || instructorUuids.isEmpty())) {
            return offers;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organisations", organisationUuids == null || organisationUuids.isEmpty()
                        ? List.of(new UUID(0, 0)) : cap(organisationUuids))
                .addValue("instructors", instructorUuids == null || instructorUuids.isEmpty()
                        ? List.of(new UUID(0, 0)) : cap(instructorUuids));
        jdbc.query("""
                SELECT a.course_uuid, a.applicant_uuid, LOWER(a.applicant_type) = 'organisation' AS organisation
                FROM course_training_applications a
                WHERE LOWER(a.status) = 'approved'
                  AND ((LOWER(a.applicant_type) = 'organisation' AND a.applicant_uuid IN (:organisations))
                    OR (LOWER(a.applicant_type) = 'instructor' AND a.applicant_uuid IN (:instructors)))
                ORDER BY organisation DESC, a.id
                """, params, rs -> {
            offers.putIfAbsent(rs.getObject("course_uuid", UUID.class),
                    new AffiliationOffer(rs.getObject("applicant_uuid", UUID.class), rs.getBoolean("organisation")));
        });
        return offers;
    }

    /** Every course enrolment, oldest first, for the offline evaluator. */
    public List<EnrolmentRow> loadAllEnrolments() {
        return jdbc.query("""
                SELECT student_uuid, course_uuid, LOWER(status) AS status, progress_percentage, enrollment_date, completion_date
                FROM course_enrollments
                WHERE student_uuid IS NOT NULL AND course_uuid IS NOT NULL
                ORDER BY student_uuid, enrollment_date, id
                """, new MapSqlParameterSource(), (rs, rowNum) -> {
            BigDecimal progress = rs.getBigDecimal("progress_percentage");
            return new EnrolmentRow(rs.getObject("student_uuid", UUID.class), rs.getObject("course_uuid", UUID.class),
                    rs.getString("status"), progress == null ? 0 : progress.doubleValue(),
                    utc(rs, "enrollment_date"), utc(rs, "completion_date"));
        });
    }

    /** One raw course enrolment row. */
    public record EnrolmentRow(UUID studentUuid, UUID courseUuid, String status, double progress,
                               LocalDateTime enrolledAt, LocalDateTime completedAt) {
    }

    // ================================================================== plumbing

    private static String audienceClause(Audience audience, MapSqlParameterSource params) {
        if (audience == null || !audience.restricted()) {
            return "";
        }
        if (audience.age() == null) {
            return " AND c.age_lower_limit IS NULL AND c.age_upper_limit IS NULL";
        }
        params.addValue("age", audience.age());
        return " AND (c.age_lower_limit IS NULL OR c.age_lower_limit <= :age)"
                + " AND (c.age_upper_limit IS NULL OR c.age_upper_limit >= :age)";
    }

    private static String exclusionClause(Collection<UUID> excluded, MapSqlParameterSource params) {
        if (excluded == null || excluded.isEmpty()) {
            return "";
        }
        params.addValue("excluded", cap(excluded));
        return " AND c.uuid NOT IN (:excluded)";
    }

    private static List<UUID> cap(Collection<UUID> uuids) {
        return uuids.stream().distinct().limit(MAX_IN).toList();
    }

    private static CandidateCourse candidate(ResultSet rs) throws SQLException {
        return new CandidateCourse(
                rs.getObject("uuid", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("thumbnail_url"),
                utc(rs, "created_date"),
                nullableInt(rs, "level_order"),
                categories(rs),
                uuids(rs.getArray("skills")),
                prerequisites(rs),
                nullableDouble(rs, "rating_bayes"),
                rs.getLong("popularity_30d"),
                rs.getBoolean("publicly_listed"));
    }

    private static List<CategoryRef> categories(ResultSet rs) throws SQLException {
        List<CategoryRef> categories = new ArrayList<>();
        for (String packed : strings(rs.getArray("categories"))) {
            String[] parts = packed.split("\\|", 3);
            if (parts.length < 3 || parts[0].isBlank()) {
                continue;
            }
            categories.add(new CategoryRef(UUID.fromString(parts[0]),
                    parts[1].isBlank() ? null : UUID.fromString(parts[1]), parts[2]));
        }
        return categories;
    }

    private static List<PrerequisiteRef> prerequisites(ResultSet rs) throws SQLException {
        List<PrerequisiteRef> prerequisites = new ArrayList<>();
        for (String packed : strings(rs.getArray("prerequisites"))) {
            String[] parts = packed.split("\\|", 3);
            if (parts.length < 3 || parts[0].isBlank()) {
                continue;
            }
            prerequisites.add(new PrerequisiteRef(UUID.fromString(parts[0]), parts[2], "1".equals(parts[1])));
        }
        return prerequisites;
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object[] values = (Object[]) array.getArray();
        List<String> result = new ArrayList<>(values.length);
        for (Object value : values) {
            if (value != null) {
                result.add(value.toString());
            }
        }
        return result;
    }

    private static List<UUID> uuids(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object[] values = (Object[]) array.getArray();
        List<UUID> result = new ArrayList<>(values.length);
        for (Object value : values) {
            if (value instanceof UUID uuid) {
                result.add(uuid);
            } else if (value != null) {
                result.add(UUID.fromString(value.toString()));
            }
        }
        return result;
    }

    /** A timestamp column (with or without time zone) as a UTC wall-clock time. */
    private static LocalDateTime utc(ResultSet rs, String column) throws SQLException {
        java.time.OffsetDateTime value = rs.getObject(column, java.time.OffsetDateTime.class);
        return value == null ? null : value.withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
