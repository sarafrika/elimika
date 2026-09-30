package apps.sarafrika.elimika.shared.integration;

import static org.assertj.core.api.Assertions.assertThat;

import db.migration.V202609301821__redact_historical_audit_query_strings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Builds the schema up to the migration before the redaction, seeds audit rows, then migrates. */
@Testcontainers
@DisplayName("Historical request_audit_log query strings are redacted in place")
class AuditQueryStringRedactionMigrationTest {

    private static final String SCHEMA_BEFORE_THE_REDACTION = "202609301820";
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 15, 8, 30, 0);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 1, 15, 8, 30, 5);
    private static final UUID SEARCH_ROW = UUID.randomUUID();
    private static final UUID PLAIN_ROW = UUID.randomUUID();
    private static final UUID NULL_ROW = UUID.randomUUID();
    private static final int BULK_ROWS = 2_345;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @BeforeAll
    static void seedThenMigrate() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE_THE_REDACTION)).load().migrate();
        try (Connection connection = connect()) {
            audit(connection, SEARCH_ROW, "/api/v1/users/search",
                    "q=jane+doe&page=0&email_eq=jane%40x.co&first_name_like=ja&lat=-1.29&lng=36.82", 200);
            audit(connection, PLAIN_ROW, "/api/v1/admin/organizations/x/moderate", "action=approve&size=20", 204);
            audit(connection, NULL_ROW, "/api/v1/courses", null, 500);
            try (Statement statement = connection.createStatement()) {
                // Enough rows to cross two batch boundaries of the 1000-row walk.
                statement.execute("""
                        INSERT INTO request_audit_log (request_id, http_method, request_uri, query_string, ip_address,
                                                       response_status, authentication_name, created_date, created_by,
                                                       updated_date)
                        SELECT 'bulk-' || n, 'GET', '/api/v1/search', 'q=term' || n || '&limit=5', '10.0.0.1', 200,
                               'bulk-user', TIMESTAMP '2026-01-15 08:30:00', 'test', TIMESTAMP '2026-01-15 08:30:05'
                        FROM generate_series(1, %d) AS n""".formatted(BULK_ROWS));
            }
        }
        flyway().load().migrate();
    }

    @Test
    @DisplayName("sensitive values are masked with their length; everything else is byte-identical")
    void redactsSensitiveValues() throws SQLException {
        assertThat(queryString(SEARCH_ROW)).isEqualTo(
                "q=[redacted:8]&page=0&email_eq=[redacted:9]&first_name_like=[redacted:2]&lat=[redacted:5]&lng=[redacted:5]");
        assertThat(queryString(PLAIN_ROW)).isEqualTo("action=approve&size=20");
        assertThat(queryString(NULL_ROW)).isNull();
    }

    @Test
    @DisplayName("actor, URI, status and time are untouched")
    void keepsTheRestOfTheRow() throws SQLException {
        try (Connection connection = connect();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT request_uri, response_status, authentication_name, user_email, created_date, updated_date,
                            created_by, updated_by
                     FROM request_audit_log WHERE uuid = ?""")) {
            select.setObject(1, SEARCH_ROW);
            try (ResultSet row = select.executeQuery()) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString("request_uri")).isEqualTo("/api/v1/users/search");
                assertThat(row.getInt("response_status")).isEqualTo(200);
                assertThat(row.getString("authentication_name")).isEqualTo("actor-subject");
                assertThat(row.getString("user_email")).isEqualTo("actor@test.local");
                assertThat(row.getTimestamp("created_date").toLocalDateTime()).isEqualTo(CREATED);
                assertThat(row.getTimestamp("updated_date").toLocalDateTime()).isEqualTo(UPDATED);
                assertThat(row.getString("created_by")).isEqualTo("test");
                assertThat(row.getString("updated_by")).isNull();
            }
        }
    }

    @Test
    @DisplayName("every batch is covered, and the migration was recorded as applied")
    void coversEveryBatch() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("""
                    SELECT COUNT(*) FILTER (WHERE query_string LIKE 'q=[redacted:%]&limit=5') AS redacted,
                           COUNT(*) FILTER (WHERE updated_date <> TIMESTAMP '2026-01-15 08:30:05') AS touched
                    FROM request_audit_log WHERE request_id LIKE 'bulk-%'""")) {
                rows.next();
                assertThat(rows.getLong("redacted")).isEqualTo(BULK_ROWS);
                assertThat(rows.getLong("touched")).isZero();
            }
            try (ResultSet history = statement.executeQuery(
                    "SELECT type, success FROM flyway_schema_history WHERE version = '202609301821'")) {
                assertThat(history.next()).isTrue();
                assertThat(history.getString("type")).isEqualTo("JDBC");
                assertThat(history.getBoolean("success")).isTrue();
            }
        }
    }

    @Test
    @DisplayName("running the redaction again changes nothing")
    void isIdempotent() throws SQLException {
        String before = queryString(SEARCH_ROW);
        try (Connection connection = connect()) {
            assertThat(V202609301821__redact_historical_audit_query_strings.redactAll(connection)).isZero();
        }
        assertThat(queryString(SEARCH_ROW)).isEqualTo(before);
    }

    // --- fixtures -----------------------------------------------------------------------------

    private static FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(true);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static void audit(Connection connection, UUID uuid, String uri, String queryString, int status)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO request_audit_log (uuid, request_id, http_method, request_uri, query_string, ip_address,
                                               response_status, authentication_name, user_email, created_date,
                                               created_by, updated_date)
                VALUES (?, ?, 'GET', ?, ?, '10.0.0.1', ?, 'actor-subject', 'actor@test.local', ?, 'test', ?)""")) {
            insert.setObject(1, uuid);
            insert.setString(2, uuid.toString().substring(0, 8));
            insert.setString(3, uri);
            insert.setString(4, queryString);
            insert.setInt(5, status);
            insert.setTimestamp(6, Timestamp.valueOf(CREATED));
            insert.setTimestamp(7, Timestamp.valueOf(UPDATED));
            insert.executeUpdate();
        }
    }

    private static String queryString(UUID uuid) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT query_string FROM request_audit_log WHERE uuid = ?")) {
            select.setObject(1, uuid);
            try (ResultSet row = select.executeQuery()) {
                row.next();
                return row.getString(1);
            }
        }
    }
}
