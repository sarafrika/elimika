package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService.ClassScheduleSummary;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduleSlice;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.ClassScheduleServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ClassListingReadQueryIntegrationTest.TestConfig.class)
@DisplayName("Class listing reads fetch only the columns and rows they answer with")
class ClassListingReadQueryIntegrationTest {

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

    private static final LocalDateTime FIRST_SESSION = LocalDateTime.of(2031, 5, 5, 9, 0);

    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private EntityManager entityManager;

    private UUID pianoClass;
    private UUID emptyClass;
    private UUID amina;
    private UUID brian;
    private UUID aminaFirst;
    private UUID aminaThird;

    @BeforeEach
    void seed() {
        // Enrolments and classes reference students and organisations the queries never read.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();

        pianoClass = classDefinition("Grade 5 Piano");
        emptyClass = classDefinition("Empty");
        UUID otherClass = classDefinition("Violin");

        UUID first = instance(pianoClass, 0, 120, SchedulingStatus.COMPLETED);
        UUID second = instance(pianoClass, 0, 60, SchedulingStatus.CANCELLED);
        UUID third = instance(pianoClass, 1, 90, SchedulingStatus.SCHEDULED);
        UUID fourth = instance(pianoClass, 2, 30, SchedulingStatus.SCHEDULED);
        UUID violin = instance(otherClass, 0, 60, SchedulingStatus.SCHEDULED);

        amina = UUID.randomUUID();
        brian = UUID.randomUUID();
        aminaFirst = enrol(amina, first, EnrollmentStatus.ATTENDED);
        enrol(amina, second, EnrollmentStatus.ENROLLED);
        aminaThird = enrol(amina, third, EnrollmentStatus.ENROLLED);
        enrol(amina, fourth, EnrollmentStatus.CANCELLED);
        enrol(amina, violin, EnrollmentStatus.ENROLLED);
        enrol(brian, first, EnrollmentStatus.ATTENDED);
        enrol(brian, third, EnrollmentStatus.ENROLLED);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("a learner's own rows of a class match the roster filtered to them")
    void learnerRowsMatchTheFilteredRoster() {
        List<UUID> filteredRoster = enrollmentRepository.findByClassDefinitionUuid(pianoClass).stream()
                .filter(enrollment -> amina.equals(enrollment.getStudentUuid()))
                .map(Enrollment::getUuid)
                .toList();

        List<UUID> ownRows = enrollmentRepository.findByClassDefinitionUuidAndStudentUuid(pianoClass, amina).stream()
                .map(Enrollment::getUuid)
                .toList();

        assertThat(ownRows).containsExactlyInAnyOrder(aminaFirst, aminaThird);
        assertThat(ownRows).containsExactlyInAnyOrderElementsOf(filteredRoster);
        assertThat(enrollmentRepository.findByClassDefinitionUuidAndStudentUuid(emptyClass, amina)).isEmpty();
    }

    @Test
    @DisplayName("schedule slices carry the converted status, and summaries match the entity-based figures")
    void sliceSummariesMatchTheEntityFigures() {
        List<ScheduleSlice> slices = scheduledInstanceRepository
                .findScheduleSlicesByClassDefinitionUuidIn(List.of(pianoClass));

        assertThat(slices).hasSize(4).allSatisfy(slice -> assertThat(slice.classDefinitionUuid()).isEqualTo(pianoClass));
        assertThat(slices).extracting(ScheduleSlice::status).containsExactlyInAnyOrder(
                SchedulingStatus.COMPLETED, SchedulingStatus.CANCELLED, SchedulingStatus.SCHEDULED,
                SchedulingStatus.SCHEDULED);

        ClassScheduleServiceImpl service = new ClassScheduleServiceImpl(scheduledInstanceRepository);
        Map<UUID, ClassScheduleSummary> summaries = service.getScheduleSummaries(List.of(pianoClass, emptyClass));
        ClassScheduleSummary piano = summaries.get(pianoClass);

        assertThat(piano.scheduledInstances()).isEqualTo(3);
        assertThat(piano.completedSessions()).isEqualTo(1);
        assertThat(piano.scheduledMinutes()).isEqualTo(240);
        assertThat(piano.scheduledDays()).isEqualTo(3);
        assertThat(piano.classProgressPercentage()).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(service.getScheduleSummary(pianoClass)).isEqualTo(piano);
        assertThat(summaries.get(emptyClass).scheduledInstances()).isZero();
    }

    @Test
    @DisplayName("the batch existence check returns ids of known classes only")
    void existingUuidsSkipUnknownIds() {
        UUID unknown = UUID.randomUUID();

        assertThat(classDefinitionRepository.findExistingUuids(List.of(pianoClass, unknown, emptyClass)))
                .containsExactlyInAnyOrder(pianoClass, emptyClass);
    }

    private UUID classDefinition(String title) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle(title);
        definition.setOrganisationUuid(UUID.randomUUID());
        definition.setDefaultInstructorUuid(UUID.randomUUID());
        definition.setLocationType(LocationType.ONLINE);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setSessionFormat(SessionFormat.GROUP);
        definition.setMaxParticipants(20);
        definition.setAllowWaitlist(true);
        definition.setIsActive(true);
        definition.setRateBasis(RateBasis.PER_HOUR);
        definition.setSalePrice(new BigDecimal("1000.00"));
        definition.setDefaultStartTime(FIRST_SESSION);
        definition.setDefaultEndTime(FIRST_SESSION.plusHours(2));
        definition.setRegistrationPeriodStartDate(FIRST_SESSION.toLocalDate().minusDays(30));
        definition.setRegistrationPeriodEndDate(FIRST_SESSION.toLocalDate());
        return classDefinitionRepository.saveAndFlush(definition).getUuid();
    }

    private UUID instance(UUID classUuid, int week, int minutes, SchedulingStatus status) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(classUuid);
        instance.setInstructorUuid(UUID.randomUUID());
        instance.setStartTime(FIRST_SESSION.plusWeeks(week));
        instance.setEndTime(FIRST_SESSION.plusWeeks(week).plusMinutes(minutes));
        instance.setTimezone("UTC");
        instance.setTitle("Session");
        instance.setLocationType("ONLINE");
        instance.setMaxParticipants(20);
        instance.setStatus(status);
        return scheduledInstanceRepository.saveAndFlush(instance).getUuid();
    }

    private UUID enrol(UUID studentUuid, UUID instanceUuid, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setScheduledInstanceUuid(instanceUuid);
        enrollment.setStatus(status);
        return enrollmentRepository.saveAndFlush(enrollment).getUuid();
    }
}
