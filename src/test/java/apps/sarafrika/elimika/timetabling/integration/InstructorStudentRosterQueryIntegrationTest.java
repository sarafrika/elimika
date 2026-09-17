package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.timetabling.dto.InstructorClassOptionDTO;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentDTO;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService.InstructorStudentRoster;
import apps.sarafrika.elimika.timetabling.service.impl.InstructorStudentRosterServiceImpl;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(InstructorStudentRosterQueryIntegrationTest.TestConfig.class)
@DisplayName("An instructor's students in an organisation's classes come back in one page query")
class InstructorStudentRosterQueryIntegrationTest {

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
    private static final UUID ORGANISATION = UUID.randomUUID();
    private static final UUID OTHER_ORGANISATION = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID OTHER_INSTRUCTOR = UUID.randomUUID();
    private static final UUID BRANCH = UUID.randomUUID();

    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private ClassSessionTemplateRepository templateRepository;
    @Autowired
    private EntityManager entityManager;

    private InstructorStudentRosterServiceImpl service;
    private UUID pianoClass;
    private UUID theoryClass;
    private UUID amina;
    private UUID brian;

    @BeforeEach
    void seed() {
        // Students, courses and classes reference users, creators and branches the query never reads.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();

        UUID course = course("Beginner Piano");
        pianoClass = classDefinition("Grade 5 Piano", ORGANISATION, INSTRUCTOR, course, BRANCH,
                SessionFormat.GROUP, LocationType.IN_PERSON);
        template(pianoClass, ClassRecurrenceType.WEEKLY, "WEDNESDAY,MONDAY", FIRST_SESSION);
        theoryClass = classDefinition("aural Theory", ORGANISATION, INSTRUCTOR, null, null,
                SessionFormat.INDIVIDUAL, LocationType.ONLINE);
        template(theoryClass, null, null, LocalDateTime.of(2031, 5, 10, 14, 30));
        UUID violinClass = classDefinition("Violin", ORGANISATION, OTHER_INSTRUCTOR, course, BRANCH,
                SessionFormat.GROUP, LocationType.IN_PERSON);
        UUID choirClass = classDefinition("Choir", OTHER_ORGANISATION, INSTRUCTOR, course, BRANCH,
                SessionFormat.GROUP, LocationType.IN_PERSON);

        UUID piano1 = instance(pianoClass, 0, SchedulingStatus.COMPLETED);
        UUID piano2 = instance(pianoClass, 1, SchedulingStatus.COMPLETED);
        UUID piano3 = instance(pianoClass, 2, SchedulingStatus.SCHEDULED);
        UUID theory1 = instance(theoryClass, 0, SchedulingStatus.SCHEDULED);

        amina = student("Amina Otieno");
        enrol(amina, piano1, EnrollmentStatus.ATTENDED);
        enrol(amina, piano2, EnrollmentStatus.ABSENT);
        enrol(amina, piano3, EnrollmentStatus.ENROLLED);

        brian = student("Brian Kamau");
        enrol(brian, piano1, EnrollmentStatus.ATTENDED);
        enrol(brian, piano2, EnrollmentStatus.ATTENDED);
        enrol(brian, theory1, EnrollmentStatus.ENROLLED);

        UUID cheruiyot = student("Cheruiyot Langat");
        enrol(cheruiyot, piano1, EnrollmentStatus.CANCELLED);
        enrol(cheruiyot, piano2, EnrollmentStatus.CANCELLED);

        enrol(student("Esther Violinist"), instance(violinClass, 0, SchedulingStatus.SCHEDULED), EnrollmentStatus.ENROLLED);
        enrol(student("Faith Chorister"), instance(choirClass, 0, SchedulingStatus.SCHEDULED), EnrollmentStatus.ENROLLED);

        DomainSecurityService security = mock(DomainSecurityService.class);
        when(security.managesOrganisation(ORGANISATION)).thenReturn(true);
        TrainingBranchLookupService branches = mock(TrainingBranchLookupService.class);
        when(branches.findBranchNames(anyCollection())).thenReturn(Map.of(BRANCH, "Main Campus"));
        service = new InstructorStudentRosterServiceImpl(enrollmentRepository, branches, security);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("one row per student per class, only for the organisation's classes the instructor teaches")
    void listsTheInstructorsStudentsInTheOrganisationsClassesOnly() {
        InstructorStudentRoster roster = service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 0, 20);

        assertThat(roster.students().getTotalElements()).isEqualTo(4);
        assertThat(roster.studentCount()).as("Brian is in two classes but counts once").isEqualTo(3);
        assertThat(roster.students().getContent())
                .extracting(InstructorStudentDTO::studentName, InstructorStudentDTO::classTitle)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Amina Otieno", "Grade 5 Piano"),
                        org.assertj.core.groups.Tuple.tuple("Brian Kamau", "aural Theory"),
                        org.assertj.core.groups.Tuple.tuple("Brian Kamau", "Grade 5 Piano"),
                        org.assertj.core.groups.Tuple.tuple("Cheruiyot Langat", "Grade 5 Piano"));
        assertThat(roster.classOptions()).containsExactly(
                new InstructorClassOptionDTO(theoryClass, "aural Theory"),
                new InstructorClassOptionDTO(pianoClass, "Grade 5 Piano"));
    }

    @Test
    @DisplayName("each row carries the class, its schedule, branch, attendance and standing")
    void rowsCarryClassScheduleAttendanceAndStanding() {
        List<InstructorStudentDTO> rows =
                service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 0, 20).students().getContent();

        InstructorStudentDTO aminaPiano = rows.get(0);
        assertThat(aminaPiano.studentUuid()).isEqualTo(amina);
        assertThat(aminaPiano.classDefinitionUuid()).isEqualTo(pianoClass);
        assertThat(aminaPiano.courseName()).isEqualTo("Beginner Piano");
        assertThat(aminaPiano.sessionFormat()).isEqualTo(SessionFormat.GROUP);
        assertThat(aminaPiano.locationType()).isEqualTo(LocationType.IN_PERSON);
        assertThat(aminaPiano.scheduleSummary()).isEqualTo("Mon & Wed · 9:00–11:00");
        assertThat(aminaPiano.branchUuid()).isEqualTo(BRANCH);
        assertThat(aminaPiano.branchName()).isEqualTo("Main Campus");
        assertThat(aminaPiano.enrolledAt()).isNotNull();
        assertThat(aminaPiano.attendanceRate()).isEqualTo(50.0);
        assertThat(aminaPiano.enrollmentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

        InstructorStudentDTO brianTheory = rows.get(1);
        assertThat(brianTheory.courseName()).isNull();
        assertThat(brianTheory.scheduleSummary()).isEqualTo("Sat 10 May · 14:30–16:30");
        assertThat(brianTheory.branchName()).isNull();
        assertThat(brianTheory.attendanceRate()).as("no attendance recorded yet").isNull();

        assertThat(rows.get(2).attendanceRate()).isEqualTo(100.0);
        assertThat(rows.get(3).enrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(rows.get(3).attendanceRate()).isNull();
    }

    @Test
    @DisplayName("search matches part of the name case-insensitively and takes wildcards literally")
    void searchMatchesPartOfTheName() {
        assertThat(service.listInstructorStudents(ORGANISATION, INSTRUCTOR, " kAMa ", null, 0, 20)
                .students().getContent())
                .extracting(InstructorStudentDTO::studentUuid)
                .containsExactly(brian, brian);
        assertThat(service.listInstructorStudents(ORGANISATION, INSTRUCTOR, "%", null, 0, 20)
                .students().getTotalElements()).isZero();
    }

    @Test
    @DisplayName("the class filter narrows the rows but not the class options")
    void classFilterNarrowsRowsNotOptions() {
        InstructorStudentRoster roster =
                service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, theoryClass, 0, 20);

        assertThat(roster.students().getContent())
                .extracting(InstructorStudentDTO::studentUuid, InstructorStudentDTO::classDefinitionUuid)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(brian, theoryClass));
        assertThat(roster.classOptions()).hasSize(2);
        assertThat(roster.studentCount()).as("filters narrow the rows, not the student count").isEqualTo(3);
    }

    @Test
    @DisplayName("pages split the rows in order and report the total")
    void pagesSplitTheRows() {
        InstructorStudentRoster second = service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 1, 2);

        assertThat(second.students().getTotalElements()).isEqualTo(4);
        assertThat(second.students().getTotalPages()).isEqualTo(2);
        assertThat(second.students().getContent())
                .extracting(InstructorStudentDTO::studentName, InstructorStudentDTO::classTitle)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Brian Kamau", "Grade 5 Piano"),
                        org.assertj.core.groups.Tuple.tuple("Cheruiyot Langat", "Grade 5 Piano"));
    }

    @Test
    @DisplayName("a page costs the same few statements however many students it holds")
    void aPageCostsAFixedNumberOfStatements() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        service.listInstructorStudents(ORGANISATION, INSTRUCTOR, null, null, 0, 2);

        assertThat(statistics.getPrepareStatementCount())
                .as("the page, its count, the class options and the distinct student count")
                .isEqualTo(4);
    }

    private UUID course(String name) {
        UUID uuid = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                        + "VALUES (?1, ?2, ?3, 'published', true, 'test')")
                .setParameter(1, uuid).setParameter(2, name).setParameter(3, UUID.randomUUID())
                .executeUpdate();
        return uuid;
    }

    private UUID student(String fullName) {
        UUID uuid = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO students (uuid, user_uuid, full_name, created_by) "
                        + "VALUES (?1, ?2, ?3, 'test')")
                .setParameter(1, uuid).setParameter(2, UUID.randomUUID()).setParameter(3, fullName)
                .executeUpdate();
        return uuid;
    }

    private UUID classDefinition(String title, UUID organisation, UUID instructor, UUID course, UUID branch,
                                 SessionFormat format, LocationType locationType) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle(title);
        definition.setOrganisationUuid(organisation);
        definition.setDefaultInstructorUuid(instructor);
        definition.setCourseUuid(course);
        definition.setBranchUuid(branch);
        definition.setLocationType(locationType);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setSessionFormat(format);
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

    private void template(UUID classUuid, ClassRecurrenceType recurrence, String days, LocalDateTime start) {
        ClassSessionTemplate template = new ClassSessionTemplate();
        template.setClassDefinitionUuid(classUuid);
        template.setTemplateOrder(0);
        template.setStartTime(start);
        template.setEndTime(start.plusHours(2));
        template.setTimezone("Africa/Nairobi");
        template.setRecurrenceType(recurrence);
        template.setIntervalValue(recurrence == null ? null : 1);
        template.setDaysOfWeek(days);
        template.setOccurrenceCount(recurrence == null ? null : 6);
        template.setConflictResolution(ConflictResolutionStrategy.FAIL);
        templateRepository.saveAndFlush(template);
    }

    private UUID instance(UUID classUuid, int week, SchedulingStatus status) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(classUuid);
        instance.setInstructorUuid(INSTRUCTOR);
        instance.setStartTime(FIRST_SESSION.plusWeeks(week));
        instance.setEndTime(FIRST_SESSION.plusWeeks(week).plusHours(2));
        instance.setTimezone("UTC");
        instance.setTitle("Session");
        instance.setLocationType("ONLINE");
        instance.setMaxParticipants(20);
        instance.setStatus(status);
        return scheduledInstanceRepository.saveAndFlush(instance).getUuid();
    }

    private void enrol(UUID studentUuid, UUID instanceUuid, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setScheduledInstanceUuid(instanceUuid);
        enrollment.setStatus(status);
        enrollmentRepository.saveAndFlush(enrollment);
    }
}
