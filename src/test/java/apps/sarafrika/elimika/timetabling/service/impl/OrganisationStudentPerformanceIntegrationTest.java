package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.spi.OrganisationStudentPerformanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The org student page 500'd: the timestamptz {@code last_session} column was cast to {@code Timestamp}. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Organisation student performance")
class OrganisationStudentPerformanceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void relaxForeignKeys() {
        jdbc.execute("SET LOCAL session_replication_role = replica");
        jdbc.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                    FOR r IN SELECT table_name, column_name FROM information_schema.columns
                             WHERE table_schema = current_schema()
                               AND table_name IN ('class_definitions', 'scheduled_instances', 'class_enrollments')
                               AND is_nullable = 'NO' AND column_default IS NULL
                    LOOP
                        EXECUTE format('ALTER TABLE %I ALTER COLUMN %I DROP NOT NULL', r.table_name, r.column_name);
                    END LOOP;
                END $$
                """);
    }

    @Test
    @DisplayName("maps the real query rows, including the last session time, for the org's own classes only")
    void mapsRealRows() {
        UUID organisation = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        Instant earlier = Instant.parse("2026-10-01T08:00:00Z");
        Instant latest = Instant.parse("2026-10-08T08:00:00Z");
        UUID classUuid = classDefinition(organisation);
        enrol(student, instance(classUuid, earlier), "ATTENDED");
        enrol(student, instance(classUuid, latest), "ABSENT");
        enrol(student, instance(classDefinition(UUID.randomUUID()), latest), "ATTENDED");

        List<OrganisationStudentPerformanceDTO> performance = enrollmentRepository
                .findStudentPerformanceForOrganisation(organisation, student).stream()
                .map(TimetableServiceImpl::toStudentPerformance)
                .toList();

        assertThat(performance).hasSize(1);
        OrganisationStudentPerformanceDTO row = performance.getFirst();
        assertThat(row.classDefinitionUuid()).isEqualTo(classUuid);
        assertThat(row.totalSessions()).isEqualTo(2);
        assertThat(row.attended()).isEqualTo(1);
        assertThat(row.absent()).isEqualTo(1);
        assertThat(row.lastSessionAt()).isEqualTo(LocalDateTime.ofInstant(latest, ZoneOffset.UTC));
    }

    private UUID classDefinition(UUID organisation) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, organisation_uuid, created_by) VALUES (?, 'Class', ?, 'test')",
                uuid, organisation);
        return uuid;
    }

    private UUID instance(UUID classUuid, Instant start) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, start_time, end_time, title, "
                        + "location_type, max_participants, created_by) VALUES (?, ?, ?, ?, 'Session', 'ONLINE', 10, 'test')",
                uuid, classUuid, Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)));
        return uuid;
    }

    private void enrol(UUID student, UUID instance, String status) {
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", UUID.randomUUID(), instance, student, status);
    }
}
