package apps.sarafrika.elimika.profile.integration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds the schema up to the empty user_* tables, seeds overlapping instructor and course creator
 * qualifications for one user, then runs the backfill and checks what each user ends up with.
 */
@Testcontainers
@DisplayName("The professional profile backfill")
class ProfessionalProfileBackfillMigrationTest {

    private static final String SCHEMA_BEFORE_THE_BACKFILL = "202610061243";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID BOTH_USER = UUID.randomUUID();
    private static final UUID CREATOR_USER = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CREATOR = UUID.randomUUID();
    private static final UUID SOLO_CREATOR = UUID.randomUUID();
    private static final UUID INSTRUCTOR_JAVA = UUID.randomUUID();
    private static final UUID CREATOR_JAVA = UUID.randomUUID();
    private static final UUID INSTRUCTOR_BSC = UUID.randomUUID();
    private static final UUID CREATOR_BSC = UUID.randomUUID();
    private static final UUID INSTRUCTOR_DOC = UUID.randomUUID();
    private static final UUID CREATOR_DOC = UUID.randomUUID();
    private static final UUID CREATOR_DUPLICATE_DOC = UUID.randomUUID();

    @BeforeAll
    static void seedLegacyThenMigrate() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE_THE_BACKFILL)).load().migrate();
        try (Connection c = connect()) {
            user(c, BOTH_USER, "both");
            user(c, CREATOR_USER, "solo");
            update(c, "INSERT INTO instructors (uuid, user_uuid, full_name, bio, professional_headline, location_name, "
                    + "lat, long, created_by) VALUES (?, ?, 'Both', '  ', 'Instructor headline', 'Kisumu', -0.09, 34.76, "
                    + "'test')", INSTRUCTOR, BOTH_USER);
            update(c, "INSERT INTO course_creators (uuid, user_uuid, full_name, bio, professional_headline, website, "
                    + "location_name, created_by) VALUES (?, ?, 'Both', 'Creator bio', 'Creator headline', "
                    + "'https://creator.example', 'Nairobi', 'test')", CREATOR, BOTH_USER);
            update(c, "INSERT INTO course_creators (uuid, user_uuid, full_name, bio, created_by) "
                    + "VALUES (?, ?, 'Solo', 'Only a creator', 'test')", SOLO_CREATOR, CREATOR_USER);

            update(c, "INSERT INTO instructor_skills (uuid, instructor_uuid, skill_name, proficiency_level, created_by) "
                    + "VALUES (?, ?, 'Java Programming', 'EXPERT', 'test')", INSTRUCTOR_JAVA, INSTRUCTOR);
            update(c, "INSERT INTO instructor_skills (uuid, instructor_uuid, skill_name, proficiency_level, created_by) "
                    + "VALUES (gen_random_uuid(), ?, 'Python', 'ADVANCED', 'test')", INSTRUCTOR);
            update(c, "INSERT INTO course_creator_skills (uuid, course_creator_uuid, skill_name, proficiency_level, "
                    + "evidence, verification_status, verified_at, created_by) VALUES (?, ?, ' java   programming', "
                    + "'BEGINNER', 'github.com/both', 'VERIFIED', now(), 'test')", CREATOR_JAVA, CREATOR);
            update(c, "INSERT INTO course_creator_skills (uuid, course_creator_uuid, skill_name, proficiency_level, "
                    + "created_by) VALUES (gen_random_uuid(), ?, 'Public Speaking', 'INTERMEDIATE', 'test')", CREATOR);

            update(c, "INSERT INTO instructor_education (uuid, instructor_uuid, qualification, school_name, "
                    + "year_completed, created_by) VALUES (?, ?, 'BSc', 'UoN', 2015, 'test')", INSTRUCTOR_BSC, INSTRUCTOR);
            update(c, "INSERT INTO course_creator_education (uuid, course_creator_uuid, qualification, school_name, "
                    + "year_completed, field_of_study, created_by) VALUES (?, ?, 'bsc', ' UoN ', 2015, 'Computing', "
                    + "'test')", CREATOR_BSC, CREATOR);
            update(c, "INSERT INTO course_creator_education (uuid, course_creator_uuid, qualification, school_name, "
                    + "year_completed, created_by) VALUES (gen_random_uuid(), ?, 'MSc', 'JKUAT', 2019, 'test')", CREATOR);

            document(c, "instructor_documents", "instructor_uuid", INSTRUCTOR_DOC, INSTRUCTOR, INSTRUCTOR_BSC,
                    "profile_documents/instructors/bsc.pdf", false);
            document(c, "course_creator_documents", "course_creator_uuid", CREATOR_DOC, CREATOR, CREATOR_BSC,
                    "profile_documents/course-creators/bsc-scan.pdf", false);
            document(c, "course_creator_documents", "course_creator_uuid", CREATOR_DUPLICATE_DOC, CREATOR, null,
                    "profile_documents/instructors/bsc.pdf", true);

            update(c, "INSERT INTO course_creator_certifications (course_creator_uuid, certification_name, "
                    + "issuing_organization, is_verified, created_by) VALUES (?, 'AWS SAA', 'AWS', TRUE, 'test')", CREATOR);
            update(c, "INSERT INTO course_creator_competencies (course_creator_uuid, competency, framework, level, "
                    + "created_by) VALUES (?, 'Facilitation', 'KICD', 3, 'test')", CREATOR);
            update(c, "INSERT INTO course_creator_portfolio_items (course_creator_uuid, title, item_type, created_by) "
                    + "VALUES (?, 'Robotics kit', 'project', 'test')", CREATOR);
        }
        flyway().load().migrate();
    }

    @Test
    @DisplayName("identical skills collapse into the instructor row, keeping the creator's evidence and verdict")
    void skillsCollapse() throws SQLException {
        List<List<String>> rows = rows("SELECT uuid::text, skill_name, proficiency_level, COALESCE(evidence, ''), "
                + "verification_status FROM user_skills WHERE user_uuid = ? ORDER BY skill_name", BOTH_USER);
        assertThat(rows).containsExactly(
                List.of(INSTRUCTOR_JAVA.toString(), "Java Programming", "EXPERT", "github.com/both", "VERIFIED"),
                List.of(rows.get(1).get(0), "Public Speaking", "INTERMEDIATE", "", "PENDING"),
                List.of(rows.get(2).get(0), "Python", "ADVANCED", "", "PENDING"));
    }

    @Test
    @DisplayName("identical education collapses, and documents follow the row they collapsed into")
    void educationAndDocumentsCollapse() throws SQLException {
        assertThat(rows("SELECT uuid::text, qualification, COALESCE(field_of_study, '') FROM user_education "
                + "WHERE user_uuid = ? ORDER BY qualification", BOTH_USER)).containsExactly(
                List.of(INSTRUCTOR_BSC.toString(), "BSc", "Computing"),
                List.of(rows("SELECT uuid::text FROM user_education WHERE qualification = 'MSc'").get(0).get(0),
                        "MSc", ""));

        List<List<String>> documents = rows("SELECT uuid::text, file_path, COALESCE(education_uuid::text, ''), "
                + "is_verified::text FROM user_documents WHERE user_uuid = ? ORDER BY file_path", BOTH_USER);
        assertThat(documents).containsExactly(
                List.of(CREATOR_DOC.toString(), "profile_documents/course-creators/bsc-scan.pdf",
                        INSTRUCTOR_BSC.toString(), "false"),
                List.of(CREATOR_DUPLICATE_DOC.toString(), "profile_documents/instructors/bsc.pdf", "", "true"));
        assertThat(rows("SELECT owner_type FROM media_files WHERE file_key = 'profile_documents/instructors/bsc.pdf'"))
                .containsExactly(List.of("USER_DOCUMENT"));
    }

    @Test
    @DisplayName("basics prefer non-blank values, instructor first, and both domain rows take the result")
    void basicsMerge() throws SQLException {
        List<String> expected = List.of("Creator bio", "Instructor headline", "https://creator.example", "Kisumu");
        assertThat(rows("SELECT bio, professional_headline, website, location_name FROM user_professional_profiles "
                + "WHERE user_uuid = ?", BOTH_USER)).containsExactly(expected);
        assertThat(rows("SELECT bio, professional_headline, website, location_name FROM instructors WHERE uuid = ?",
                INSTRUCTOR)).containsExactly(expected);
        assertThat(rows("SELECT bio, professional_headline, website, location_name FROM course_creators WHERE uuid = ?",
                CREATOR)).containsExactly(expected);
        assertThat(rows("SELECT bio FROM user_professional_profiles WHERE user_uuid = ?", CREATOR_USER))
                .containsExactly(List.of("Only a creator"));
    }

    @Test
    @DisplayName("course creator wallet sections carry over, with verified credentials still verified")
    void walletCarriesOver() throws SQLException {
        assertThat(rows("SELECT certification_name, verification_status FROM user_certifications WHERE user_uuid = ?",
                BOTH_USER)).containsExactly(List.of("AWS SAA", "VERIFIED"));
        assertThat(rows("SELECT competency, verification_status FROM user_competencies WHERE user_uuid = ?", BOTH_USER))
                .containsExactly(List.of("Facilitation", "PENDING"));
        assertThat(rows("SELECT title, item_type FROM user_portfolio_items WHERE user_uuid = ?", BOTH_USER))
                .containsExactly(List.of("Robotics kit", "PROJECT"));
    }

    @Test
    @DisplayName("the legacy tables are kept")
    void legacyTablesStay() throws SQLException {
        assertThat(rows("SELECT count(*)::text FROM instructor_skills")).containsExactly(List.of("2"));
        assertThat(rows("SELECT count(*)::text FROM course_creator_skills")).containsExactly(List.of("2"));
    }

    private static void user(Connection c, UUID uuid, String name) throws SQLException {
        try (PreparedStatement insert = c.prepareStatement("INSERT INTO users (uuid, user_no, first_name, last_name, "
                + "email, created_by) VALUES (?, ?, ?, 'Test', ?, 'test')")) {
            insert.setObject(1, uuid);
            insert.setString(2, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000));
            insert.setString(3, name);
            insert.setString(4, uuid + "@test.local");
            insert.executeUpdate();
        }
    }

    private static void document(Connection c, String table, String ownerColumn, UUID uuid, UUID owner,
                                 UUID educationUuid, String path, boolean verified) throws SQLException {
        try (PreparedStatement insert = c.prepareStatement("INSERT INTO " + table + " (uuid, " + ownerColumn
                + ", document_type_uuid, education_uuid, original_filename, stored_filename, file_path, "
                + "file_size_bytes, mime_type, is_verified, created_by) VALUES (?, ?, (SELECT uuid FROM document_types "
                + "ORDER BY id LIMIT 1), ?, 'scan.pdf', ?, ?, 10, 'application/pdf', ?, 'test')")) {
            insert.setObject(1, uuid);
            insert.setObject(2, owner);
            insert.setObject(3, educationUuid);
            insert.setString(4, path + "#" + table);
            insert.setString(5, path);
            insert.setBoolean(6, verified);
            insert.executeUpdate();
        }
    }

    private static void update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
        }
    }

    private static List<List<String>> rows(String sql, Object... params) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement statement = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                int columns = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= columns; i++) {
                        row.add(rs.getString(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
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
