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

// Builds the schema up to just before course_prerequisites, seeds program sequences, then migrates.
@Testcontainers
@DisplayName("course_prerequisites is backfilled from program sequences")
class CoursePrerequisiteBackfillMigrationTest {

    private static final String SCHEMA_BEFORE = "202609291433";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID BASICS = UUID.randomUUID();
    private static final UUID INTERMEDIATE = UUID.randomUUID();
    private static final UUID ADVANCED = UUID.randomUUID();
    private static final UUID LOOP_X = UUID.randomUUID();
    private static final UUID LOOP_Y = UUID.randomUUID();

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
            for (UUID course : List.of(BASICS, INTERMEDIATE, ADVANCED, LOOP_X, LOOP_Y)) {
                execute(connection, "INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                        + "VALUES (?, ?, ?, 'published', true, 'test')", course, "Course " + course, creatorUuid);
            }

            // Two programs repeat the Basics -> Intermediate step: one row comes out.
            UUID first = program(connection, creatorUuid, "Python track");
            programCourse(connection, first, BASICS, 1, null);
            programCourse(connection, first, INTERMEDIATE, 2, BASICS);
            programCourse(connection, first, ADVANCED, 3, INTERMEDIATE);
            UUID second = program(connection, creatorUuid, "Data track");
            programCourse(connection, second, BASICS, 1, null);
            programCourse(connection, second, INTERMEDIATE, 2, BASICS);

            // Two programs that disagree on the order would make a cycle: neither pair is taken.
            UUID loopOne = program(connection, creatorUuid, "Loop one");
            programCourse(connection, loopOne, LOOP_X, 1, null);
            programCourse(connection, loopOne, LOOP_Y, 2, LOOP_X);
            UUID loopTwo = program(connection, creatorUuid, "Loop two");
            programCourse(connection, loopTwo, LOOP_Y, 1, null);
            programCourse(connection, loopTwo, LOOP_X, 2, LOOP_Y);
        }

        flyway().load().migrate();
    }

    @Test
    @DisplayName("every program step becomes one mandatory prerequisite; contradictory steps are skipped")
    void backfillsMandatoryPairs() throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT course_uuid, prerequisite_course_uuid, is_mandatory FROM course_prerequisites");
             ResultSet rs = query.executeQuery()) {
            while (rs.next()) {
                rows.add(rs.getObject(1) + "<-" + rs.getObject(2) + ":" + rs.getBoolean(3));
            }
        }
        assertThat(rows).containsExactlyInAnyOrder(
                INTERMEDIATE + "<-" + BASICS + ":true",
                ADVANCED + "<-" + INTERMEDIATE + ":true");
    }

    private static UUID program(Connection connection, UUID creatorUuid, String title) throws SQLException {
        UUID uuid = UUID.randomUUID();
        execute(connection, "INSERT INTO training_programs (uuid, title, course_creator_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", uuid, title, creatorUuid);
        return uuid;
    }

    private static void programCourse(Connection connection, UUID program, UUID course, int order, UUID prerequisite)
            throws SQLException {
        execute(connection, "INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, "
                        + "prerequisite_course_uuid, created_by) VALUES (?, ?, ?, ?, ?, 'test')",
                UUID.randomUUID(), program, course, order, prerequisite);
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
