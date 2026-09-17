package apps.sarafrika.elimika.resourcing.integration;

import apps.sarafrika.elimika.resourcing.model.ResourceBooking;
import apps.sarafrika.elimika.resourcing.repository.ResourceBookingRepository;
import apps.sarafrika.elimika.resourcing.repository.projection.JobResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JobResourceBookingStatusQueryIntegrationTest.TestConfig.class)
@DisplayName("A job's resource booking states are grouped in one query")
class JobResourceBookingStatusQueryIntegrationTest {

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

    private static final LocalDateTime START = LocalDateTime.of(2031, 5, 6, 9, 0);

    @Autowired
    private ResourceBookingRepository bookingRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void skipForeignKeysForThisTransaction() {
        // Bookings reference resources, organisations and jobs; the grouping under test reads none of them.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();
    }

    @Test
    @DisplayName("each distinct job, resource and status comes back once, and other jobs stay out")
    void groupsDistinctStatesPerJobAndResource() {
        UUID job = UUID.randomUUID();
        UUID otherJob = UUID.randomUUID();
        UUID unrelatedJob = UUID.randomUUID();
        UUID venue = UUID.randomUUID();
        UUID pool = UUID.randomUUID();
        book(job, venue, ResourceBookingStatus.HOLD, 0);
        book(job, venue, ResourceBookingStatus.HOLD, 1);
        book(job, venue, ResourceBookingStatus.RELEASED, 2);
        book(job, pool, ResourceBookingStatus.CANCELLED, 0);
        book(otherJob, venue, ResourceBookingStatus.CONFIRMED, 3);
        book(unrelatedJob, venue, ResourceBookingStatus.HOLD, 4);

        List<JobResourceBookingStatus> rows = bookingRepository.findJobResourceStatuses(List.of(job, otherJob));

        assertThat(rows).containsExactlyInAnyOrder(
                new JobResourceBookingStatus(job, venue, ResourceBookingStatus.HOLD),
                new JobResourceBookingStatus(job, venue, ResourceBookingStatus.RELEASED),
                new JobResourceBookingStatus(job, pool, ResourceBookingStatus.CANCELLED),
                new JobResourceBookingStatus(otherJob, venue, ResourceBookingStatus.CONFIRMED));
    }

    private void book(UUID jobUuid, UUID resourceUuid, ResourceBookingStatus status, int weekOffset) {
        ResourceBooking booking = new ResourceBooking();
        booking.setResourceUuid(resourceUuid);
        booking.setOrganisationUuid(UUID.randomUUID());
        booking.setStatus(status);
        booking.setQuantity(1);
        booking.setStartTime(START.plusWeeks(weekOffset));
        booking.setEndTime(START.plusWeeks(weekOffset).plusHours(2));
        booking.setSourceType(ResourceBookingSourceType.MARKETPLACE_JOB);
        booking.setJobUuid(jobUuid);
        bookingRepository.saveAndFlush(booking);
    }
}
