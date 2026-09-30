package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.internal.LearnerAffiliationLookupImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** A learner's affiliations from their class enrolments (one query) plus the memberships tenancy reports. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(LearnerAffiliationLookupImpl.class)
@DisplayName("Learner affiliation lookup")
class LearnerAffiliationLookupIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @MockitoBean private StudentLookupService studentLookupService;
    @MockitoBean private UserLookupService userLookupService;

    @Autowired private LearnerAffiliationLookupImpl lookup;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void relaxForeignKeys() {
        // Classes and enrolments reference users, courses and organisations the query never reads.
        jdbc.execute("SET LOCAL session_replication_role = replica");
        // Scheduling columns the query never reads; the DDL is rolled back with the test transaction.
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
    @DisplayName("organisations, instructors and courses of live class enrolments, plus memberships; "
            + "cancelled and waitlisted enrolments do not count")
    void affiliations() {
        UUID student = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID organisation = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID defaultInstructor = UUID.randomUUID();
        UUID sessionInstructor = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        UUID classUuid = classDefinition(organisation, defaultInstructor, course);
        enrol(student, instance(classUuid, sessionInstructor), "ENROLLED");

        UUID cancelledOrganisation = UUID.randomUUID();
        UUID cancelledClass = classDefinition(cancelledOrganisation, UUID.randomUUID(), UUID.randomUUID());
        enrol(student, instance(cancelledClass, UUID.randomUUID()), "CANCELLED");
        UUID waitlistedClass = classDefinition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        enrol(student, instance(waitlistedClass, UUID.randomUUID()), "WAITLISTED");

        when(studentLookupService.getStudentUserUuid(student)).thenReturn(Optional.of(user));
        when(userLookupService.getActiveUserOrganizations(user)).thenReturn(List.of(member));

        LearnerAffiliations affiliations = lookup.findAffiliations(student);

        assertThat(affiliations.organisationUuids()).containsExactlyInAnyOrder(organisation, member);
        assertThat(affiliations.instructorUuids()).containsExactlyInAnyOrder(defaultInstructor, sessionInstructor);
        assertThat(affiliations.courseUuids()).containsExactly(course);
        assertThat(lookup.findAffiliations(null).isEmpty()).isTrue();
    }

    private UUID classDefinition(UUID organisation, UUID instructor, UUID course) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, organisation_uuid, course_uuid, "
                        + "created_by) VALUES (?, 'Class', ?, ?, ?, 'test')",
                uuid, instructor, organisation, course);
        return uuid;
    }

    private UUID instance(UUID classUuid, UUID instructor) {
        UUID uuid = UUID.randomUUID();
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, start_time, end_time, "
                        + "title, location_type, max_participants, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, 'Session', 'ONLINE', 10, 'test')",
                uuid, classUuid, instructor, Timestamp.from(start), Timestamp.from(start.plus(1, ChronoUnit.HOURS)));
        return uuid;
    }

    private void enrol(UUID student, UUID instance, String status) {
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", UUID.randomUUID(), instance, student, status);
    }
}
