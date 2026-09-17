package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository;
import apps.sarafrika.elimika.classes.repository.ClassSchedulingConflictRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.classes.service.impl.ClassDefinitionServiceImpl;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionLookupServiceImpl;
import apps.sarafrika.elimika.commerce.spi.paywall.CommercePaywallService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.spi.payout.InstructorPayableLookupService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.InstructorTimeHoldRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.security.TimetableSecurityService;
import apps.sarafrika.elimika.timetabling.service.impl.InstructorTimeHoldServiceImpl;
import apps.sarafrika.elimika.timetabling.service.impl.TimetableServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A hired instructor's FIRM hold is busy time to every scheduler except the class created from that
 * same job; a TENTATIVE hold from a mere application never blocks. Real schedule, hold and class rows.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(InstructorFirmHoldConflictIntegrationTest.TestConfig.class)
@DisplayName("A hired job's hold is busy time for the instructor")
class InstructorFirmHoldConflictIntegrationTest {

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

    private static final LocalDateTime SESSION_START = LocalDate.of(2031, 5, 6).atTime(9, 0);
    private static final LocalDateTime SESSION_END = SESSION_START.plusHours(2);

    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private InstructorTimeHoldRepository holdRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ClassSchedulingConflictRepository classSchedulingConflictRepository;
    @Autowired
    private ClassSessionTemplateRepository classSessionTemplateRepository;
    @Autowired
    private ClassDefinitionResourceRepository classDefinitionResourceRepository;

    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID hiredJobUuid = UUID.randomUUID();

    private TimetableServiceImpl timetableService;
    private ClassDefinitionServiceImpl classDefinitionService;

    @BeforeEach
    void wireRealScheduleHoldsAndClasses() {
        // Holds and classes reference jobs and applications; nothing under test reads those tables.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();

        AvailabilityService availabilityService = mock(AvailabilityService.class);
        when(availabilityService.isInstructorAvailable(any(), any(), any())).thenReturn(true);

        timetableService = new TimetableServiceImpl(
                scheduledInstanceRepository,
                enrollmentRepository,
                mock(ApplicationEventPublisher.class),
                mock(GenericSpecificationBuilder.class),
                mock(GenericSpecificationBuilder.class),
                new ClassDefinitionLookupServiceImpl(classDefinitionRepository),
                mock(CourseInfoService.class),
                mock(LearnerProgressLookupService.class),
                mock(AgeVerificationService.class),
                mock(CommercePaywallService.class),
                availabilityService,
                mock(StudentLookupService.class),
                mock(UserLookupService.class),
                mock(OrganisationLookupService.class),
                mock(OrganisationAffiliationService.class),
                mock(InstructorLookupService.class),
                mock(ResourceBookingService.class),
                mock(DomainSecurityService.class),
                mock(TimetableSecurityService.class),
                new InstructorTimeHoldServiceImpl(holdRepository, mock(OrganisationLookupService.class)));

        @SuppressWarnings("unchecked")
        ObjectProvider<TimetableService> timetableProvider = mock(ObjectProvider.class);
        when(timetableProvider.getIfAvailable()).thenReturn(timetableService);
        classDefinitionService = new ClassDefinitionServiceImpl(
                classDefinitionRepository,
                mock(DomainSecurityService.class),
                new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache())),
                classSchedulingConflictRepository,
                classSessionTemplateRepository,
                classDefinitionResourceRepository,
                mock(ResourceBookingService.class),
                mock(ResourceLookupService.class),
                availabilityService,
                mock(ApplicationEventPublisher.class),
                mock(CourseInfoService.class),
                mock(CourseTrainingApprovalSpi.class),
                timetableProvider,
                mock(ObjectProvider.class),
                mock(InstructorPayableLookupService.class),
                mock(MediaStorageService.class),
                mock(MediaValidationService.class),
                new StorageProperties(),
                new BranchLocationResolver(mock(TrainingBranchLookupService.class)));
    }

    @Test
    @DisplayName("creating the class for the hired job succeeds over the hire's own firm holds")
    void creatingTheHiredJobsClassSucceeds() {
        hold(hiredJobUuid, InstructorTimeHoldStatus.FIRM, SESSION_START, SESSION_END);

        ClassDefinitionDTO created = classDefinitionService
                .createClassDefinition(classRequest().withResourceLinks(null, hiredJobUuid))
                .classDefinition();

        assertThat(created.marketplaceJobUuid()).isEqualTo(hiredJobUuid);
        assertThat(scheduledInstanceRepository.findByClassDefinitionUuid(created.uuid())).hasSize(1);
    }

    @Test
    @DisplayName("creating any other class over the hire's firm holds is refused as a scheduling conflict")
    void creatingAnotherClassOverTheHoldIsRefused() {
        hold(hiredJobUuid, InstructorTimeHoldStatus.FIRM, SESSION_START, SESSION_END);

        assertThatThrownBy(() -> classDefinitionService.createClassDefinition(classRequest()))
                .isInstanceOfSatisfying(SchedulingConflictException.class, conflict ->
                        assertThat(conflict.getConflicts().getFirst().reasons())
                                .containsExactly("Instructor has overlapping scheduled instances or a class job "
                                        + "they accepted holds this time"));
    }

    @Test
    @DisplayName("a firm hold for a job the instructor was hired for is a conflict for any other class")
    void firmHoldBlocksAnotherClass() {
        hold(hiredJobUuid, InstructorTimeHoldStatus.FIRM, SESSION_START.minusHours(1), SESSION_END);
        ScheduleRequestDTO request = requestFor(classFromJob(null));

        assertThat(timetableService.hasInstructorConflict(instructorUuid, request)).isTrue();
        assertThatThrownBy(() -> timetableService.scheduleClass(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accepted a class job that holds this time");
    }

    @Test
    @DisplayName("a tentative hold from an application never blocks")
    void tentativeHoldDoesNotBlock() {
        hold(UUID.randomUUID(), InstructorTimeHoldStatus.TENTATIVE, SESSION_START, SESSION_END);
        ScheduleRequestDTO request = requestFor(classFromJob(null));

        assertThat(timetableService.hasInstructorConflict(instructorUuid, request)).isFalse();
        assertThat(timetableService.scheduleClass(request).uuid()).isNotNull();
    }

    @Test
    @DisplayName("the class created from the hired job schedules straight over that job's own firm holds")
    void classFromTheHiredJobIgnoresItsOwnHolds() {
        hold(hiredJobUuid, InstructorTimeHoldStatus.FIRM, SESSION_START, SESSION_END);
        ScheduleRequestDTO request = requestFor(classFromJob(hiredJobUuid));

        assertThat(timetableService.hasInstructorConflict(instructorUuid, request)).isFalse();
        ScheduledInstanceDTO scheduled = timetableService.scheduleClass(request);
        assertThat(scheduled.startTime()).isEqualTo(SESSION_START);
    }

    @Test
    @DisplayName("a class from one job still clashes with the firm hold of a different hired job")
    void classFromOneJobClashesWithAnotherJobsHold() {
        hold(UUID.randomUUID(), InstructorTimeHoldStatus.FIRM, SESSION_START, SESSION_END);
        ScheduleRequestDTO request = requestFor(classFromJob(hiredJobUuid));

        assertThat(timetableService.hasInstructorConflict(instructorUuid, request)).isTrue();
    }

    @Test
    @DisplayName("a firm hold that only touches the session boundary is not a clash")
    void adjacentFirmHoldDoesNotBlock() {
        hold(UUID.randomUUID(), InstructorTimeHoldStatus.FIRM, SESSION_END, SESSION_END.plusHours(1));

        assertThat(timetableService.hasInstructorConflict(instructorUuid, requestFor(classFromJob(null)))).isFalse();
    }

    private ClassDefinitionDTO classRequest() {
        return new ClassDefinitionDTO(
                null, "Grade 5 Piano", "Hired job class", instructorUuid, null, null, null, null,
                null, null, null, ClassVisibility.PUBLIC, SessionFormat.GROUP, SESSION_START, SESSION_END,
                null, null, SESSION_START.toLocalDate().minusDays(30), SESSION_START.toLocalDate(),
                null, null, LocationType.ONLINE, null, null, null, "https://meet.example/piano", 20, true, true,
                List.of(new ClassSessionTemplateDTO(SESSION_START, SESSION_END, null, "UTC",
                        ConflictResolutionStrategy.FAIL)),
                null, null, null, null);
    }

    private ScheduleRequestDTO requestFor(UUID classDefinitionUuid) {
        return new ScheduleRequestDTO(classDefinitionUuid, instructorUuid, SESSION_START, SESSION_END, "UTC");
    }

    private UUID classFromJob(UUID marketplaceJobUuid) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle("Grade 5 Piano");
        definition.setDefaultInstructorUuid(instructorUuid);
        definition.setLocationType(LocationType.ONLINE);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setSessionFormat(SessionFormat.GROUP);
        definition.setMaxParticipants(20);
        definition.setAllowWaitlist(true);
        definition.setIsActive(true);
        definition.setRateBasis(RateBasis.PER_HOUR);
        definition.setSalePrice(new BigDecimal("1000.00"));
        definition.setDefaultStartTime(SESSION_START);
        definition.setDefaultEndTime(SESSION_END);
        definition.setRegistrationPeriodStartDate(SESSION_START.toLocalDate().minusDays(30));
        definition.setRegistrationPeriodEndDate(SESSION_START.toLocalDate());
        definition.setMarketplaceJobUuid(marketplaceJobUuid);
        return classDefinitionRepository.saveAndFlush(definition).getUuid();
    }

    private void hold(UUID jobUuid, InstructorTimeHoldStatus status, LocalDateTime start, LocalDateTime end) {
        InstructorTimeHold hold = new InstructorTimeHold();
        hold.setInstructorUuid(instructorUuid);
        hold.setJobUuid(jobUuid);
        hold.setApplicationUuid(UUID.randomUUID());
        hold.setTitle("Held job");
        hold.setStartTime(start);
        hold.setEndTime(end);
        hold.setTimezone("UTC");
        hold.setStatus(status);
        holdRepository.saveAndFlush(hold);
    }
}
