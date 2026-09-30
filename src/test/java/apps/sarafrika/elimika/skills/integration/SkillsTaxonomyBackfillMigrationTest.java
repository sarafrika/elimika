package apps.sarafrika.elimika.skills.integration;

import apps.sarafrika.elimika.skills.spi.SkillSlugs;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds the schema up to the migration before the taxonomy, seeds free-text instructor and course
 * creator skills, then runs the seed and backfill.
 */
@Testcontainers
@DisplayName("The skills taxonomy seed and instructor_skills backfill")
class SkillsTaxonomyBackfillMigrationTest {

    private static final String SCHEMA_BEFORE_THE_TAXONOMY = "202609291433";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID INSTRUCTOR_A = UUID.randomUUID();
    private static final UUID INSTRUCTOR_B = UUID.randomUUID();
    private static final UUID CREATOR = UUID.randomUUID();

    @BeforeAll
    static void seedFreeTextThenMigrate() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE_THE_TAXONOMY)).load().migrate();
        try (Connection connection = connect()) {
            // The profiles reference users the backfill never reads.
            connection.createStatement().execute("SET session_replication_role = replica");
            execute(connection, "INSERT INTO instructors (uuid, user_uuid, full_name, created_by) "
                    + "VALUES (?, gen_random_uuid(), 'A', 'test')", INSTRUCTOR_A);
            execute(connection, "INSERT INTO instructors (uuid, user_uuid, full_name, created_by) "
                    + "VALUES (?, gen_random_uuid(), 'B', 'test')", INSTRUCTOR_B);
            execute(connection, "INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                    + "VALUES (?, gen_random_uuid(), 'C', 'test')", CREATOR);
            skill(connection, "instructor_skills", "instructor_uuid", INSTRUCTOR_A, "Java Programming");
            skill(connection, "instructor_skills", "instructor_uuid", INSTRUCTOR_B, "java  programming ");
            skill(connection, "instructor_skills", "instructor_uuid", INSTRUCTOR_B, "Java Programming");
            skill(connection, "instructor_skills", "instructor_uuid", INSTRUCTOR_A, "C++ / Embedded");
            skill(connection, "instructor_skills", "instructor_uuid", INSTRUCTOR_A, "日本語");
            skill(connection, "course_creator_skills", "course_creator_uuid", CREATOR, "Public Speaking");
        }
        flyway().load().migrate();
    }

    @Test
    @DisplayName("one skill per distinct slug, named by the most used spelling, only for names in use")
    void seedsOneSkillPerSlug() throws SQLException {
        Map<String, String> skills = query("SELECT slug, name FROM skills ORDER BY slug");
        assertThat(skills).containsExactly(
                Map.entry("c-embedded", "C++ / Embedded"),
                Map.entry("java-programming", "Java Programming"),
                Map.entry("public-speaking", "Public Speaking"));
    }

    @Test
    @DisplayName("instructor skills link by slug; a name with no slug keeps its free text and no link")
    void backfillsInstructorSkills() throws SQLException {
        Map<String, String> links = query("""
                SELECT i.skill_name, COALESCE(s.slug, '<none>')
                FROM instructor_skills i LEFT JOIN skills s ON s.uuid = i.skill_uuid
                ORDER BY i.id""");
        assertThat(links).containsExactly(
                Map.entry("Java Programming", "java-programming"),
                Map.entry("java  programming ", "java-programming"),
                Map.entry("C++ / Embedded", "c-embedded"),
                Map.entry("日本語", "<none>"));
    }

    @Test
    @DisplayName("the SQL slug and SkillSlugs agree")
    void sqlAndJavaSlugsAgree() {
        assertThat(SkillSlugs.slugify("java  programming ")).isEqualTo("java-programming");
        assertThat(SkillSlugs.slugify("C++ / Embedded")).isEqualTo("c-embedded");
        assertThat(SkillSlugs.slugify("日本語")).isEmpty();
    }

    private static Map<String, String> query(String sql) throws SQLException {
        Map<String, String> rows = new LinkedHashMap<>();
        try (Connection connection = connect(); ResultSet rs = connection.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getString(2));
            }
        }
        return rows;
    }

    private static void skill(Connection connection, String table, String ownerColumn, UUID owner, String name)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " (" + ownerColumn
                + ", skill_name, proficiency_level, created_by) VALUES (?, ?, 'BEGINNER', 'test')")) {
            insert.setObject(1, owner);
            insert.setString(2, name);
            insert.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.executeUpdate();
        }
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(true);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
