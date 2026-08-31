package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the boundary condition of the two queries that decide whether an instructor is double
 * booked.
 * <p>
 * Both are interval-overlap tests, and an interval here is half-open: a session running 09:00-11:00
 * occupies the instructor right up to 11:00 but not at 11:00 itself, so the 11:00-12:00 session that
 * follows it is adjacent, not overlapping. Written with {@code <=} / {@code >=} the comparison
 * treats the shared instant as time occupied twice and rejects back-to-back teaching — the class
 * that cannot be saved for a reason the user can see is false on the calendar in front of them.
 * <p>
 * The predicate lives in JPQL, so only a real database can answer what it actually matches.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(AdjacentSessionOverlapIntegrationTest.TestConfig.class)
@DisplayName("Adjacent sessions are not overlapping sessions")
class AdjacentSessionOverlapIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;

    private static final LocalDate DAY = LocalDate.of(2026, 3, 4);

    @Test
    @DisplayName("a session starting exactly when another ends does not count as an overlap")
    void adjacentSessionDoesNotOverlap() {
        UUID instructorUuid = UUID.randomUUID();
        persistSession(instructorUuid, DAY.atTime(9, 0), DAY.atTime(11, 0));

        List<ScheduledInstance> overlapping = scheduledInstanceRepository
                .findOverlappingInstancesForInstructor(instructorUuid, DAY.atTime(11, 0), DAY.atTime(12, 0));

        assertThat(overlapping)
                .as("11:00-12:00 begins the instant 09:00-11:00 ends, so nothing is double booked")
                .isEmpty();
    }

    @Test
    @DisplayName("a session that genuinely straddles another is still reported")
    void genuineOverlapIsStillDetected() {
        UUID instructorUuid = UUID.randomUUID();
        persistSession(instructorUuid, DAY.atTime(9, 0), DAY.atTime(11, 0));

        List<ScheduledInstance> overlapping = scheduledInstanceRepository
                .findOverlappingInstancesForInstructor(instructorUuid, DAY.atTime(10, 30), DAY.atTime(12, 0));

        assertThat(overlapping)
                .as("10:30 falls inside 09:00-11:00, so the instructor really is double booked")
                .hasSize(1);
    }

    @Test
    @DisplayName("an availability block butting up against a class does not shadow it")
    void adjacentAvailabilityBlockDoesNotMatch() {
        UUID instructorUuid = UUID.randomUUID();
        persistAvailability(instructorUuid, LocalTime.of(12, 0), LocalTime.of(13, 0));

        List<InstructorAvailability> overlapping = availabilityRepository
                .findOverlappingAvailability(instructorUuid, LocalTime.of(11, 0), LocalTime.of(12, 0), DAY);

        assertThat(overlapping)
                .as("a block that starts at 12:00 does not cover the class that ends at 12:00")
                .isEmpty();
    }

    private void persistSession(UUID instructorUuid, LocalDateTime start, LocalDateTime end) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(UUID.randomUUID());
        instance.setInstructorUuid(instructorUuid);
        instance.setStartTime(start);
        instance.setEndTime(end);
        instance.setTimezone("Africa/Nairobi");
        instance.setTitle("Existing session");
        instance.setLocationType("ONLINE");
        instance.setMaxParticipants(20);
        instance.setStatus(SchedulingStatus.SCHEDULED);
        scheduledInstanceRepository.saveAndFlush(instance);
    }

    private void persistAvailability(UUID instructorUuid, LocalTime start, LocalTime end) {
        InstructorAvailability availability = new InstructorAvailability();
        availability.setInstructorUuid(instructorUuid);
        availability.setAvailabilityType(AvailabilityType.DAILY);
        availability.setStartTime(start);
        availability.setEndTime(end);
        availability.setIsAvailable(false);
        availability.setEffectiveStartDate(DAY.minusDays(1));
        availability.setEffectiveEndDate(DAY.plusDays(1));
        availabilityRepository.saveAndFlush(availability);
    }
}
