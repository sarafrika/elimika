package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Class and assignment reminders are claimed once per start or due time")
class ReminderClaimIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;

    @Autowired
    private ClassAssignmentScheduleRepository assignmentScheduleRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void skipForeignKeys() {
        // Only the claim columns matter here; the rows' parents are irrelevant.
        jdbc.execute("SET session_replication_role = replica");
    }

    @Test
    @DisplayName("the first claim wins, repeats lose, and a rescheduled session is due again")
    void upcomingReminderIsClaimedOncePerStartTime() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO scheduled_instances (uuid, instructor_uuid, start_time, end_time, title, location_type,
                                                 max_participants, status, created_by)
                VALUES (?, ?, now() + interval '1 hour', now() + interval '2 hours', 'Class', 'ONLINE', 10,
                        'SCHEDULED', 'test')
                """, uuid, UUID.randomUUID());

        assertThat(scheduledInstanceRepository.claimUpcomingReminder(uuid)).isEqualTo(1);
        assertThat(scheduledInstanceRepository.claimUpcomingReminder(uuid)).isZero();

        jdbc.update("UPDATE scheduled_instances SET start_time = start_time + interval '1 day' WHERE uuid = ?", uuid);
        assertThat(scheduledInstanceRepository.claimUpcomingReminder(uuid)).isEqualTo(1);
    }

    @Test
    @DisplayName("one-day and due-now assignment reminders are claimed independently, once each")
    void assignmentRemindersAreClaimedOncePerDueTime() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO class_assignment_schedules (uuid, class_definition_uuid, lesson_uuid, assignment_uuid,
                                                        instructor_uuid, due_at, created_by)
                VALUES (?, ?, ?, ?, ?, now() + interval '12 hours', 'test')
                """, uuid, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(assignmentScheduleRepository.claimOneDayReminder(uuid)).isEqualTo(1);
        assertThat(assignmentScheduleRepository.claimOneDayReminder(uuid)).isZero();
        assertThat(assignmentScheduleRepository.claimDueReminder(uuid)).isEqualTo(1);
        assertThat(assignmentScheduleRepository.claimDueReminder(uuid)).isZero();

        jdbc.update("UPDATE class_assignment_schedules SET due_at = due_at + interval '2 days' WHERE uuid = ?", uuid);
        assertThat(assignmentScheduleRepository.claimOneDayReminder(uuid)).isEqualTo(1);
    }
}
