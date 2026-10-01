package apps.sarafrika.elimika.course.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds the schema up to just before the dedupe, seeds the duplicated requirements staging showed
 * (each of three learner requirements listed twice), migrates, and checks that one of each is left -
 * the oldest - with the rows pointing at the removed copies moved onto it, and that the new
 * constraint refuses another copy.
 */
@Testcontainers
@DisplayName("course_training_requirements is deduplicated and guarded")
class CourseTrainingRequirementDedupeMigrationTest {

    private static final String SCHEMA_BEFORE = "202610011146";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID OTHER_COURSE = UUID.randomUUID();
    private static final UUID DRAFT = UUID.randomUUID();
    private static final UUID APPLICATION = UUID.randomUUID();
    private static final UUID OTHER_APPLICATION = UUID.randomUUID();

    private static UUID ppeKept;
    private static UUID ppeDuplicate;
    private static UUID toolsKept;
    private static UUID toolsDuplicate;
    private static UUID draftCopy;

    @BeforeAll
    static void seedThenMigrate() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE)).load().migrate();

        try (Connection connection = connect()) {
            UUID userUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                    + "VALUES (?, '000000001', 'Course', 'Creator', 'creator@example.com', 'kc-creator', 'test')", userUuid);
            UUID creatorUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                    + "VALUES (?, ?, 'Course Creator', 'test')", creatorUuid, userUuid);
            for (UUID course : List.of(COURSE, OTHER_COURSE)) {
                execute(connection, "INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                        + "VALUES (?, ?, ?, 'published', true, 'test')", course, "Course " + course, creatorUuid);
            }
            execute(connection, "INSERT INTO courses (uuid, name, course_creator_uuid, status, active, parent_course_uuid, "
                    + "created_by) VALUES (?, 'Draft', ?, 'draft', false, ?, 'test')", DRAFT, creatorUuid, COURSE);

            ppeKept = requirement(connection, COURSE, "PPE suit", "equipment", 1, "student");
            toolsKept = requirement(connection, COURSE, "Gardening tool set", "equipment", null, "student");
            requirement(connection, COURSE, "Measuring tools set", "equipment", 1, null);
            ppeDuplicate = requirement(connection, COURSE, "PPE suit", "equipment", 1, "student");
            toolsDuplicate = requirement(connection, COURSE, "Gardening tool set", "equipment", null, "student");
            requirement(connection, COURSE, "Measuring tools set", "equipment", 1, null);
            // Not duplicates: another quantity, another provider side, another course.
            requirement(connection, COURSE, "PPE suit", "equipment", 2, "student");
            requirement(connection, COURSE, "PPE suit", "equipment", 1, "instructor");
            requirement(connection, OTHER_COURSE, "PPE suit", "equipment", 1, "student");

            // A pending draft copy promoting onto the copy that is about to go.
            draftCopy = requirement(connection, DRAFT, "PPE suit", "equipment", 1, "student");
            execute(connection, "UPDATE course_training_requirements SET source_requirement_uuid = ? WHERE uuid = ?",
                    ppeDuplicate, draftCopy);

            // One application answered only the copy; another answered both copies.
            answer(connection, APPLICATION, ppeDuplicate);
            answer(connection, OTHER_APPLICATION, toolsKept);
            answer(connection, OTHER_APPLICATION, toolsDuplicate);
        }

        flyway().load().migrate();
    }

    @Test
    @DisplayName("each exact duplicate group keeps one row, the oldest")
    void keepsTheOldestOfEachGroup() throws SQLException {
        assertThat(names(COURSE)).containsExactlyInAnyOrder(
                "PPE suit|equipment|1|student",
                "Gardening tool set|equipment|null|student",
                "Measuring tools set|equipment|1|null",
                "PPE suit|equipment|2|student",
                "PPE suit|equipment|1|instructor");
        assertThat(exists(ppeKept)).isTrue();
        assertThat(exists(toolsKept)).isTrue();
        assertThat(exists(ppeDuplicate)).isFalse();
        assertThat(exists(toolsDuplicate)).isFalse();
        assertThat(names(OTHER_COURSE)).containsExactly("PPE suit|equipment|1|student");
    }

    @Test
    @DisplayName("draft links and application answers move onto the kept row")
    void referencesMoveToTheKeptRow() throws SQLException {
        assertThat(single("SELECT source_requirement_uuid FROM course_training_requirements WHERE uuid = ?", draftCopy))
                .isEqualTo(ppeKept);
        assertThat(single("SELECT requirement_uuid FROM training_application_requirement_answers "
                + "WHERE application_uuid = ?", APPLICATION)).isEqualTo(ppeKept);
        assertThat(single("SELECT COUNT(*) FROM training_application_requirement_answers WHERE application_uuid = ?",
                OTHER_APPLICATION)).isEqualTo(1L);
    }

    @Test
    @DisplayName("the constraint refuses another copy, including one with a missing quantity")
    void guardRefusesNewDuplicates() throws SQLException {
        try (Connection connection = connect()) {
            assertThatThrownBy(() -> requirement(connection, COURSE, "PPE suit", "equipment", 1, "student"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uq_course_training_requirements_identity");
            assertThatThrownBy(() -> requirement(connection, COURSE, "Gardening tool set", "equipment", null, "student"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uq_course_training_requirements_identity");
            // A different quantity is a different requirement.
            UUID different = requirement(connection, COURSE, "PPE suit", "equipment", 3, "student");
            execute(connection, "DELETE FROM course_training_requirements WHERE uuid = ?", different);
        }
    }

    private static UUID requirement(Connection connection, UUID course, String name, String type, Integer quantity,
                                    String providedBy) throws SQLException {
        UUID uuid = UUID.randomUUID();
        execute(connection, "INSERT INTO course_training_requirements (uuid, course_uuid, requirement_type, name, "
                + "quantity, provided_by, created_by) VALUES (?, ?, ?, ?, ?, ?, 'test')",
                uuid, course, type, name, quantity, providedBy);
        return uuid;
    }

    private static void answer(Connection connection, UUID application, UUID requirement) throws SQLException {
        execute(connection, "INSERT INTO training_application_requirement_answers (application_type, application_uuid, "
                + "requirement_uuid, has_it, created_by) VALUES ('COURSE', ?, ?, true, 'test')", application, requirement);
    }

    private static List<String> names(UUID course) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT name, requirement_type, quantity, provided_by FROM course_training_requirements "
                             + "WHERE course_uuid = ?")) {
            query.setObject(1, course);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getObject(3) + "|" + rs.getString(4));
                }
            }
        }
        return rows;
    }

    private static boolean exists(UUID requirement) throws SQLException {
        return ((Number) single("SELECT COUNT(*) FROM course_training_requirements WHERE uuid = ?", requirement))
                .longValue() == 1L;
    }

    private static Object single(String sql, UUID parameter) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setObject(1, parameter);
            try (ResultSet rs = query.executeQuery()) {
                rs.next();
                return rs.getObject(1);
            }
        }
    }

    private static FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(true);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static void execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        }
    }
}
