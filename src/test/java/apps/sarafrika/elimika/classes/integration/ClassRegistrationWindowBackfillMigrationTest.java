package apps.sarafrika.elimika.classes.integration;

import org.springframework.test.annotation.DirtiesContext;
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
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the backfill that makes the registration window mandatory cannot shut a class that is
 * taking enrolments today.
 * <p>
 * The window has never been enforced, so every class in production is enrollable right now whatever
 * these two columns say. That makes the backfill, not the enforcement, the part of this change that
 * can quietly take a live class off sale — and it is the part no mocked repository can test. So this
 * builds the real schema from the real migrations up to the one before the backfill, inserts a row
 * for every combination the old NULL-tolerant constraint permitted, runs the backfill, and reads the
 * rows back.
 * <p>
 * The first thing it proves is that the migration runs at all. An earlier draft dropped the old
 * constraint after the UPDATEs instead of before, so the constraint rejected the very rows the
 * backfill existed to repair; Flyway would have aborted, marked the migration failed and left the
 * application unable to start.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@DisplayName("The registration window backfill leaves every open class open")
class ClassRegistrationWindowBackfillMigrationTest {

    /** The migration immediately before the backfill: the schema production is on today. */
    private static final String SCHEMA_BEFORE_THE_BACKFILL = "202609041000";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate SENTINEL = LocalDate.of(2099, 12, 31);

    @BeforeAll
    static void seedTheOldSchemaThenRunTheBackfill() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE_THE_BACKFILL)).load().migrate();

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            // Every shape the old constraint allowed: either half NULL, or an ordered pair.
            seed(connection, "nulls, no academic period", null, null, null, null, 200);
            seed(connection, "nulls, live academic period", days(-30), days(100), null, null, 60);
            seed(connection, "nulls, future academic period", days(26), days(180), null, null, 10);
            seed(connection, "nulls, past academic period", days(-300), days(-60), null, null, 310);
            seed(connection, "stated open, no close", null, null, days(-20), null, 25);
            seed(connection, "stated open, no close, academic period already over", days(-300), days(-60), days(-20), null, 310);
            seed(connection, "stated open in the future, no close", days(-300), days(-60), days(40), null, 310);
            seed(connection, "stated close in the past, no open", null, null, null, days(-90), 10);
            seed(connection, "stated close in the future, no open", null, null, null, days(90), 10);
            seed(connection, "both stated, open today", null, null, days(-10), days(10), 30);
            seed(connection, "both stated, closed last month", null, null, days(-90), days(-30), 120);
            seed(connection, "both stated, opens next month", null, null, days(10), days(40), 5);

            // A pair the old constraint should never have let in. It is here so the new constraint
            // cannot abort on a row that arrived some other way.
            statement.execute("ALTER TABLE class_definitions DROP CONSTRAINT chk_class_definitions_registration_period_valid");
            seed(connection, "inverted pair already stored", null, null, days(20), days(-20), 40);
            statement.execute("""
                    ALTER TABLE class_definitions
                        ADD CONSTRAINT chk_class_definitions_registration_period_valid
                            CHECK (registration_period_start_date IS NULL
                                   OR registration_period_end_date IS NULL
                                   OR registration_period_start_date <= registration_period_end_date) NOT VALID""");
        }

        flyway().load().migrate();
    }

    @Test
    @DisplayName("a class with no window of its own is left open, whatever its academic period says")
    void classesWithNoStatedWindowStayOpen() {
        assertThat(openToday("nulls, no academic period")).isTrue();
        assertThat(openToday("nulls, live academic period")).isTrue();
        // The ordinary case for a class selling seats for next term: registration precedes the
        // academic period, so backfilling the window from that period would shut it until October.
        assertThat(openToday("nulls, future academic period")).isTrue();
        assertThat(openToday("nulls, past academic period")).isTrue();

        assertThat(window("nulls, no academic period").closesOn())
                .as("nothing states when this closes, so it stays as open ended as it is today")
                .isEqualTo(SENTINEL);
    }

    @Test
    @DisplayName("a stated half is kept, and only the missing half is invented")
    void onlyTheMissingHalfIsInvented() {
        assertThat(window("stated open, no close").opensOn())
                .as("the operator stated the opening day; it is not moved")
                .isEqualTo(days(-20));
        assertThat(openToday("stated open, no close")).isTrue();
        assertThat(openToday("stated open, no close, academic period already over"))
                .as("an academic period that ended before registration opened says nothing about when it closes")
                .isTrue();

        assertThat(window("stated close in the future, no open").closesOn())
                .as("the operator stated the closing day; it is not moved")
                .isEqualTo(days(90));
        assertThat(openToday("stated close in the future, no open")).isTrue();
    }

    @Test
    @DisplayName("a closing date somebody actually stated is honoured, not pushed out to the sentinel")
    void aStatedClosingDateIsHonoured() {
        Window closed = window("stated close in the past, no open");

        assertThat(closed.closesOn())
                .as("'registration closed on this day' is a sentence an operator wrote and the listing shows")
                .isEqualTo(days(-90));
        assertThat(closed.closesOn()).isNotEqualTo(SENTINEL);
        assertThat(closed.opensOn())
                .as("the invented half gives way instead, so the pair is in order")
                .isBeforeOrEqualTo(closed.closesOn());
        assertThat(openToday("stated close in the past, no open")).isFalse();
    }

    @Test
    @DisplayName("a window stated in full is left exactly as it was written")
    void fullyStatedWindowsAreUntouched() {
        assertThat(window("both stated, open today")).isEqualTo(new Window(days(-10), days(10)));
        assertThat(window("both stated, closed last month")).isEqualTo(new Window(days(-90), days(-30)));
        assertThat(window("both stated, opens next month")).isEqualTo(new Window(days(10), days(40)));
    }

    @Test
    @DisplayName("an inverted pair is repaired by pulling the opening date back, not by pushing the close out")
    void invertedPairsGiveWayOnTheOpeningDate() {
        Window repaired = window("inverted pair already stored");

        assertThat(repaired.closesOn()).isEqualTo(days(-20));
        assertThat(repaired.opensOn()).isBeforeOrEqualTo(repaired.closesOn());
    }

    @Test
    @DisplayName("both columns end up mandatory, under a constraint with no NULL branches")
    void theColumnsBecomeMandatory() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("""
                     SELECT column_name, is_nullable FROM information_schema.columns
                      WHERE table_name = 'class_definitions'
                        AND column_name LIKE 'registration_period%'""")) {
            int checked = 0;
            while (columns.next()) {
                assertThat(columns.getString("is_nullable")).isEqualTo("NO");
                checked++;
            }
            assertThat(checked).isEqualTo(2);
        }

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet constraint = statement.executeQuery("""
                     SELECT pg_get_constraintdef(oid) AS definition, convalidated FROM pg_constraint
                      WHERE conname = 'chk_class_definitions_registration_period_valid'""")) {
            assertThat(constraint.next()).isTrue();
            assertThat(constraint.getString("definition")).doesNotContain("IS NULL");
            assertThat(constraint.getBoolean("convalidated"))
                    .as("a constraint left NOT VALID would not be checked against the rows already stored")
                    .isTrue();
        }
    }

    // --- fixtures -----------------------------------------------------------------------------

    private record Window(LocalDate opensOn, LocalDate closesOn) { }

    private static LocalDate days(int offset) {
        return TODAY.plusDays(offset);
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                // Matches spring.flyway.out-of-order in application.yaml.
                .outOfOrder(true);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    /** A class row as production holds one, with only the four dates under test varying. */
    private static void seed(Connection connection, String title,
                             LocalDate academicStart, LocalDate academicEnd,
                             LocalDate registrationStart, LocalDate registrationEnd,
                             int createdDaysAgo) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO class_definitions
                    (title, default_instructor_uuid, created_by, default_start_time, default_end_time,
                     class_visibility, session_format,
                     academic_period_start_date, academic_period_end_date,
                     registration_period_start_date, registration_period_end_date, created_date)
                VALUES (?, gen_random_uuid(), 'migration-test', NOW() + INTERVAL '45 days',
                        NOW() + INTERVAL '45 days 2 hours', 'PUBLIC', 'GROUP', ?, ?, ?, ?,
                        NOW() - make_interval(days => ?))""")) {
            insert.setString(1, title);
            insert.setObject(2, academicStart);
            insert.setObject(3, academicEnd);
            insert.setObject(4, registrationStart);
            insert.setObject(5, registrationEnd);
            insert.setInt(6, createdDaysAgo);
            insert.executeUpdate();
        }
    }

    private static Window window(String title) {
        try (Connection connection = connect();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT registration_period_start_date, registration_period_end_date
                       FROM class_definitions WHERE title = ?""")) {
            select.setString(1, title);
            try (ResultSet row = select.executeQuery()) {
                assertThat(row.next()).as("seeded row '%s' is missing", title).isTrue();
                return new Window(
                        row.getObject(1, LocalDate.class),
                        row.getObject(2, LocalDate.class));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read back the class titled " + title, e);
        }
    }

    private static boolean openToday(String title) {
        Window window = window(title);
        return !TODAY.isBefore(window.opensOn()) && !TODAY.isAfter(window.closesOn());
    }
}
