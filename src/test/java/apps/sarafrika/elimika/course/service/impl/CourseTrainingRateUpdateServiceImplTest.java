package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDecisionRequest;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateRequest;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicantNames;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationHistory;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.repository.TrainingApplicationEventRepository;
import apps.sarafrika.elimika.course.internal.training.TrainingRateUpdateNotifier;
import apps.sarafrika.elimika.course.internal.training.TrainingSubmitters;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.CourseTrainingRateUpdate;
import apps.sarafrika.elimika.course.model.TrainingApplicationEvent;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseTrainingRateUpdateServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseTrainingApplicationRepository applicationRepository;
    @Mock private CourseTrainingRateUpdateRepository rateUpdateRepository;
    @Mock private ProgramCourseRepository programCourseRepository;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private OrganisationLookupService organisationLookupService;
    @Mock private UserLookupService userLookupService;
    @Mock private CurrencyService currencyService;
    @Mock private DomainSecurityService domainSecurityService;
    @Mock private CourseSecuritySpi courseSecurity;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TrainingApplicationEventRepository eventRepository;

    private CourseTrainingRateUpdateServiceImpl service;

    private final UUID courseUuid = UUID.randomUUID();
    private final UUID creatorUuid = UUID.randomUUID();
    private final UUID creatorUserUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID instructorUserUuid = UUID.randomUUID();
    private final UUID organisationUuid = UUID.randomUUID();
    private CourseTrainingApplication application;

    @BeforeEach
    void setUp() {
        CourseFootingCap footingCap = new CourseFootingCap(new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache())));
        TrainingApplicationAccess access = new TrainingApplicationAccess(domainSecurityService, footingCap, courseSecurity);
        service = new CourseTrainingRateUpdateServiceImpl(
                courseRepository,
                applicationRepository,
                rateUpdateRepository,
                courseCreatorLookupService,
                new CourseTrainingRateCardValidator(),
                currencyService,
                access,
                new TrainingFeeFloors(courseRepository, programCourseRepository),
                new TrainingApplicantNames(instructorLookupService, organisationLookupService),
                new TrainingRateUpdateNotifier(instructorLookupService, userLookupService,
                        new TrainingSubmitters(userLookupService), eventPublisher),
                new TrainingApplicationHistory(eventRepository, domainSecurityService, userLookupService));

        Course course = new Course();
        course.setUuid(courseUuid);
        course.setName("Welding");
        course.setCourseCreatorUuid(creatorUuid);
        course.setMinimumTrainingFee(new BigDecimal("2000"));
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(courseCreatorLookupService.getCourseCreatorUserUuid(creatorUuid)).thenReturn(Optional.of(creatorUserUuid));
        when(currencyService.resolveCurrencyOrDefault(any())).thenReturn(
                new PlatformCurrency("KES", 404, "Kenyan Shilling", "KSh", 2, true, false));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructorUserUuid));
        when(instructorLookupService.findInstructorDirectoryEntries(any())).thenReturn(
                Map.of(instructorUuid, new InstructorDirectoryEntry(instructorUuid, "Amina Otieno", "Kisumu", true)));
        when(userLookupService.getUserEmail(any())).thenReturn(Optional.of("person@test.local"));
        when(userLookupService.getUserFullName(any())).thenReturn(Optional.of("A Person"));
        when(rateUpdateRepository.saveAndFlush(any(CourseTrainingRateUpdate.class))).thenAnswer(invocation -> {
            CourseTrainingRateUpdate update = invocation.getArgument(0);
            update.setUuid(UUID.randomUUID());
            return update;
        });
        when(rateUpdateRepository.save(any(CourseTrainingRateUpdate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        application = approvedApplication(CourseTrainingApplicantType.INSTRUCTOR, instructorUuid);
    }

    // ===== proposing =====

    @Test
    @DisplayName("the instructor applicant proposes a full card and the course creator is told")
    void instructorProposesAnUpdate() {
        actAsInstructor();

        TrainingRateUpdateDTO dto = service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), "Costs rose"));

        ArgumentCaptor<CourseTrainingRateUpdate> saved = ArgumentCaptor.forClass(CourseTrainingRateUpdate.class);
        verify(rateUpdateRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TrainingRateUpdateStatus.PENDING);
        assertThat(saved.getValue().getGroupOnlineDailyRate()).isEqualByComparingTo("3000");
        assertThat(saved.getValue().getRateCurrency()).isEqualTo("KES");
        assertThat(saved.getValue().getApplicationUuid()).isEqualTo(application.getUuid());

        assertThat(dto.applicationType()).isEqualTo(TrainingApplicationType.COURSE);
        assertThat(dto.courseUuid()).isEqualTo(courseUuid);
        assertThat(dto.applicantName()).isEqualTo("Amina Otieno");
        assertThat(dto.currentRateCard().groupOnlineHourlyRate()).isEqualByComparingTo("2500");
        assertThat(dto.proposedRateCard().groupOnlineHourlyRate()).isEqualByComparingTo("3000");
        assertThat(dto.note()).isEqualTo("Costs rose");
        verify(applicationRepository, never()).save(any());

        NotificationRequestedEvent event = publishedEvents().getFirst();
        assertThat(event.notificationType()).isEqualTo("TRAINING_RATE_UPDATE_SUBMITTED");
        assertThat(event.recipientId()).isEqualTo(creatorUserUuid);
        assertThat(event.actionUrl()).isEqualTo("/dashboard/course-creator/training-applications?tab=rate-updates");
    }

    @Test
    @DisplayName("a manager of the applicant organisation may propose an update")
    void organisationManagerProposesAnUpdate() {
        application = approvedApplication(CourseTrainingApplicantType.ORGANISATION, organisationUuid);
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);

        service.submitRateUpdate(courseUuid, application.getUuid(), new TrainingRateUpdateRequest(groupOnlineCard("3000"), null));

        verify(rateUpdateRepository).saveAndFlush(any(CourseTrainingRateUpdate.class));
    }

    @Test
    @DisplayName("staff of another organisation are refused")
    void anotherOrganisationIsForbidden() {
        application = approvedApplication(CourseTrainingApplicantType.ORGANISATION, organisationUuid);
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(rateUpdateRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("the course creator cannot propose rates on someone else's behalf")
    void courseOwnerCannotPropose() {
        when(courseSecurity.isCourseOwner(courseUuid)).thenReturn(true);

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the applicant");
    }

    @Test
    @DisplayName("only one update may be pending at a time")
    void onePendingAtATime() {
        actAsInstructor();
        when(rateUpdateRepository.existsByApplicationUuidAndStatus(application.getUuid(), TrainingRateUpdateStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(rateUpdateRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a concurrent second proposal caught by the partial unique index is still a conflict")
    void theIndexBacksTheOnePendingRule() {
        actAsInstructor();
        when(rateUpdateRepository.saveAndFlush(any(CourseTrainingRateUpdate.class))).thenThrow(
                new DataIntegrityViolationException("duplicate key violates uq_course_training_rate_updates_one_pending"));

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("rates can only be updated on an approved application")
    void onlyApprovedApplications() {
        actAsInstructor();
        application.setStatus(CourseTrainingApplicationStatus.PENDING);

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("3000"), null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved training application");
    }

    @Test
    @DisplayName("the proposed card is validated exactly like an application's card")
    void proposalsReuseCardValidation() {
        actAsInstructor();
        CourseTrainingRateCardDTO partial = new CourseTrainingRateCardDTO("KES",
                null, null, new BigDecimal("3000"), null,
                null, null, null, null,
                null, null, new BigDecimal("3000"), null);

        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(partial, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("group_online_session_rate");
        assertThatThrownBy(() -> service.submitRateUpdate(courseUuid, application.getUuid(),
                new TrainingRateUpdateRequest(groupOnlineCard("1500"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum training fee");
        verify(rateUpdateRepository, never()).saveAndFlush(any());
    }

    // ===== deciding =====

    @Test
    @DisplayName("approval copies the proposed card onto the application, which stays approved")
    void approvalMergesTheCard() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));

        TrainingRateUpdateDTO dto = service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(),
                new TrainingRateUpdateDecisionRequest("Fair"));

        verify(applicationRepository).save(application);
        assertThat(application.getGroupOnlineHourlyRate()).isEqualByComparingTo("3200");
        assertThat(application.getGroupOnlineSessionRate()).isEqualByComparingTo("3200");
        assertThat(application.getStatus()).isEqualTo(CourseTrainingApplicationStatus.APPROVED);
        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.APPROVED);
        assertThat(update.getReviewNotes()).isEqualTo("Fair");
        assertThat(update.getReviewedAt()).isNotNull();
        assertThat(dto.currentRateCard().groupOnlineHourlyRate()).isEqualByComparingTo("3200");

        List<NotificationRequestedEvent> events = publishedEvents();
        assertThat(events).extracting(NotificationRequestedEvent::notificationType)
                .containsOnly("TRAINING_RATE_UPDATE_APPROVED");
        assertThat(events).extracting(NotificationRequestedEvent::recipientId).containsOnly(instructorUserUuid);
        assertThat(events.getFirst().actionUrl()).isEqualTo("/dashboard/instructor/rate-card");
        assertThat(events).anyMatch(event -> event.deliveryChannels().contains("email"));
    }

    @Test
    @DisplayName("approval re-validates against the minimum fee as it stands now")
    void approvalRevalidates() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("2100"));
        courseRepository.findByUuid(courseUuid).orElseThrow().setMinimumTrainingFee(new BigDecimal("2500"));

        assertThatThrownBy(() -> service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(applicationRepository, never()).save(any());
        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.PENDING);
    }

    @Test
    @DisplayName("rejection leaves the application's card untouched")
    void rejectionLeavesTheCard() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));

        service.rejectRateUpdate(courseUuid, application.getUuid(), update.getUuid(), new TrainingRateUpdateDecisionRequest("Too high"));

        verify(applicationRepository, never()).save(any());
        assertThat(application.getGroupOnlineHourlyRate()).isEqualByComparingTo("2500");
        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.REJECTED);
        assertThat(publishedEvents()).extracting(NotificationRequestedEvent::notificationType)
                .containsOnly("TRAINING_RATE_UPDATE_REJECTED");
    }

    @Test
    @DisplayName("the applicant cannot approve or reject their own update")
    void applicantCannotDecide() {
        actAsInstructor();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));

        assertThatThrownBy(() -> service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.rejectRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.PENDING);
    }

    @Test
    @DisplayName("a decided update cannot be decided again")
    void decidedUpdatesAreFinal() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));
        update.setStatus(TrainingRateUpdateStatus.REJECTED);

        assertThatThrownBy(() -> service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending rate updates can be approved");
    }

    @Test
    @DisplayName("an update from another application is not found")
    void updatesAreScopedToTheirApplication() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));
        update.setApplicationUuid(UUID.randomUUID());

        assertThatThrownBy(() -> service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an organisation's decision goes to whoever proposed the update, in the organisation inbox")
    void organisationDecisionNotification() {
        actAsOwner();
        application = approvedApplication(CourseTrainingApplicantType.ORGANISATION, organisationUuid);
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));
        update.setCreatedBy("keycloak-manager");
        UUID managerUserUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByKeycloakId("keycloak-manager")).thenReturn(Optional.of(managerUserUuid));

        service.approveRateUpdate(courseUuid, application.getUuid(), update.getUuid(), null);

        NotificationRequestedEvent inApp = publishedEvents().stream()
                .filter(event -> event.deliveryChannels().contains("in_app")).findFirst().orElseThrow();
        assertThat(inApp.recipientId()).isEqualTo(managerUserUuid);
        assertThat(inApp.recipientDomain()).isEqualTo("organisation_user");
        assertThat(inApp.actionUrl()).isEqualTo("/dashboard/organisation/approvals/" + application.getUuid());
    }

    // ===== withdrawing, reading and closing =====

    @Test
    @DisplayName("the applicant withdraws a pending update, but not a decided one")
    void withdrawOnlyPending() {
        actAsInstructor();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));

        service.withdrawRateUpdate(courseUuid, application.getUuid(), update.getUuid());
        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.WITHDRAWN);

        assertThatThrownBy(() -> service.withdrawRateUpdate(courseUuid, application.getUuid(), update.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending rate updates can be withdrawn");
    }

    @Test
    @DisplayName("the course creator cannot withdraw the applicant's update")
    void ownerCannotWithdraw() {
        actAsOwner();
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));

        assertThatThrownBy(() -> service.withdrawRateUpdate(courseUuid, application.getUuid(), update.getUuid()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("an application's updates are hidden from anyone but its two parties")
    void readsAreLimitedToParties() {
        assertThatThrownBy(() -> service.getRateUpdates(courseUuid, application.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getRateUpdatesForReview(courseUuid, Optional.empty(), null))
                .isInstanceOf(AccessDeniedException.class);

        actAsOwner();
        pendingUpdate(groupOnlineCard("3200"));
        when(rateUpdateRepository.findByApplicationUuidOrderByCreatedDateDesc(application.getUuid()))
                .thenReturn(List.of(new CourseTrainingRateUpdate()));
        assertThat(service.getRateUpdates(courseUuid, application.getUuid())).hasSize(1);
    }

    @Test
    @DisplayName("a pending update is closed when the application stops being approved")
    void pendingUpdatesCloseWithTheApproval() {
        CourseTrainingRateUpdate update = pendingUpdate(groupOnlineCard("3200"));
        when(rateUpdateRepository.findFirstByApplicationUuidAndStatus(application.getUuid(), TrainingRateUpdateStatus.PENDING))
                .thenReturn(Optional.of(update));

        service.closePendingRateUpdate(application.getUuid(), "Closed because the training approval was revoked.");

        assertThat(update.getStatus()).isEqualTo(TrainingRateUpdateStatus.REJECTED);
        assertThat(update.getReviewNotes()).contains("revoked");
    }

    @Test
    @DisplayName("every rate update step is written to the application's history")
    void rateUpdateStepsAreRecorded() {
        actAsInstructor();
        service.submitRateUpdate(courseUuid, application.getUuid(), new TrainingRateUpdateRequest(groupOnlineCard("3000"), "Costs rose"));
        CourseTrainingRateUpdate withdrawn = pendingUpdate(groupOnlineCard("3000"));
        service.withdrawRateUpdate(courseUuid, application.getUuid(), withdrawn.getUuid());

        actAsOwner();
        service.approveRateUpdate(courseUuid, application.getUuid(), pendingUpdate(groupOnlineCard("3000")).getUuid(),
                new TrainingRateUpdateDecisionRequest("Fair"));
        service.rejectRateUpdate(courseUuid, application.getUuid(), pendingUpdate(groupOnlineCard("3000")).getUuid(), null);
        CourseTrainingRateUpdate closed = pendingUpdate(groupOnlineCard("3000"));
        when(rateUpdateRepository.findFirstByApplicationUuidAndStatus(application.getUuid(), TrainingRateUpdateStatus.PENDING))
                .thenReturn(Optional.of(closed));
        service.closePendingRateUpdate(application.getUuid(), "Closed because the training approval was revoked.");

        ArgumentCaptor<TrainingApplicationEvent> events = ArgumentCaptor.forClass(TrainingApplicationEvent.class);
        verify(eventRepository, atLeastOnce()).save(events.capture());
        assertThat(events.getAllValues()).extracting(TrainingApplicationEvent::getEventType).containsExactly(
                TrainingApplicationEventType.RATES_UPDATE_SUBMITTED,
                TrainingApplicationEventType.RATES_UPDATE_WITHDRAWN,
                TrainingApplicationEventType.RATES_UPDATE_APPROVED,
                TrainingApplicationEventType.RATES_UPDATE_REJECTED,
                TrainingApplicationEventType.RATES_UPDATE_REJECTED);
        assertThat(events.getAllValues()).extracting(TrainingApplicationEvent::getApplicationUuid).containsOnly(application.getUuid());
        assertThat(events.getAllValues().get(0).getNote()).isEqualTo("Costs rose");
        assertThat(events.getAllValues().get(2).getNote()).isEqualTo("Fair");
        assertThat(events.getAllValues().get(4).getNote()).contains("revoked");
    }

    // ===== fixtures =====

    private void actAsInstructor() {
        when(domainSecurityService.isInstructorWithUuid(instructorUuid)).thenReturn(true);
    }

    private void actAsOwner() {
        when(courseSecurity.isCourseOwner(courseUuid)).thenReturn(true);
    }

    private CourseTrainingApplication approvedApplication(CourseTrainingApplicantType type, UUID applicantUuid) {
        CourseTrainingApplication approved = new CourseTrainingApplication();
        approved.setUuid(UUID.randomUUID());
        approved.setCourseUuid(courseUuid);
        approved.setApplicantType(type);
        approved.setApplicantUuid(applicantUuid);
        approved.setStatus(CourseTrainingApplicationStatus.APPROVED);
        approved.setRateCurrency("KES");
        approved.setGroupOnlineHourlyRate(new BigDecimal("2500"));
        approved.setGroupOnlineSessionRate(new BigDecimal("4000"));
        approved.setGroupOnlineDailyRate(new BigDecimal("9000"));
        when(applicationRepository.findByUuid(approved.getUuid())).thenReturn(Optional.of(approved));
        return approved;
    }

    private CourseTrainingRateUpdate pendingUpdate(CourseTrainingRateCardDTO card) {
        CourseTrainingRateUpdate update = new CourseTrainingRateUpdate();
        update.setUuid(UUID.randomUUID());
        update.setApplicationUuid(application.getUuid());
        update.setStatus(TrainingRateUpdateStatus.PENDING);
        apps.sarafrika.elimika.course.factory.TrainingRateCardFactory.apply(update, card, "KES");
        when(rateUpdateRepository.findByUuid(update.getUuid())).thenReturn(Optional.of(update));
        return update;
    }

    private static CourseTrainingRateCardDTO groupOnlineCard(String amount) {
        BigDecimal rate = new BigDecimal(amount);
        return new CourseTrainingRateCardDTO("KES",
                null, null, rate, null,
                null, null, rate, null,
                null, null, rate, null);
    }

    private List<NotificationRequestedEvent> publishedEvents() {
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(events.capture());
        return events.getAllValues().stream()
                .filter(NotificationRequestedEvent.class::isInstance)
                .map(NotificationRequestedEvent.class::cast)
                .toList();
    }
}
