package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationUpdateRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationExtrasResolver;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationHistory;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.repository.TrainingApplicationEventRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingRateUpdateService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.TrainingApplicationEvent;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.enums.LocationType;

@ExtendWith(MockitoExtension.class)
class CourseTrainingApplicationServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTrainingApplicationRepository applicationRepository;

    @Mock
    private GenericSpecificationBuilder<CourseTrainingApplication> specificationBuilder;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private DomainSecurityService domainSecurityService;

    @Mock
    private CourseCreatorLookupService courseCreatorLookupService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private apps.sarafrika.elimika.tenancy.spi.UserLookupService userLookupService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private CourseSecuritySpi courseSecurity;

    @Mock
    private CourseTrainingRateUpdateRepository courseRateUpdates;

    @Mock
    private ProgramTrainingRateUpdateRepository programRateUpdates;

    @Mock
    private ProgramCourseRepository programCourseRepository;

    @Mock
    private TrainingApplicationEventRepository eventRepository;

    @Mock
    private CourseTrainingRateUpdateService rateUpdateService;

    private CourseTrainingRateCardValidator rateCardValidator;

    private CourseTrainingApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        rateCardValidator = new CourseTrainingRateCardValidator();
        // Built for real: outside a request there is no acting-domain header, so every footing is permitted.
        CourseFootingCap footingCap = new CourseFootingCap(new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache())));
        TrainingApplicationHistory history = new TrainingApplicationHistory(eventRepository, domainSecurityService, userLookupService);
        service = new CourseTrainingApplicationServiceImpl(
                courseRepository,
                applicationRepository,
                specificationBuilder,
                currencyService,
                domainSecurityService,
                footingCap,
                rateCardValidator,
                courseCreatorLookupService,
                instructorLookupService,
                userLookupService,
                applicationEventPublisher,
                new TrainingApplicationAccess(domainSecurityService, footingCap, courseSecurity),
                new TrainingApplicationExtrasResolver(courseRateUpdates, programRateUpdates, history),
                rateUpdateService,
                new TrainingFeeFloors(courseRepository, programCourseRepository),
                history
        );
    }

    @Test
    void submitApplicationRejectsRateBelowMinimum() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2500.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);
        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("KES", "2000.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("private_online_hourly_rate")
                .hasMessageContaining("minimum training fee");
    }

    @Test
    void submitApplicationRejectsOrganisationRateBelowMinimum() {
        UUID courseUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("3000.00"));

        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                rateCard("KES", "3200.00", "2500.00", "3600.00", "4100.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("private_inperson_hourly_rate")
                .hasMessageContaining("minimum training fee");
    }

    @Test
    void submitApplicationPersistsNormalisedRateAndCurrency() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2500.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(applicationRepository.findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid,
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid)).thenReturn(Optional.empty());

        lenient().when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);

        PlatformCurrency usd = new PlatformCurrency(
                "USD",
                840,
                "US Dollar",
                "$",
                2,
                true,
                false
        );
        when(currencyService.resolveCurrencyOrDefault("usd")).thenReturn(usd);

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("usd", "2800.1254"),
                "Ready to deliver evening cohorts"
        );

        service.submitApplication(courseUuid, request);

        ArgumentCaptor<CourseTrainingApplication> captor = ArgumentCaptor.forClass(CourseTrainingApplication.class);
        verify(applicationRepository).save(captor.capture());

        CourseTrainingApplication saved = captor.getValue();
        assertThat(saved.getPrivateOnlineHourlyRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getPrivateInpersonHourlyRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getGroupOnlineHourlyRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getGroupInpersonHourlyRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getRateCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(CourseTrainingApplicationStatus.PENDING);
    }

    @Test
    void submitApplicationRejectsInstructorImpersonation() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(false);
        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Instructors may only submit training applications for themselves");
    }

    @Test
    void submitApplicationRejectsOrganisationImpersonation() {
        UUID courseUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(false);

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Organisations may only submit training applications for organisations they manage");

        verify(courseRepository, org.mockito.Mockito.never()).findByUuid(courseUuid);
    }

    @Test
    void submitApplicationRejectsApprovedOrganisationApplication() {
        UUID courseUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2000.00"));

        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                CourseTrainingApplicationStatus.PENDING
        )).thenReturn(false);
        when(applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                CourseTrainingApplicationStatus.APPROVED
        )).thenReturn(true);

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already approved");

        verify(applicationRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class));
    }

    @Test
    void updateApplicationEditsPendingRateCard() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2000.00"));

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        existing.setApplicantUuid(applicantUuid);
        existing.setStatus(CourseTrainingApplicationStatus.PENDING);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(currencyService.resolveCurrencyOrDefault("KES")).thenReturn(defaultKes());
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseTrainingApplicationUpdateRequest request = new CourseTrainingApplicationUpdateRequest(
                rateCard("KES", "2500.00"),
                "Updated notes"
        );

        service.updateApplication(courseUuid, applicationUuid, request);

        ArgumentCaptor<CourseTrainingApplication> captor = ArgumentCaptor.forClass(CourseTrainingApplication.class);
        verify(applicationRepository).save(captor.capture());
        CourseTrainingApplication saved = captor.getValue();
        assertThat(saved.getPrivateOnlineHourlyRate()).isEqualByComparingTo("2500.00");
        assertThat(saved.getApplicationNotes()).isEqualTo("Updated notes");
        assertThat(saved.getStatus()).isEqualTo(CourseTrainingApplicationStatus.PENDING);
    }

    @Test
    void updateApplicationRejectsNonPending() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        existing.setApplicantUuid(applicantUuid);
        existing.setStatus(CourseTrainingApplicationStatus.APPROVED);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);

        CourseTrainingApplicationUpdateRequest request = new CourseTrainingApplicationUpdateRequest(
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.updateApplication(courseUuid, applicationUuid, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending applications can be updated");
    }

    @Test
    void updateApplicationRejectsForeignInstructor() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        existing.setApplicantUuid(applicantUuid);
        existing.setStatus(CourseTrainingApplicationStatus.PENDING);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(false);

        CourseTrainingApplicationUpdateRequest request = new CourseTrainingApplicationUpdateRequest(
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.updateApplication(courseUuid, applicationUuid, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void withdrawApplicationDeletesPending() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        existing.setApplicantUuid(applicantUuid);
        existing.setStatus(CourseTrainingApplicationStatus.PENDING);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);

        service.withdrawApplication(courseUuid, applicationUuid);

        verify(applicationRepository).delete(existing);
    }

    @Test
    void withdrawApplicationRejectsNonPending() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        existing.setApplicantUuid(applicantUuid);
        existing.setStatus(CourseTrainingApplicationStatus.REJECTED);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);

        assertThatThrownBy(() -> service.withdrawApplication(courseUuid, applicationUuid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending applications can be withdrawn");

        verify(applicationRepository, org.mockito.Mockito.never())
                .delete(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class));
    }

    @Test
    void withdrawApplicationAllowsOrganisationOwner() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.ORGANISATION);
        existing.setApplicantUuid(organisationUuid);
        existing.setStatus(CourseTrainingApplicationStatus.PENDING);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);

        service.withdrawApplication(courseUuid, applicationUuid);

        verify(applicationRepository).delete(existing);
    }

    @Test
    void withdrawApplicationRejectsForeignOrganisation() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        CourseTrainingApplication existing = new CourseTrainingApplication();
        existing.setCourseUuid(courseUuid);
        existing.setApplicantType(CourseTrainingApplicantType.ORGANISATION);
        existing.setApplicantUuid(organisationUuid);
        existing.setStatus(CourseTrainingApplicationStatus.PENDING);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(existing));
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.withdrawApplication(courseUuid, applicationUuid))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    @DisplayName("revoking an approval closes any rate update still awaiting review")
    void revokeClosesPendingRateUpdate() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();

        CourseTrainingApplication approved = new CourseTrainingApplication();
        approved.setUuid(applicationUuid);
        approved.setCourseUuid(courseUuid);
        approved.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        approved.setApplicantUuid(UUID.randomUUID());
        approved.setStatus(CourseTrainingApplicationStatus.APPROVED);

        when(applicationRepository.findByUuid(applicationUuid)).thenReturn(Optional.of(approved));
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.revokeApplication(courseUuid, applicationUuid,
                new apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest("No longer eligible"));

        verify(rateUpdateService).closePendingRateUpdate(
                org.mockito.ArgumentMatchers.eq(applicationUuid), org.mockito.ArgumentMatchers.contains("revoked"));
    }

    @Test
    @DisplayName("the course creator reads the full card with a flag on each cell priced below the minimum")
    void ownerReadsFloorFlags() {
        CourseTrainingApplication legacy = legacyApplication();
        when(courseSecurity.isCourseOwner(legacy.getCourseUuid())).thenReturn(true);

        apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO dto =
                service.getApplication(legacy.getCourseUuid(), legacy.getUuid());

        assertThat(dto.rateCard().groupOnlineHourlyRate()).isEqualByComparingTo("2500");
        assertThat(dto.rateFloorFlags()).isNotNull();
        assertThat(dto.rateFloorFlags().minimumTrainingFee()).isEqualByComparingTo("3000");
        assertThat(dto.rateFloorFlags().groupOnlineHourlyRate()).isTrue();
        assertThat(dto.rateFloorFlags().groupOnlineSessionRate()).isFalse();
        assertThat(dto.rateFloorFlags().groupOnlineDailyRate()).as("an unset cell is never flagged").isFalse();
        assertThat(dto.rateFloorFlags().privateOnlineHourlyRate()).isFalse();
    }

    @Test
    @DisplayName("the applicant reads their own card without floor flags")
    void applicantReadsNoFloorFlags() {
        CourseTrainingApplication legacy = legacyApplication();
        when(domainSecurityService.isInstructorWithUuid(legacy.getApplicantUuid())).thenReturn(true);

        apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO dto =
                service.getApplication(legacy.getCourseUuid(), legacy.getUuid());

        assertThat(dto.rateCard()).isNotNull();
        assertThat(dto.rateFloorFlags()).isNull();
    }

    @Test
    @DisplayName("searching, the owner gets floor flags and a non-party gets the redacted row")
    void searchFlagsForOwnerOnly() {
        CourseTrainingApplication legacy = legacyApplication();
        UUID callerUuid = UUID.randomUUID();
        UUID creatorUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(callerUuid);
        when(applicationRepository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<CourseTrainingApplication>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(legacy)));
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, 20);

        apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO stranger =
                service.search(java.util.Map.of(), page).getContent().getFirst();
        assertThat(stranger.rateCard()).isNull();
        assertThat(stranger.rateFloorFlags()).isNull();
        assertThat(stranger.pendingRateUpdateUuid()).isNull();

        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(callerUuid)).thenReturn(Optional.of(creatorUuid));
        when(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)).thenReturn(java.util.List.of(legacy.getCourseUuid()));
        apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO owner =
                service.search(java.util.Map.of(), page).getContent().getFirst();
        assertThat(owner.rateCard()).isNotNull();
        assertThat(owner.rateFloorFlags().groupOnlineHourlyRate()).isTrue();
    }

    @Test
    @DisplayName("resubmitting a rejected application records a fresh submitted event with its actor")
    void resubmissionIsRecorded() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();
        UUID actorUuid = UUID.randomUUID();
        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2000"));
        CourseTrainingApplication rejected = new CourseTrainingApplication();
        rejected.setUuid(UUID.randomUUID());
        rejected.setCourseUuid(courseUuid);
        rejected.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        rejected.setApplicantUuid(applicantUuid);
        rejected.setStatus(CourseTrainingApplicationStatus.REJECTED);

        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(actorUuid);
        when(userLookupService.getUserFullName(actorUuid)).thenReturn(Optional.of("Amina Otieno"));
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(currencyService.resolveCurrencyOrDefault("KES")).thenReturn(defaultKes());
        when(applicationRepository.findByCourseUuidAndApplicantTypeAndApplicantUuid(
                courseUuid, CourseTrainingApplicantType.INSTRUCTOR, applicantUuid)).thenReturn(Optional.of(rejected));
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submitApplication(courseUuid, new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR, applicantUuid, rateCard("KES", "2500.00"), "Trying again"));

        TrainingApplicationEvent event = recordedEvents().getFirst();
        assertThat(event.getEventType()).isEqualTo(TrainingApplicationEventType.SUBMITTED);
        assertThat(event.getApplicationType()).isEqualTo(TrainingApplicationType.COURSE);
        assertThat(event.getApplicationUuid()).isEqualTo(rejected.getUuid());
        assertThat(event.getActorUuid()).isEqualTo(actorUuid);
        assertThat(event.getActorName()).isEqualTo("Amina Otieno");
        assertThat(event.getNote()).isEqualTo("Trying again");
    }

    @Test
    @DisplayName("edits, decisions and withdrawals are each recorded")
    void everyTransitionIsRecorded() {
        CourseTrainingApplication application = legacyApplication();
        UUID courseUuid = application.getCourseUuid();
        when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(true);
        when(currencyService.resolveCurrencyOrDefault("KES")).thenReturn(defaultKes());
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest notes =
                new apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest("Reviewed");

        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        service.updateApplication(courseUuid, application.getUuid(),
                new CourseTrainingApplicationUpdateRequest(rateCard("KES", "3000.00"), "New notes"));
        service.approveApplication(courseUuid, application.getUuid(), notes);
        service.revokeApplication(courseUuid, application.getUuid(), notes);
        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        service.rejectApplication(courseUuid, application.getUuid(), notes);
        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        service.withdrawApplication(courseUuid, application.getUuid());

        assertThat(recordedEvents()).extracting(TrainingApplicationEvent::getEventType).containsExactly(
                TrainingApplicationEventType.EDITED,
                TrainingApplicationEventType.APPROVED,
                TrainingApplicationEventType.REVOKED,
                TrainingApplicationEventType.REJECTED,
                TrainingApplicationEventType.WITHDRAWN);
        assertThat(recordedEvents().get(1).getNote()).isEqualTo("Reviewed");
    }

    @Test
    @DisplayName("the creator's read records the first open; the applicant's read does not")
    void onlyTheCreatorsReadIsRecorded() {
        CourseTrainingApplication application = legacyApplication();
        when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(true);

        service.getApplication(application.getCourseUuid(), application.getUuid());
        verify(eventRepository, org.mockito.Mockito.never()).insertFirstOpenIfAbsent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        when(courseSecurity.isCourseOwner(application.getCourseUuid())).thenReturn(true);
        service.getApplication(application.getCourseUuid(), application.getUuid());
        verify(eventRepository).insertFirstOpenIfAbsent(
                org.mockito.ArgumentMatchers.eq("COURSE"), org.mockito.ArgumentMatchers.eq(application.getUuid()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("history is readable by the applicant and the creator, and hidden from everyone else")
    void historyIsLimitedToParties() {
        CourseTrainingApplication application = legacyApplication();
        assertThatThrownBy(() -> service.getApplicationHistory(application.getCourseUuid(), application.getUuid()))
                .isInstanceOf(apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException.class);

        when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(true);
        assertThat(service.getApplicationHistory(application.getCourseUuid(), application.getUuid())).isEmpty();
        lenient().when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(false);

        when(courseSecurity.isCourseOwner(application.getCourseUuid())).thenReturn(true);
        assertThat(service.getApplicationHistory(application.getCourseUuid(), application.getUuid())).isEmpty();
    }

    private java.util.List<TrainingApplicationEvent> recordedEvents() {
        ArgumentCaptor<TrainingApplicationEvent> events = ArgumentCaptor.forClass(TrainingApplicationEvent.class);
        verify(eventRepository, org.mockito.Mockito.atLeastOnce()).save(events.capture());
        return events.getAllValues();
    }

    private CourseTrainingApplication legacyApplication() {
        UUID courseUuid = UUID.randomUUID();
        Course course = new Course();
        course.setUuid(courseUuid);
        course.setMinimumTrainingFee(new BigDecimal("3000"));
        lenient().when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        lenient().when(courseRepository.findByUuidIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(java.util.List.of(course));

        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setUuid(UUID.randomUUID());
        application.setCourseUuid(courseUuid);
        application.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        application.setApplicantUuid(UUID.randomUUID());
        application.setStatus(CourseTrainingApplicationStatus.APPROVED);
        application.setRateCurrency("KES");
        application.setGroupOnlineHourlyRate(new BigDecimal("2500"));
        application.setGroupOnlineSessionRate(new BigDecimal("3500"));
        lenient().when(applicationRepository.findByUuid(application.getUuid())).thenReturn(Optional.of(application));
        return application;
    }

    // ── A rate card answers in the unit the job was contracted in ─────────────────────────────

    @Test
    @DisplayName("a card priced only per hour cannot answer a per-day job")
    void anUnpricedBasisReturnsNothingRatherThanAnHourlyFigure() {
        CourseTrainingRateCardDTO card = new CourseTrainingRateCardDTO(
                "KES",
                new BigDecimal("2000"), new BigDecimal("2000"), new BigDecimal("2000"), new BigDecimal("2000"),
                null, null, null, null,
                null, null, null, null);

        assertThat(card.resolveRate(SessionFormat.GROUP, LocationType.IN_PERSON,
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR))
                .isEqualByComparingTo("2000");
        // Deliberately null, not a derived number: a per-day rate inferred from an hourly one is a
        // price the instructor never agreed to.
        assertThat(card.resolveRate(SessionFormat.GROUP, LocationType.IN_PERSON,
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_DAY))
                .isNull();
    }

    @Test
    @DisplayName("each basis resolves to its own rate for the same delivery mode")
    void eachBasisResolvesIndependently() {
        CourseTrainingRateCardDTO card = new CourseTrainingRateCardDTO(
                "KES",
                new BigDecimal("2000"), new BigDecimal("2000"), new BigDecimal("2000"), new BigDecimal("2000"),
                new BigDecimal("3500"), new BigDecimal("3500"), new BigDecimal("3500"), new BigDecimal("3500"),
                new BigDecimal("9000"), new BigDecimal("9000"), new BigDecimal("9000"), new BigDecimal("9000"));

        assertThat(card.resolveRate(SessionFormat.GROUP, LocationType.ONLINE,
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR)).isEqualByComparingTo("2000");
        assertThat(card.resolveRate(SessionFormat.GROUP, LocationType.ONLINE,
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_SESSION)).isEqualByComparingTo("3500");
        assertThat(card.resolveRate(SessionFormat.GROUP, LocationType.ONLINE,
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_DAY)).isEqualByComparingTo("9000");
    }

    private CourseTrainingRateCardDTO rateCard(String currency, String amount) {
        BigDecimal normalized = new BigDecimal(amount);
        return new CourseTrainingRateCardDTO(
                currency,
                normalized, normalized, normalized, normalized,
                normalized, normalized, normalized, normalized,
                normalized, normalized, normalized, normalized
        );
    }

    private CourseTrainingRateCardDTO rateCard(String currency,
                                               String privateOnline,
                                               String privateInperson,
                                               String groupOnline,
                                               String groupInperson) {
        return new CourseTrainingRateCardDTO(
                currency,
                new BigDecimal(privateOnline),
                new BigDecimal(privateInperson),
                new BigDecimal(groupOnline),
                new BigDecimal(groupInperson),
                new BigDecimal(privateOnline),
                new BigDecimal(privateInperson),
                new BigDecimal(groupOnline),
                new BigDecimal(groupInperson),
                new BigDecimal(privateOnline),
                new BigDecimal(privateInperson),
                new BigDecimal(groupOnline),
                new BigDecimal(groupInperson)
        );
    }

    private PlatformCurrency defaultKes() {
        return new PlatformCurrency(
                "KES",
                404,
                "Kenyan Shilling",
                "KSh",
                2,
                true,
                false
        );
    }
}
