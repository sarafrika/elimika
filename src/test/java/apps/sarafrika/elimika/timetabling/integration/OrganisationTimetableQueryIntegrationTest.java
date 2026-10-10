package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.timetabling.dto.OrganisationTimetableEntryDTO;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.service.impl.OrganisationTimetableServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(OrganisationTimetableQueryIntegrationTest.TestConfig.class)
@DisplayName("An organisation's timetable for a date range comes back in one query")
class OrganisationTimetableQueryIntegrationTest {

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

    private static final LocalDateTime MONDAY_NINE = LocalDateTime.of(2031, 5, 5, 9, 0);
    private static final UUID ORGANISATION = UUID.randomUUID();
    private static final UUID OTHER_ORGANISATION = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID OTHER_INSTRUCTOR = UUID.randomUUID();

    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private EntityManager entityManager;

    private InstructorLookupService instructorLookupService;
    private OrganisationTimetableServiceImpl service;
    private UUID pianoClass;
    private UUID violinClass;
    private UUID pianoMonday;
    private UUID violinTuesday;
    private UUID pianoWednesdayLate;

    @BeforeEach
    void seed() {
        // Classes reference courses, creators and branches the query never reads.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();

        pianoClass = classDefinition("Grade 5 Piano", ORGANISATION);
        violinClass = classDefinition("Violin Ensemble", ORGANISATION);
        UUID choirClass = classDefinition("Choir", OTHER_ORGANISATION);

        pianoMonday = instance(pianoClass, INSTRUCTOR, MONDAY_NINE, SchedulingStatus.COMPLETED);
        violinTuesday = instance(violinClass, OTHER_INSTRUCTOR, MONDAY_NINE.plusDays(1), SchedulingStatus.SCHEDULED);
        // Starts late on the range's last day and runs past midnight: still on the timetable.
        pianoWednesdayLate = instance(pianoClass, INSTRUCTOR,
                MONDAY_NINE.plusDays(2).withHour(23), SchedulingStatus.SCHEDULED);
        instance(pianoClass, INSTRUCTOR, MONDAY_NINE.plusDays(1).withHour(14), SchedulingStatus.CANCELLED);
        instance(pianoClass, INSTRUCTOR, MONDAY_NINE.plusDays(7), SchedulingStatus.SCHEDULED);
        instance(choirClass, INSTRUCTOR, MONDAY_NINE.plusDays(1), SchedulingStatus.SCHEDULED);

        enrol(pianoMonday, EnrollmentStatus.ATTENDED);
        enrol(pianoMonday, EnrollmentStatus.ABSENT);
        enrol(pianoMonday, EnrollmentStatus.CANCELLED);
        enrol(pianoMonday, EnrollmentStatus.WAITLISTED);
        enrol(violinTuesday, EnrollmentStatus.ENROLLED);

        instructorLookupService = mock(InstructorLookupService.class);
        when(instructorLookupService.findInstructorDirectoryEntries(anyCollection())).thenReturn(Map.of(
                INSTRUCTOR, new InstructorDirectoryEntry(INSTRUCTOR, "Jane Wanjiku", "Nairobi", true)));
        service = new OrganisationTimetableServiceImpl(scheduledInstanceRepository, instructorLookupService);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("lists the organisation's non-cancelled sessions overlapping the range, by start time")
    void listsTheOrganisationsSessionsInRange() {
        List<OrganisationTimetableEntryDTO> timetable =
                service.getOrganisationTimetable(ORGANISATION, LocalDate.of(2031, 5, 5), LocalDate.of(2031, 5, 7));

        assertThat(timetable).extracting(OrganisationTimetableEntryDTO::uuid)
                .containsExactly(pianoMonday, violinTuesday, pianoWednesdayLate);
    }

    @Test
    @DisplayName("each session carries its class title, instructor name and active enrolment count")
    void sessionsCarryTitleInstructorAndEnrolledCount() {
        List<OrganisationTimetableEntryDTO> timetable =
                service.getOrganisationTimetable(ORGANISATION, LocalDate.of(2031, 5, 5), LocalDate.of(2031, 5, 7));

        OrganisationTimetableEntryDTO piano = timetable.get(0);
        assertThat(piano.classDefinitionUuid()).isEqualTo(pianoClass);
        assertThat(piano.classTitle()).as("the class's title, not the session's cached one").isEqualTo("Grade 5 Piano");
        assertThat(piano.instructorUuid()).isEqualTo(INSTRUCTOR);
        assertThat(piano.instructorName()).isEqualTo("Jane Wanjiku");
        assertThat(piano.startTime()).isEqualTo(MONDAY_NINE);
        assertThat(piano.endTime()).isEqualTo(MONDAY_NINE.plusHours(2));
        assertThat(piano.status()).isEqualTo(SchedulingStatus.COMPLETED);
        assertThat(piano.maxParticipants()).isEqualTo(20);
        assertThat(piano.enrolledCount()).as("cancelled and waitlisted enrolments excluded").isEqualTo(2);

        OrganisationTimetableEntryDTO violin = timetable.get(1);
        assertThat(violin.classTitle()).isEqualTo("Violin Ensemble");
        assertThat(violin.instructorName()).as("unresolved instructor").isNull();
        assertThat(violin.enrolledCount()).isEqualTo(1);
        assertThat(timetable.get(2).enrolledCount()).isZero();
    }

    @Test
    @DisplayName("a range costs one statement and one batched instructor lookup however many sessions it holds")
    void aRangeCostsOneStatement() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        service.getOrganisationTimetable(ORGANISATION, LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 31));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        verify(instructorLookupService, times(1)).findInstructorDirectoryEntries(anyCollection());
    }

    @Test
    @DisplayName("an empty range skips the instructor lookup")
    void emptyRangeSkipsInstructorLookup() {
        assertThat(service.getOrganisationTimetable(ORGANISATION, LocalDate.of(2031, 6, 1), LocalDate.of(2031, 6, 30)))
                .isEmpty();
        verify(instructorLookupService, times(0)).findInstructorDirectoryEntries(anyCollection());
    }

    @Test
    @DisplayName("an inverted or over-wide range is rejected")
    void rejectsBadRanges() {
        assertThatThrownBy(() -> service.getOrganisationTimetable(ORGANISATION,
                LocalDate.of(2031, 5, 7), LocalDate.of(2031, 5, 5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getOrganisationTimetable(ORGANISATION,
                LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
    }

    private UUID classDefinition(String title, UUID organisation) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle(title);
        definition.setOrganisationUuid(organisation);
        definition.setDefaultInstructorUuid(INSTRUCTOR);
        definition.setLocationType(LocationType.IN_PERSON);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setSessionFormat(SessionFormat.GROUP);
        definition.setMaxParticipants(20);
        definition.setAllowWaitlist(true);
        definition.setIsActive(true);
        definition.setRateBasis(RateBasis.PER_HOUR);
        definition.setSalePrice(new BigDecimal("1000.00"));
        definition.setDefaultStartTime(MONDAY_NINE);
        definition.setDefaultEndTime(MONDAY_NINE.plusHours(2));
        definition.setRegistrationPeriodStartDate(MONDAY_NINE.toLocalDate().minusDays(30));
        definition.setRegistrationPeriodEndDate(MONDAY_NINE.toLocalDate());
        return classDefinitionRepository.saveAndFlush(definition).getUuid();
    }

    private UUID instance(UUID classUuid, UUID instructor, LocalDateTime start, SchedulingStatus status) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(classUuid);
        instance.setInstructorUuid(instructor);
        instance.setStartTime(start);
        instance.setEndTime(start.plusHours(2));
        instance.setTimezone("UTC");
        instance.setTitle("Session");
        instance.setLocationType("IN_PERSON");
        instance.setMaxParticipants(20);
        instance.setStatus(status);
        return scheduledInstanceRepository.saveAndFlush(instance).getUuid();
    }

    private void enrol(UUID instanceUuid, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUuid(UUID.randomUUID());
        enrollment.setScheduledInstanceUuid(instanceUuid);
        enrollment.setStatus(status);
        enrollmentRepository.saveAndFlush(enrollment);
    }
}
