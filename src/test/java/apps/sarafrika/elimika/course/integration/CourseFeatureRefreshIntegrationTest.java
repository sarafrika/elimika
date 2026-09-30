package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.internal.recommend.CourseFeatureRefresher;
import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The nightly feature job against the real schema: the stats arithmetic, the co-enrolment privacy
 * thresholds (5 learners, or 10 when any is a minor) and the advisory-lock skip.
 * <p>
 * Minors are supplied by a stub of the shared SPI, since which learners are minors is the student and
 * tenancy modules' business; {@code MinorLearnerLookupServiceImplTest} covers that side.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseFeatureRefresher.class, CourseFeatureRefreshIntegrationTest.TestConfig.class})
@DisplayName("Course feature refresh job")
class CourseFeatureRefreshIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    /** Minor student uuids for the stub; filled per test. */
    static final Set<UUID> MINORS = new HashSet<>();

    static class TestConfig {
        @Bean
        MinorLearnerLookupService minorLearnerLookupService() {
            return (studentUuids, asOf) -> {
                Set<UUID> result = new HashSet<>(studentUuids);
                result.retainAll(MINORS);
                return result;
            };
        }
    }

    @Autowired private CourseFeatureRefresher refresher;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private UUID creatorUuid;

    @BeforeEach
    void seedCreator() {
        MINORS.clear();
        UUID userUuid = user();
        creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Creator', 'test')",
                creatorUuid, userUuid);
    }

    @Test
    @DisplayName("stats: counts, completion rate, progress, median days, 30-day enrolments and Bayesian rating")
    void statsMath() {
        UUID x = course("X");
        UUID y = course("Y");
        UUID z = course("Z");

        enrol(student(), x, "completed", 100, 10, 6);   // 4 days to complete
        enrol(student(), x, "completed", 100, 40, 30);  // 10 days to complete
        enrol(student(), x, "active", 50, 5, null);
        enrol(student(), x, "dropped", 20, 60, null);

        review(x, 5);
        review(x, 4);
        review(y, 1);

        assertThat(refresher.refresh(now).ran()).isTrue();

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM course_learning_stats WHERE course_uuid = ?", x);
        assertThat(row.get("enrolment_count")).isEqualTo(4);
        assertThat(row.get("active_count")).isEqualTo(1);
        assertThat(row.get("completed_count")).isEqualTo(2);
        assertThat(row.get("dropped_count")).isEqualTo(1);
        assertThat((BigDecimal) row.get("completion_rate")).isEqualByComparingTo("0.5000");
        assertThat((BigDecimal) row.get("avg_progress")).isEqualByComparingTo("67.50");
        assertThat((BigDecimal) row.get("median_days_to_complete")).isEqualByComparingTo("7.00");
        assertThat(row.get("enrolments_30d")).isEqualTo(2);
        // m = (5 + 4 + 1) / 3 = 3.3333; X = (5m + 9) / 7 = 3.6667
        assertThat((BigDecimal) row.get("rating_bayes")).isEqualByComparingTo("3.6667");

        // Y = (5m + 1) / 6 = 2.9444; Z has no reviews and sits at the prior.
        assertThat(jdbc.queryForObject("SELECT rating_bayes FROM course_learning_stats WHERE course_uuid = ?",
                BigDecimal.class, y)).isEqualByComparingTo("2.9444");
        Map<String, Object> empty = jdbc.queryForMap("SELECT * FROM course_learning_stats WHERE course_uuid = ?", z);
        assertThat((BigDecimal) empty.get("rating_bayes")).isEqualByComparingTo("3.3333");
        assertThat(empty.get("enrolment_count")).isEqualTo(0);
        assertThat(empty.get("completion_rate")).isNull();
    }

    @Test
    @DisplayName("co-enrolment: 6 adults appear, 6 with a minor do not, 10 with a minor do")
    void minorThresholds() {
        UUID a = course("A");
        UUID b = course("B");
        List<UUID> adults = students(6);
        adults.forEach(s -> { enrol(s, a, "active", 10, 1, null); enrol(s, b, "completed", 100, 20, 2); });

        UUID c = course("C");
        UUID d = course("D");
        List<UUID> sixWithMinor = students(6);
        MINORS.add(sixWithMinor.get(0));
        sixWithMinor.forEach(s -> { enrol(s, c, "active", 10, 1, null); enrol(s, d, "active", 10, 1, null); });

        UUID e = course("E");
        UUID f = course("F");
        List<UUID> tenWithMinor = students(10);
        MINORS.add(tenWithMinor.get(3));
        tenWithMinor.forEach(s -> { enrol(s, e, "active", 10, 1, null); enrol(s, f, "active", 10, 1, null); });

        // Dropped enrolments never count toward a pair.
        UUID g = course("G");
        UUID h = course("H");
        students(6).forEach(s -> { enrol(s, g, "dropped", 0, 1, null); enrol(s, h, "active", 0, 1, null); });

        refresher.refresh(now);

        Map<String, Object> ab = pair(a, b);
        assertThat(ab).isNotNull();
        assertThat(ab.get("shared_learners")).isEqualTo(6);
        assertThat(ab.get("includes_minors")).isEqualTo(false);
        assertThat((BigDecimal) ab.get("jaccard")).isEqualByComparingTo("1.00000");
        assertThat(pair(b, a)).isNotNull();

        assertThat(pair(c, d)).isNull();
        assertThat(pair(d, c)).isNull();

        Map<String, Object> ef = pair(e, f);
        assertThat(ef).isNotNull();
        assertThat(ef.get("shared_learners")).isEqualTo(10);
        assertThat(ef.get("includes_minors")).isEqualTo(true);

        assertThat(pair(g, h)).isNull();
        // lift for A-B: 6 shared * 28 learners in total / (6 * 6)
        long total = jdbc.queryForObject("SELECT COUNT(DISTINCT student_uuid) FROM course_enrollments "
                + "WHERE status IN ('active', 'completed')", Long.class);
        assertThat((BigDecimal) ab.get("lift"))
                .isEqualByComparingTo(BigDecimal.valueOf(6.0 * total / 36.0).setScale(4, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("a run skips, writing nothing, while another holds the advisory lock")
    void skipsWhenLocked() throws Exception {
        course("Locked");
        try (Connection other = dataSource.getConnection(); Statement statement = other.createStatement()) {
            statement.execute("SELECT pg_advisory_lock(" + lockKey() + ")");
            try {
                CourseFeatureRefresher.Outcome outcome = refresher.refresh(now);
                assertThat(outcome.ran()).isFalse();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course_learning_stats", Long.class)).isZero();
            } finally {
                statement.execute("SELECT pg_advisory_unlock(" + lockKey() + ")");
            }
        }
        assertThat(refresher.refresh(now).ran()).isTrue();
    }

    // ----------------------------------------------------------------- plumbing

    private static long lockKey() throws Exception {
        var field = CourseFeatureRefresher.class.getDeclaredField("LOCK_KEY");
        field.setAccessible(true);
        return field.getLong(null);
    }

    private Map<String, Object> pair(UUID course, UUID neighbour) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM course_co_enrolments WHERE course_uuid = ? AND neighbour_course_uuid = ?", course, neighbour);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UUID user() {
        UUID uuid = UUID.randomUUID();
        String tag = Long.toHexString(System.nanoTime());
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, 'test')",
                uuid, String.format("%09d", Math.floorMod(uuid.hashCode(), 1_000_000_000)), "u" + tag + "@t.io");
        return uuid;
    }

    private UUID student() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Learner', 'test')",
                uuid, user());
        return uuid;
    }

    private List<UUID> students(int count) {
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(student());
        }
        return result;
    }

    private UUID course(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, creatorUuid);
        return uuid;
    }

    private void enrol(UUID student, UUID course, String status, int progress, int enrolledDaysAgo,
                       Integer completedDaysAgo) {
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, progress_percentage, "
                        + "enrollment_date, completion_date, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                UUID.randomUUID(), student, course, status, progress,
                java.sql.Timestamp.from(now.minus(enrolledDaysAgo, ChronoUnit.DAYS)),
                completedDaysAgo == null ? null : java.sql.Timestamp.from(now.minus(completedDaysAgo, ChronoUnit.DAYS)));
    }

    private void review(UUID course, int rating) {
        jdbc.update("INSERT INTO course_reviews (uuid, course_uuid, student_uuid, rating, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", UUID.randomUUID(), course, student(), rating);
    }
}
