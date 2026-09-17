package apps.sarafrika.elimika.classes.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Builds the schema up to the migration before the events table, seeds applications, then runs the backfill. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@DisplayName("The application events backfill gives every existing application its history")
class ClassMarketplaceJobApplicationEventsBackfillMigrationTest {

    private static final String SCHEMA_BEFORE_THE_EVENTS = "202609171338";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDateTime NOW = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    private static final UUID INSTRUCTOR_USER = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID MANAGER_USER = UUID.randomUUID();
    private static final UUID PENDING = UUID.randomUUID();
    private static final UUID INTERVIEWING = UUID.randomUUID();
    private static final UUID WITHDRAWN = UUID.randomUUID();
    private static final UUID NOT_SELECTED = UUID.randomUUID();

    @BeforeAll
    static void seedTheOldSchemaThenRunTheBackfill() throws SQLException {
        flyway().target(MigrationVersion.fromVersion(SCHEMA_BEFORE_THE_EVENTS)).load().migrate();

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            // The rows reference organisations, courses and branches the backfill never reads.
            statement.execute("SET session_replication_role = replica");
            user(connection, INSTRUCTOR_USER, "Amina", "Otieno", "amina@test.local");
            user(connection, MANAGER_USER, "Grace", "Wanjiru", "grace@test.local");
            statement.execute(String.format("""
                    INSERT INTO instructors (uuid, full_name, user_uuid, created_by)
                    VALUES ('%s', 'Amina Otieno', '%s', 'test')""", INSTRUCTOR, INSTRUCTOR_USER));

            application(connection, PENDING, "PENDING", "I can cover all six sessions", null, null, null, null, 10, null);
            application(connection, INTERVIEWING, "INTERVIEWING", "Taught two cohorts", "A 30-minute video call",
                    "grace@test.local", NOW.minusDays(2), NOW.plusDays(5), 9, null);
            // Older code stamped a withdrawal with the instructor uuid when the email did not resolve.
            application(connection, WITHDRAWN, "WITHDRAWN", "Keen", "Schedule changed", INSTRUCTOR.toString(),
                    NOW.minusDays(1), null, 8, null);
            application(connection, NOT_SELECTED, "NOT_SELECTED", "Keen", "Another instructor was selected", null,
                    null, null, 7, NOW.minusDays(3));
        }

        flyway().load().migrate();
    }

    @Test
    @DisplayName("every application was applied for by its instructor, when it was created, with its note")
    void everyApplicationGetsItsAppliedEvent() {
        for (UUID application : List.of(PENDING, INTERVIEWING, WITHDRAWN, NOT_SELECTED)) {
            Event applied = events(application).getLast();
            assertThat(applied.type()).isEqualTo("APPLIED");
            assertThat(applied.actorUuid()).isEqualTo(INSTRUCTOR_USER);
            assertThat(applied.actorName()).isEqualTo("Amina Otieno");
            assertThat(applied.jobUuid()).isEqualTo(jobOf(application));
        }
        assertThat(events(PENDING)).singleElement().satisfies(event -> {
            assertThat(event.note()).isEqualTo("I can cover all six sessions");
            assertThat(event.createdDate()).isEqualTo(NOW.minusDays(10));
        });
    }

    @Test
    @DisplayName("an application past pending also gets its current stage, by its reviewer, when it was reviewed")
    void theCurrentStageIsRecorded() {
        List<Event> interviewing = events(INTERVIEWING);
        assertThat(interviewing).extracting(Event::type).containsExactly("INTERVIEWING", "APPLIED");
        Event stage = interviewing.getFirst();
        assertThat(stage.actorUuid()).isEqualTo(MANAGER_USER);
        assertThat(stage.actorName()).isEqualTo("Grace Wanjiru");
        assertThat(stage.note()).isEqualTo("A 30-minute video call");
        assertThat(stage.interviewAt()).isEqualTo(NOW.plusDays(5));
        assertThat(stage.createdDate()).isEqualTo(NOW.minusDays(2));
    }

    @Test
    @DisplayName("a withdrawal is the instructor's own, whatever reviewed_by holds")
    void aWithdrawalIsTheInstructors() {
        Event withdrawn = events(WITHDRAWN).getFirst();
        assertThat(withdrawn.type()).isEqualTo("WITHDRAWN");
        assertThat(withdrawn.actorUuid()).isEqualTo(INSTRUCTOR_USER);
        assertThat(withdrawn.note()).isEqualTo("Schedule changed");
        assertThat(withdrawn.interviewAt()).isNull();
    }

    @Test
    @DisplayName("a stage with no review stamp falls back to the row's last update and has no actor")
    void aStageWithoutAReviewStampUsesTheLastUpdate() {
        Event notSelected = events(NOT_SELECTED).getFirst();
        assertThat(notSelected.type()).isEqualTo("NOT_SELECTED");
        assertThat(notSelected.createdDate()).isEqualTo(NOW.minusDays(3));
        assertThat(notSelected.actorUuid()).isNull();
        assertThat(notSelected.note()).isEqualTo("Another instructor was selected");
    }

    // --- fixtures -----------------------------------------------------------------------------

    private record Event(String type, UUID jobUuid, UUID actorUuid, String actorName, String note,
                         LocalDateTime interviewAt, LocalDateTime createdDate) {
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

    private static void user(Connection connection, UUID uuid, String firstName, String lastName, String email)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by)
                VALUES (?, ?, ?, ?, ?, 'test')""")) {
            insert.setObject(1, uuid);
            insert.setString(2, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000));
            insert.setString(3, firstName);
            insert.setString(4, lastName);
            insert.setString(5, email);
            insert.executeUpdate();
        }
    }

    /** An instructor applies to a job once, so each application gets a job of its own. */
    private static UUID jobOf(UUID applicationUuid) {
        return UUID.nameUUIDFromBytes(applicationUuid.toString().getBytes());
    }

    private static void application(Connection connection, UUID uuid, String status, String applicationNote,
                                    String reviewNotes, String reviewedBy, LocalDateTime reviewedAt,
                                    LocalDateTime interviewAt, int createdDaysAgo, LocalDateTime updatedDate)
            throws SQLException {
        try (PreparedStatement job = connection.prepareStatement("""
                INSERT INTO class_marketplace_jobs
                    (uuid, organisation_uuid, course_uuid, branch_uuid, title, status, class_visibility,
                     session_format, default_start_time, default_end_time, location_type, max_participants,
                     sale_price, instructor_pay, rate_basis, created_by)
                VALUES (?, gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 'Python for Data Analysis',
                        'OPEN', 'PUBLIC', 'GROUP', NOW() + INTERVAL '20 days', NOW() + INTERVAL '20 days 2 hours',
                        'ONLINE', 20, 5000, 4500, 'PER_SESSION', 'test')""")) {
            job.setObject(1, jobOf(uuid));
            job.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO class_marketplace_job_applications
                    (uuid, job_uuid, instructor_uuid, status, application_note, review_notes, reviewed_by,
                     reviewed_at, interview_at, created_date, updated_date, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'test')""")) {
            insert.setObject(1, uuid);
            insert.setObject(2, jobOf(uuid));
            insert.setObject(3, INSTRUCTOR);
            insert.setString(4, status);
            insert.setString(5, applicationNote);
            insert.setString(6, reviewNotes);
            insert.setString(7, reviewedBy);
            insert.setObject(8, reviewedAt);
            insert.setObject(9, interviewAt);
            insert.setObject(10, NOW.minusDays(createdDaysAgo));
            insert.setObject(11, updatedDate);
            insert.executeUpdate();
        }
    }

    /** The application's events, newest first as the endpoint orders them. */
    private static List<Event> events(UUID applicationUuid) {
        try (Connection connection = connect();
             PreparedStatement select = connection.prepareStatement("""
                     SELECT event_type, job_uuid, actor_uuid, actor_name, note, interview_at, created_date
                       FROM class_marketplace_job_application_events
                      WHERE application_uuid = ?
                      ORDER BY created_date DESC, id DESC""")) {
            select.setObject(1, applicationUuid);
            List<Event> events = new ArrayList<>();
            try (ResultSet row = select.executeQuery()) {
                while (row.next()) {
                    events.add(new Event(row.getString(1), row.getObject(2, UUID.class), row.getObject(3, UUID.class),
                            row.getString(4), row.getString(5), row.getObject(6, LocalDateTime.class),
                            row.getObject(7, LocalDateTime.class)));
                }
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the events of application " + applicationUuid, e);
        }
    }
}
