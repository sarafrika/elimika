package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.model.Quiz;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Feeds the {@code course_content} index: one document per lesson, lesson content item, quiz and
 * assignment, keyed by the item's own UUID.
 * <p>
 * <strong>What is never indexed:</strong> quiz questions and options (they carry the answers), rubric
 * data, submissions and {@code file_url}. The queries below simply never select them.
 * <p>
 * Items of a shadow-draft course ({@code parent_course_uuid} set) are left out of every load, so a
 * pending edit never becomes searchable and is deleted from the index if it ever got there.
 * <p>
 * Triggers: each item re-indexes itself. A lesson also fans out to its children, which embed its
 * title, number and publish state; a course fans out to every item in it (its name is embedded, and a
 * status or shadow change can move items in or out). A lesson content type rename is not a trigger;
 * the nightly rebuild picks it up.
 */
@Component
public class CourseContentSearchSource implements SearchDocumentSource<CourseContentSearchDocument> {

    public static final String INDEX = "course_content";

    static final String TYPE = "type";
    static final String COURSE_UUID = "course_uuid";
    static final String PUBLISHED = "published";
    static final String SCOPE = "scope";
    static final String SCOPE_COURSE = "COURSE";
    static final String SCOPE_CLASS = "CLASS";

    static final String TYPE_LESSON = "lesson";
    static final String TYPE_CONTENT = "content";
    static final String TYPE_QUIZ = "quiz";
    static final String TYPE_ASSIGNMENT = "assignment";
    static final List<String> TYPES = List.of(TYPE_LESSON, TYPE_CONTENT, TYPE_QUIZ, TYPE_ASSIGNMENT);

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 1,
                    List.of("title", "body"),
                    List.of(TYPE, COURSE_UUID, "lesson_uuid", PUBLISHED, SCOPE, "class_definition_uuid",
                            "content_type", "uuid"),
                    List.of("lesson_number", "display_order", "updated_at"))
            .withTypoDisabledAttributes(List.of(TYPE, SCOPE));

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * One branch per item type, each already joined to its lesson and course and limited to root
     * courses. {@code %s} is the column the branch is filtered on: the item's own UUID, or its lesson's.
     */
    private static final String ITEMS = """
            SELECT l.uuid, 'lesson' AS type, l.course_uuid, l.uuid AS lesson_uuid, l.lesson_number,
                   l.title AS lesson_title, c.name AS course_name, l.title, l.description AS part1,
                   l.learning_objectives AS part2, CAST(NULL AS varchar) AS content_type, 'COURSE' AS scope,
                   CAST(NULL AS uuid) AS class_definition_uuid,
                   (LOWER(l.status) = 'published' AND l.active) AS published, CAST(NULL AS integer) AS display_order,
                   EXTRACT(EPOCH FROM COALESCE(l.updated_date, l.created_date))::bigint AS updated_at
            FROM lessons l JOIN courses c ON c.uuid = l.course_uuid
            WHERE c.parent_course_uuid IS NULL AND %1$s IN (:uuids)
            UNION ALL
            SELECT x.uuid, 'content', l.course_uuid, l.uuid, l.lesson_number, l.title, c.name, x.title,
                   x.description, x.content_text, t.name, 'COURSE', CAST(NULL AS uuid),
                   (LOWER(l.status) = 'published' AND l.active), x.display_order,
                   EXTRACT(EPOCH FROM COALESCE(x.updated_date, x.created_date))::bigint
            FROM lesson_contents x JOIN lessons l ON l.uuid = x.lesson_uuid JOIN courses c ON c.uuid = l.course_uuid
            LEFT JOIN lesson_content_types t ON t.uuid = x.content_type_uuid
            WHERE c.parent_course_uuid IS NULL AND %2$s IN (:uuids)
            UNION ALL
            SELECT x.uuid, 'quiz', l.course_uuid, l.uuid, l.lesson_number, l.title, c.name, x.title,
                   x.description, x.instructions, CAST(NULL AS varchar),
                   CASE WHEN UPPER(x.scope) = 'CLASS_CLONE' THEN 'CLASS' ELSE 'COURSE' END, x.class_definition_uuid,
                   (LOWER(x.status) = 'published' AND x.active AND LOWER(l.status) = 'published' AND l.active),
                   CAST(NULL AS integer),
                   EXTRACT(EPOCH FROM COALESCE(x.updated_date, x.created_date))::bigint
            FROM quizzes x JOIN lessons l ON l.uuid = x.lesson_uuid JOIN courses c ON c.uuid = l.course_uuid
            WHERE c.parent_course_uuid IS NULL AND %2$s IN (:uuids)
            UNION ALL
            SELECT x.uuid, 'assignment', l.course_uuid, l.uuid, l.lesson_number, l.title, c.name, x.title,
                   x.description, x.instructions, CAST(NULL AS varchar),
                   CASE WHEN UPPER(x.scope) = 'CLASS_CLONE' THEN 'CLASS' ELSE 'COURSE' END, x.class_definition_uuid,
                   (COALESCE(x.is_published, false) AND LOWER(l.status) = 'published' AND l.active),
                   CAST(NULL AS integer),
                   EXTRACT(EPOCH FROM COALESCE(x.updated_date, x.created_date))::bigint
            FROM assignments x JOIN lessons l ON l.uuid = x.lesson_uuid JOIN courses c ON c.uuid = l.course_uuid
            WHERE c.parent_course_uuid IS NULL AND %2$s IN (:uuids)
            """;

    private static final String BY_ITEM_UUID = ITEMS.formatted("l.uuid", "x.uuid");
    private static final String BY_LESSON_UUID = ITEMS.formatted("l.uuid", "l.uuid");

    private final NamedParameterJdbcTemplate jdbc;

    public CourseContentSearchSource(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<CourseContentSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(BY_ITEM_UUID, new MapSqlParameterSource("uuids", uuids), (rs, rowNum) -> document(rs));
    }

    /**
     * Keyset over {@code lessons.id}: each batch is the next lessons plus every content item, quiz and
     * assignment hanging off them, since all four item types live under a lesson.
     */
    @Override
    public SearchBatch<CourseContentSearchDocument> loadAfter(long lastId, int batchSize) {
        List<Map<String, Object>> lessons = jdbc.queryForList(
                "SELECT id, uuid FROM lessons WHERE id > :lastId ORDER BY id LIMIT :limit",
                new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)));
        if (lessons.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        long highestId = ((Number) lessons.getLast().get("id")).longValue();
        List<UUID> lessonUuids = lessons.stream().map(row -> (UUID) row.get("uuid")).toList();
        List<CourseContentSearchDocument> documents = jdbc.query(BY_LESSON_UUID,
                new MapSqlParameterSource("uuids", lessonUuids), (rs, rowNum) -> document(rs));
        return new SearchBatch<>(documents, highestId);
    }

    @Override
    public long countIndexable() {
        Long count = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM lessons l JOIN courses c ON c.uuid = l.course_uuid
                        WHERE c.parent_course_uuid IS NULL)
                     + (SELECT COUNT(*) FROM lesson_contents x JOIN lessons l ON l.uuid = x.lesson_uuid
                        JOIN courses c ON c.uuid = l.course_uuid WHERE c.parent_course_uuid IS NULL)
                     + (SELECT COUNT(*) FROM quizzes x JOIN lessons l ON l.uuid = x.lesson_uuid
                        JOIN courses c ON c.uuid = l.course_uuid WHERE c.parent_course_uuid IS NULL)
                     + (SELECT COUNT(*) FROM assignments x JOIN lessons l ON l.uuid = x.lesson_uuid
                        JOIN courses c ON c.uuid = l.course_uuid WHERE c.parent_course_uuid IS NULL)
                """, Map.of(), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(Lesson.class, Lesson::getUuid),
                SearchIndexTrigger.fanOut(Lesson.class, lesson -> keyOf("lesson", lesson.getUuid(), null)),
                SearchIndexTrigger.direct(LessonContent.class, LessonContent::getUuid),
                SearchIndexTrigger.direct(Quiz.class, Quiz::getUuid),
                SearchIndexTrigger.direct(Assignment.class, Assignment::getUuid),
                SearchIndexTrigger.fanOut(Course.class,
                        course -> keyOf("course", course.getUuid(), course.getParentCourseUuid())));
    }

    /**
     * {@code lesson:<uuid>} resolves to the lesson's children; {@code course:<uuid>[,<parent uuid>]} to
     * every item of the course and, for a shadow draft, of the live course it edits.
     */
    @Override
    public Set<UUID> resolveFanOut(String key) {
        int separator = key.indexOf(':');
        if (separator < 0) {
            return Set.of();
        }
        List<UUID> uuids = new ArrayList<>();
        try {
            for (String part : key.substring(separator + 1).split(",")) {
                uuids.add(UUID.fromString(part));
            }
        } catch (IllegalArgumentException ex) {
            return Set.of();
        }
        String sql = switch (key.substring(0, separator)) {
            case "lesson" -> """
                    SELECT uuid FROM lesson_contents WHERE lesson_uuid IN (:uuids)
                    UNION ALL SELECT uuid FROM quizzes WHERE lesson_uuid IN (:uuids)
                    UNION ALL SELECT uuid FROM assignments WHERE lesson_uuid IN (:uuids)
                    """;
            case "course" -> """
                    WITH course_lessons AS (SELECT uuid FROM lessons WHERE course_uuid IN (:uuids))
                    SELECT uuid FROM course_lessons
                    UNION ALL SELECT uuid FROM lesson_contents WHERE lesson_uuid IN (SELECT uuid FROM course_lessons)
                    UNION ALL SELECT uuid FROM quizzes WHERE lesson_uuid IN (SELECT uuid FROM course_lessons)
                    UNION ALL SELECT uuid FROM assignments WHERE lesson_uuid IN (SELECT uuid FROM course_lessons)
                    """;
            default -> null;
        };
        if (sql == null) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(sql, new MapSqlParameterSource("uuids", uuids), UUID.class));
    }

    private static String keyOf(String kind, UUID uuid, UUID parentUuid) {
        if (uuid == null) {
            return null;
        }
        return parentUuid == null ? kind + ":" + uuid : kind + ":" + uuid + "," + parentUuid;
    }

    private static CourseContentSearchDocument document(ResultSet rs) throws SQLException {
        int lessonNumber = rs.getInt("lesson_number");
        Integer lessonNumberOrNull = rs.wasNull() ? null : lessonNumber;
        int displayOrder = rs.getInt("display_order");
        Integer displayOrderOrNull = rs.wasNull() ? null : displayOrder;
        return new CourseContentSearchDocument(
                SearchRows.uuid(rs, "uuid"),
                rs.getString("type"),
                SearchRows.uuid(rs, "course_uuid"),
                SearchRows.uuid(rs, "lesson_uuid"),
                lessonNumberOrNull,
                rs.getString("lesson_title"),
                rs.getString("course_name"),
                rs.getString("title"),
                SearchRows.truncate(body(rs.getString("part1"), rs.getString("part2"))),
                rs.getString("content_type"),
                rs.getString("scope"),
                SearchRows.uuid(rs, "class_definition_uuid"),
                SearchRows.flag(rs, "published"),
                displayOrderOrNull,
                SearchRows.nullableLong(rs, "updated_at"));
    }

    /** The two text parts as plain text: markup stripped, whitespace collapsed, blanks dropped. */
    static String body(String first, String second) {
        String a = plain(first);
        String b = plain(second);
        if (a == null) {
            return b;
        }
        return b == null ? a : a + "\n" + b;
    }

    private static String plain(String text) {
        if (text == null) {
            return null;
        }
        String stripped = WHITESPACE.matcher(TAGS.matcher(text).replaceAll(" ")).replaceAll(" ").trim();
        return stripped.isEmpty() ? null : stripped;
    }
}
