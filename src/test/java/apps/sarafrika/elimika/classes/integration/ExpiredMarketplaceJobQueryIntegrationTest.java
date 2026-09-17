package apps.sarafrika.elimika.classes.integration;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ExpiredMarketplaceJobQueryIntegrationTest.TestConfig.class)
@DisplayName("Which marketplace jobs the expiry sweep picks up")
class ExpiredMarketplaceJobQueryIntegrationTest {

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

    private static final LocalDateTime NOW = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);

    @Autowired
    private ClassMarketplaceJobRepository jobRepository;
    @Autowired
    private ClassMarketplaceJobSessionTemplateRepository templateRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void skipForeignKeysForThisTransaction() {
        // Jobs reference organisations and courses; the query under test reads neither.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();
    }

    @Test
    @DisplayName("an open job whose first session has started expires though registration is still open")
    void openJobPastItsStartExpires() {
        ClassMarketplaceJob started = job(ClassMarketplaceJobStatus.OPEN, NOW.minusHours(1), 10);

        assertThat(expiredUuids()).containsExactly(started.getUuid());
    }

    @Test
    @DisplayName("an open job whose earliest session template started expires even if its stated start is later")
    void openJobWithStartedTemplateExpires() {
        ClassMarketplaceJob job = job(ClassMarketplaceJobStatus.OPEN, NOW.plusDays(3), 10);
        template(job.getUuid(), NOW.plusDays(3));
        template(job.getUuid(), NOW.minusMinutes(30));

        assertThat(expiredUuids()).containsExactly(job.getUuid());
    }

    @Test
    @DisplayName("an open job not yet started and still inside its registration window is left alone")
    void openJobNotStartedStays() {
        ClassMarketplaceJob job = job(ClassMarketplaceJobStatus.OPEN, NOW.plusHours(2), 10);
        template(job.getUuid(), NOW.plusHours(2));

        assertThat(expiredUuids()).isEmpty();
    }

    @Test
    @DisplayName("an awaiting-class job is never expired by its start time")
    void awaitingClassJobPastItsStartStays() {
        ClassMarketplaceJob job = job(ClassMarketplaceJobStatus.AWAITING_CLASS, NOW.minusDays(1), 10);
        template(job.getUuid(), NOW.minusDays(1));

        assertThat(expiredUuids()).isEmpty();
    }

    @Test
    @DisplayName("the registration window rule still expires open and awaiting-class jobs")
    void lapsedRegistrationWindowStillExpires() {
        ClassMarketplaceJob open = job(ClassMarketplaceJobStatus.OPEN, NOW.plusDays(5), -1);
        ClassMarketplaceJob awaiting = job(ClassMarketplaceJobStatus.AWAITING_CLASS, NOW.plusDays(5), -1);
        job(ClassMarketplaceJobStatus.FILLED, NOW.minusDays(5), -1);

        assertThat(expiredUuids()).containsExactlyInAnyOrder(open.getUuid(), awaiting.getUuid());
    }

    private List<UUID> expiredUuids() {
        return jobRepository.findExpiredOpenJobs(NOW.toLocalDate(), NOW).stream()
                .map(ClassMarketplaceJob::getUuid)
                .toList();
    }

    private ClassMarketplaceJob job(ClassMarketplaceJobStatus status, LocalDateTime start, int registrationEndsInDays) {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setOrganisationUuid(UUID.randomUUID());
        job.setCourseUuid(UUID.randomUUID());
        job.setTitle("Job " + status);
        job.setStatus(status);
        job.setClassVisibility(ClassVisibility.PUBLIC);
        job.setSessionFormat(SessionFormat.GROUP);
        job.setDefaultStartTime(start);
        job.setDefaultEndTime(start.plusHours(2));
        job.setRegistrationPeriodStartDate(NOW.toLocalDate().minusDays(30));
        job.setRegistrationPeriodEndDate(NOW.toLocalDate().plusDays(registrationEndsInDays));
        job.setLocationType(LocationType.ONLINE);
        job.setMaxParticipants(20);
        job.setAllowWaitlist(true);
        job.setRateBasis(RateBasis.PER_HOUR);
        return jobRepository.saveAndFlush(job);
    }

    private void template(UUID jobUuid, LocalDateTime start) {
        ClassMarketplaceJobSessionTemplate template = new ClassMarketplaceJobSessionTemplate();
        template.setJobUuid(jobUuid);
        template.setStartTime(start);
        template.setEndTime(start.plusHours(2));
        template.setTimezone("UTC");
        template.setConflictResolution("FAIL");
        templateRepository.saveAndFlush(template);
    }
}
