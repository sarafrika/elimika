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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Builds the real schema up to the migration before the change, seeds zero rates, then migrates.
@Testcontainers
@DisplayName("Unoffered training methods migrate from zero to null")
class UnofferedTrainingMethodMigrationTest {

    private static final String SCHEMA_BEFORE = "202609170845";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID COURSE_APPLICATION = UUID.randomUUID();
    private static final UUID PROGRAM_APPLICATION = UUID.randomUUID();
    private static UUID courseUuid;

    @BeforeAll
    static void seedZerosThenMigrate() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE)).load().migrate();

        try (Connection connection = connect()) {
            UUID userUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                    + "VALUES (?, '000000001', 'Course', 'Creator', 'creator@example.com', 'kc-creator', 'test')", userUuid);
            UUID creatorUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                    + "VALUES (?, ?, 'Course Creator', 'test')", creatorUuid, userUuid);
            courseUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                    + "VALUES (?, 'Welding', ?, 'published', true, 'test')", courseUuid, creatorUuid);
            UUID programUuid = UUID.randomUUID();
            execute(connection, "INSERT INTO training_programs (uuid, title, course_creator_uuid, created_by) "
                    + "VALUES (?, 'Fabrication', ?, 'test')", programUuid, creatorUuid);

            // Group online is the only method offered; the other three were sent as zero.
            execute(connection, "INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                    + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, group_online_hourly_rate, "
                    + "group_inperson_hourly_rate, private_online_session_rate, group_online_session_rate, group_online_daily_rate, "
                    + "private_inperson_daily_rate, created_by) "
                    + "VALUES (?, ?, 'instructor', ?, 'approved', 'KES', 0, 0, 2500, 0, 0, 4000, 9000, 0, 'test')",
                    COURSE_APPLICATION, courseUuid, UUID.randomUUID());
            execute(connection, "INSERT INTO program_training_applications (uuid, program_uuid, applicant_type, applicant_uuid, "
                    + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, group_online_hourly_rate, "
                    + "group_inperson_hourly_rate, group_inperson_session_rate, created_by) "
                    + "VALUES (?, ?, 'organisation', ?, 'pending', 'KES', 1800, 0, 0, 0, 0, 'test')",
                    PROGRAM_APPLICATION, programUuid, UUID.randomUUID());
        }

        flyway().load().migrate();
    }

    @Test
    @DisplayName("every zero rate on a course application becomes null and real prices are untouched")
    void courseZerosBecomeNull() throws SQLException {
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT * FROM course_training_applications WHERE uuid = ?")) {
            query.setObject(1, COURSE_APPLICATION);
            try (ResultSet row = query.executeQuery()) {
                assertThat(row.next()).isTrue();
                assertThat(row.getBigDecimal("private_online_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("private_inperson_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("group_inperson_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("private_online_session_rate")).isNull();
                assertThat(row.getBigDecimal("private_inperson_daily_rate")).isNull();
                assertThat(row.getBigDecimal("group_online_hourly_rate")).isEqualByComparingTo(new BigDecimal("2500"));
                assertThat(row.getBigDecimal("group_online_session_rate")).isEqualByComparingTo(new BigDecimal("4000"));
                assertThat(row.getBigDecimal("group_online_daily_rate")).isEqualByComparingTo(new BigDecimal("9000"));
            }
        }
    }

    @Test
    @DisplayName("every zero rate on a program application becomes null")
    void programZerosBecomeNull() throws SQLException {
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT * FROM program_training_applications WHERE uuid = ?")) {
            query.setObject(1, PROGRAM_APPLICATION);
            try (ResultSet row = query.executeQuery()) {
                assertThat(row.next()).isTrue();
                assertThat(row.getBigDecimal("private_online_hourly_rate")).isEqualByComparingTo(new BigDecimal("1800"));
                assertThat(row.getBigDecimal("private_inperson_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("group_online_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("group_inperson_hourly_rate")).isNull();
                assertThat(row.getBigDecimal("group_inperson_session_rate")).isNull();
            }
        }
    }

    @Test
    @DisplayName("hourly rates may now be null, and zero can no longer be stored")
    void zeroIsRefusedAfterTheMigration() throws SQLException {
        try (Connection connection = connect()) {
            execute(connection, "INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                    + "status, rate_currency, group_online_hourly_rate, created_by) "
                    + "VALUES (?, ?, 'instructor', ?, 'pending', 'KES', 3000, 'test')",
                    UUID.randomUUID(), courseUuid, UUID.randomUUID());

            assertThatThrownBy(() -> execute(connection, "INSERT INTO course_training_applications (uuid, course_uuid, "
                    + "applicant_type, applicant_uuid, status, rate_currency, group_online_hourly_rate, created_by) "
                    + "VALUES (?, ?, 'instructor', ?, 'pending', 'KES', 0, 'test')",
                    UUID.randomUUID(), courseUuid, UUID.randomUUID()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("chk_course_training_application_rates_positive");
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
