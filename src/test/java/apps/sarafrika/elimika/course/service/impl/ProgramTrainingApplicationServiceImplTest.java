package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationExtrasResolver;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationHistory;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationOffers;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationRequirementAnswerRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationVenueRepository;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.repository.TrainingApplicationEventRepository;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramTrainingRateUpdateService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramTrainingApplicationServiceImplTest {

    @Mock private TrainingProgramRepository programRepository;
    @Mock private ProgramTrainingApplicationRepository applicationRepository;
    @Mock private CourseTrainingApplicationRepository courseApplicationRepository;
    @Mock private GenericSpecificationBuilder<ProgramTrainingApplication> specificationBuilder;
    @Mock private CurrencyService currencyService;
    @Mock private DomainSecurityService domainSecurityService;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private UserLookupService userLookupService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CourseSecuritySpi courseSecurity;
    @Mock private CourseRepository courseRepository;
    @Mock private ProgramCourseRepository programCourseRepository;
    @Mock private CourseTrainingRateUpdateRepository courseRateUpdates;
    @Mock private ProgramTrainingRateUpdateRepository programRateUpdates;
    @Mock private ProgramTrainingRateUpdateService rateUpdateService;
    @Mock private TrainingApplicationEventRepository eventRepository;
    @Mock private TrainingApplicationVenueRepository venueRepository;
    @Mock private TrainingApplicationRequirementAnswerRepository answerRepository;
    @Mock private CourseTrainingRequirementRepository requirementRepository;
    @Mock private ResourceLookupService resourceLookupService;
    @Mock private TrainingBranchLookupService branchLookupService;

    private ProgramTrainingApplicationServiceImpl service;
    private ProgramTrainingApplication application;
    private final UUID programUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        CourseFootingCap footingCap = new CourseFootingCap(new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache())));
        TrainingApplicationHistory history = new TrainingApplicationHistory(eventRepository, domainSecurityService, userLookupService);
        TrainingApplicationOffers offers = new TrainingApplicationOffers(venueRepository, answerRepository, requirementRepository,
                resourceLookupService, branchLookupService);
        service = new ProgramTrainingApplicationServiceImpl(
                programRepository, applicationRepository, courseApplicationRepository, specificationBuilder,
                currencyService, domainSecurityService, new CourseTrainingRateCardValidator(),
                courseCreatorLookupService, instructorLookupService, userLookupService, eventPublisher,
                new TrainingApplicationAccess(domainSecurityService, footingCap, courseSecurity),
                new TrainingFeeFloors(courseRepository, programCourseRepository),
                history,
                offers,
                new TrainingApplicationExtrasResolver(courseRateUpdates, programRateUpdates, history, offers),
                rateUpdateService);

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid))
                .thenReturn(List.of(programCourse(first), programCourse(second)));
        when(courseRepository.findByUuidIn(anyList())).thenReturn(List.of(course(first, "1500"), course(second, "3000")));

        application = new ProgramTrainingApplication();
        application.setUuid(UUID.randomUUID());
        application.setProgramUuid(programUuid);
        application.setApplicantType(CourseTrainingApplicantType.INSTRUCTOR);
        application.setApplicantUuid(UUID.randomUUID());
        application.setStatus(CourseTrainingApplicationStatus.APPROVED);
        application.setPrivateInpersonHourlyRate(new BigDecimal("2000"));
        application.setPrivateInpersonSessionRate(new BigDecimal("4000"));
        when(applicationRepository.findByUuid(application.getUuid())).thenReturn(Optional.of(application));
    }

    @Test
    @DisplayName("the program's owner sees cells flagged against the dearest course's minimum")
    void ownerSeesFloorFlags() {
        when(courseSecurity.isProgramOwner(programUuid)).thenReturn(true);

        ProgramTrainingApplicationDTO dto = service.getApplication(programUuid, application.getUuid());

        assertThat(dto.rateFloorFlags().minimumTrainingFee()).isEqualByComparingTo("3000");
        assertThat(dto.rateFloorFlags().privateInpersonHourlyRate()).isTrue();
        assertThat(dto.rateFloorFlags().privateInpersonSessionRate()).isFalse();
        org.mockito.Mockito.verify(eventRepository).insertFirstOpenIfAbsent(
                org.mockito.ArgumentMatchers.eq("PROGRAM"), org.mockito.ArgumentMatchers.eq(application.getUuid()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("the applicant sees no floor flags, and a stranger sees nothing at all")
    void othersSeeNoFlags() {
        when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(true);
        assertThat(service.getApplication(programUuid, application.getUuid()).rateFloorFlags()).isNull();
        assertThat(service.getApplicationHistory(programUuid, application.getUuid())).isEmpty();
        org.mockito.Mockito.verify(eventRepository, org.mockito.Mockito.never()).insertFirstOpenIfAbsent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        when(domainSecurityService.isInstructorWithUuid(application.getApplicantUuid())).thenReturn(false);
        assertThatThrownBy(() -> service.getApplication(programUuid, application.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getApplicationHistory(programUuid, application.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
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
