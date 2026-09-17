package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateUpdateRequest;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicantNames;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationHistory;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.repository.TrainingApplicationEventRepository;
import apps.sarafrika.elimika.course.internal.training.TrainingRateUpdateNotifier;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.model.ProgramTrainingRateUpdate;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
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
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramTrainingRateUpdateServiceImplTest {

    @Mock private TrainingProgramRepository programRepository;
    @Mock private ProgramTrainingApplicationRepository applicationRepository;
    @Mock private ProgramTrainingRateUpdateRepository rateUpdateRepository;
    @Mock private CourseRepository courseRepository;
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

    private ProgramTrainingRateUpdateServiceImpl service;

    private final UUID programUuid = UUID.randomUUID();
    private final UUID programAuthorInstructorUuid = UUID.randomUUID();
    private final UUID programAuthorUserUuid = UUID.randomUUID();
    private final UUID organisationUuid = UUID.randomUUID();
    private ProgramTrainingApplication application;

    @BeforeEach
    void setUp() {
        CourseFootingCap footingCap = new CourseFootingCap(new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache())));
        service = new ProgramTrainingRateUpdateServiceImpl(
                programRepository,
                applicationRepository,
                rateUpdateRepository,
                courseCreatorLookupService,
                instructorLookupService,
                new CourseTrainingRateCardValidator(),
                currencyService,
                new TrainingApplicationAccess(domainSecurityService, footingCap, courseSecurity),
                new TrainingFeeFloors(courseRepository, programCourseRepository),
                new TrainingApplicantNames(instructorLookupService, organisationLookupService),
                new TrainingRateUpdateNotifier(instructorLookupService, userLookupService, eventPublisher),
                new TrainingApplicationHistory(eventRepository, domainSecurityService, userLookupService));

        TrainingProgram program = new TrainingProgram();
        program.setUuid(programUuid);
        program.setTitle("Fabrication");
        program.setCourseCreatorUuid(programAuthorInstructorUuid);
        when(programRepository.findByUuid(programUuid)).thenReturn(Optional.of(program));
        when(courseCreatorLookupService.getCourseCreatorUserUuid(programAuthorInstructorUuid)).thenReturn(Optional.empty());
        when(instructorLookupService.getInstructorUserUuid(programAuthorInstructorUuid)).thenReturn(Optional.of(programAuthorUserUuid));

        UUID cheapCourse = UUID.randomUUID();
        UUID dearCourse = UUID.randomUUID();
        when(programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid))
                .thenReturn(List.of(programCourse(cheapCourse), programCourse(dearCourse)));
        when(courseRepository.findByUuidIn(anyList())).thenReturn(List.of(course(cheapCourse, "1000"), course(dearCourse, "2800")));
        when(currencyService.resolveCurrencyOrDefault(any())).thenReturn(
                new PlatformCurrency("KES", 404, "Kenyan Shilling", "KSh", 2, true, false));
        when(organisationLookupService.findOrganisationNames(any())).thenReturn(Map.of(organisationUuid, "Westlands Institute"));
        when(rateUpdateRepository.saveAndFlush(any(ProgramTrainingRateUpdate.class))).thenAnswer(invocation -> {
            ProgramTrainingRateUpdate update = invocation.getArgument(0);
            update.setUuid(UUID.randomUUID());
            return update;
        });

        application = new ProgramTrainingApplication();
        application.setUuid(UUID.randomUUID());
        application.setProgramUuid(programUuid);
        application.setApplicantType(CourseTrainingApplicantType.ORGANISATION);
        application.setApplicantUuid(organisationUuid);
        application.setStatus(CourseTrainingApplicationStatus.APPROVED);
        application.setRateCurrency("KES");
        when(applicationRepository.findByUuid(application.getUuid())).thenReturn(Optional.of(application));
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);
    }

    @Test
    @DisplayName("a program's floor is the highest minimum fee across its courses")
    void programFloorIsTheDearestCourse() {
        assertThatThrownBy(() -> service.submitRateUpdate(programUuid, application.getUuid(),
                new TrainingRateUpdateRequest(card("2500"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum training fee");

        TrainingRateUpdateDTO dto = service.submitRateUpdate(programUuid, application.getUuid(),
                new TrainingRateUpdateRequest(card("2800"), null));
        assertThat(dto.applicationType()).isEqualTo(TrainingApplicationType.PROGRAM);
        assertThat(dto.programUuid()).isEqualTo(programUuid);
        assertThat(dto.courseUuid()).isNull();
        assertThat(dto.applicantName()).isEqualTo("Westlands Institute");
    }

    @Test
    @DisplayName("a program authored from the instructor dashboard notifies that instructor's account")
    void ownerNotificationFallsBackToTheInstructorProfile() {
        service.submitRateUpdate(programUuid, application.getUuid(), new TrainingRateUpdateRequest(card("3000"), null));

        ArgumentCaptor<NotificationRequestedEvent> event = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().recipientId()).isEqualTo(programAuthorUserUuid);
        assertThat(event.getValue().notificationType()).isEqualTo("TRAINING_RATE_UPDATE_SUBMITTED");
    }

    @Test
    @DisplayName("only the program's owner decides")
    void onlyTheProgramOwnerDecides() {
        ProgramTrainingRateUpdate update = new ProgramTrainingRateUpdate();
        update.setUuid(UUID.randomUUID());
        update.setApplicationUuid(application.getUuid());
        update.setStatus(TrainingRateUpdateStatus.PENDING);
        apps.sarafrika.elimika.course.factory.TrainingRateCardFactory.apply(update, card("3000"), "KES");
        when(rateUpdateRepository.findByUuid(update.getUuid())).thenReturn(Optional.of(update));
        when(rateUpdateRepository.save(any(ProgramTrainingRateUpdate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.approveRateUpdate(programUuid, application.getUuid(), update.getUuid(), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("program creator");

        when(courseSecurity.isProgramOwner(programUuid)).thenReturn(true);
        service.approveRateUpdate(programUuid, application.getUuid(), update.getUuid(), null);
        assertThat(application.getPrivateInpersonDailyRate()).isEqualByComparingTo("3000");
        verify(applicationRepository).save(application);
    }

    private static CourseTrainingRateCardDTO card(String amount) {
        BigDecimal rate = new BigDecimal(amount);
        return new CourseTrainingRateCardDTO("KES",
                null, rate, null, null,
                null, rate, null, null,
                null, rate, null, null);
    }

    private static ProgramCourse programCourse(UUID courseUuid) {
        ProgramCourse programCourse = new ProgramCourse();
        programCourse.setCourseUuid(courseUuid);
        return programCourse;
    }

    private static Course course(UUID uuid, String minimumFee) {
        Course course = new Course();
        course.setUuid(uuid);
        course.setMinimumTrainingFee(new BigDecimal(minimumFee));
        return course;
    }
}
