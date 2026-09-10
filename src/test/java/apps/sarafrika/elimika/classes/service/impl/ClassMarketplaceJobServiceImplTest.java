package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobResourceDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingRequest;
import apps.sarafrika.elimika.resourcing.spi.ResourceSummary;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldRequest;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassMarketplaceJobServiceImplTest {

    @Mock
    private ClassMarketplaceJobRepository jobRepository;

    @Mock
    private ClassMarketplaceJobApplicationRepository applicationRepository;

    @Mock
    private ClassMarketplaceJobSessionTemplateRepository sessionTemplateRepository;

    @Mock
    private apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobResourceRepository jobResourceRepository;

    @Mock
    private apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository classDefinitionResourceRepository;

    @Mock
    private apps.sarafrika.elimika.resourcing.spi.ResourceBookingService resourceBookingService;

    @Mock
    private apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService instructorTimeHoldService;

    @Mock
    private apps.sarafrika.elimika.resourcing.spi.ResourceLookupService resourceLookupService;

    @Mock
    private apps.sarafrika.elimika.availability.spi.AvailabilityService availabilityService;

    @Mock
    private org.springframework.beans.factory.ObjectProvider<apps.sarafrika.elimika.timetabling.spi.TimetableService> timetableServiceProvider;

    @Mock
    private apps.sarafrika.elimika.timetabling.spi.TimetableService timetableService;

    @Mock
    private CourseInfoService courseInfoService;

    @Mock
    private CourseTrainingApprovalSpi courseTrainingApprovalSpi;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService organisationAffiliationService;

    @Mock
    private apps.sarafrika.elimika.tenancy.spi.StudentGroupLookupService studentGroupLookupService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private DomainSecurityService domainSecurityService;

    @Mock
    private ClassDefinitionServiceInterface classDefinitionService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private apps.sarafrika.elimika.shared.storage.service.MediaStorageService mediaStorageService;

    @Mock
    private apps.sarafrika.elimika.shared.storage.service.MediaValidationService mediaValidationService;

    @Mock
    private apps.sarafrika.elimika.shared.storage.config.StorageProperties storageProperties;

    private final RequestScopedCache requestScopedCache = new RequestScopedCache();

    private ClassMarketplaceJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassMarketplaceJobServiceImpl(
                jobRepository,
                applicationRepository,
                sessionTemplateRepository,
                jobResourceRepository,
                classDefinitionResourceRepository,
                courseInfoService,
                courseTrainingApprovalSpi,
                userLookupService,
                organisationAffiliationService,
                studentGroupLookupService,
                instructorLookupService,
                domainSecurityService,
                requestScopedCache,
                classDefinitionService,
                resourceBookingService,
                instructorTimeHoldService,
                resourceLookupService,
                availabilityService,
                timetableServiceProvider,
                eventPublisher,
                mediaStorageService,
                mediaValidationService,
                storageProperties
        );
        org.mockito.Mockito.lenient()
                .when(courseTrainingApprovalSpi.resolveOrganisationRate(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("240.00")));
        org.mockito.Mockito.lenient()
                .when(courseTrainingApprovalSpi.resolveOrganisationProgramRate(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("240.00")));
        org.mockito.Mockito.lenient().when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        org.mockito.Mockito.lenient()
                .when(availabilityService.isInstructorAvailable(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
    }

    @Test
    void listInstructorApplicationsUsesInstructorRepositoryLookup() {
        UUID instructorUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        ClassMarketplaceJobApplication application = sampleApplication(UUID.randomUUID(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(UUID.randomUUID());
        when(domainSecurityService.isInstructorWithUuid(instructorUuid)).thenReturn(true);
        when(applicationRepository.findByInstructorUuidAndStatusOrderByCreatedDateDesc(
                instructorUuid,
                ClassMarketplaceJobApplicationStatus.PENDING,
                pageable
        )).thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        var result = service.listInstructorApplications(
                instructorUuid,
                ClassMarketplaceJobApplicationStatus.PENDING,
                pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().instructorUuid()).isEqualTo(instructorUuid);
        verify(applicationRepository).findByInstructorUuidAndStatusOrderByCreatedDateDesc(
                instructorUuid,
                ClassMarketplaceJobApplicationStatus.PENDING,
                pageable
        );
    }

    @Test
    void hiringAnApplicantRejectsInstructorWithoutCourseApproval() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());
        application.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid())).thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), application.getInstructorUuid())).thenReturn(false);

        assertThatThrownBy(() -> service.hireApplication(
                job.getUuid(),
                application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Needs course approval first", null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only instructors with approved course delivery access can be hired");
    }

    @Test
    void createJobAcceptsProgramTarget() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.courseUuid()).isNull();
        assertThat(result.programUuid()).isEqualTo(programUuid);

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getCourseUuid()).isNull();
        assertThat(jobCaptor.getValue().getProgramUuid()).isEqualTo(programUuid);
    }

    @Test
    void createJobPersistsScheduledEndTimes() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        LocalDateTime classStart = LocalDateTime.of(2026, 5, 2, 9, 0);
        LocalDateTime templateStart = LocalDateTime.of(2026, 5, 3, 10, 0);
        ClassMarketplaceJobRequestDTO request = withSchedule(
                sampleRequest(null, programUuid),
                classStart,
                classStart.plusMinutes(90),
                templateStart,
                templateStart.plusMinutes(90)
        );

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        service.createJob(request);

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getDefaultEndTime()).isEqualTo(classStart.plusMinutes(90));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClassMarketplaceJobSessionTemplate>> templateCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(sessionTemplateRepository).saveAll(templateCaptor.capture());
        assertThat(templateCaptor.getValue()).hasSize(1);
        assertThat(templateCaptor.getValue().getFirst().getEndTime()).isEqualTo(templateStart.plusMinutes(90));
        assertThat(templateCaptor.getValue().getFirst().getTimezone()).isEqualTo("Africa/Nairobi");
    }

    @Test
    void createJobPersistsTrainingFee() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.salePrice()).isEqualByComparingTo(new BigDecimal("240.00"));

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getSalePrice()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    void createJobPersistsServiceTargetAndReminderFields() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.serviceType()).isEqualTo(ClassServiceType.ONLINE);
        assertThat(result.preferredInstructorUuid()).isEqualTo(request.preferredInstructorUuid());
        assertThat(result.targetGroups()).containsExactly("Grade 1", "Grade 2");
        assertThat(result.remindStudents()).isTrue();
        assertThat(result.remindInstructor()).isFalse();
        assertThat(result.remindViaEmail()).isTrue();
        assertThat(result.remindViaSms()).isFalse();
        assertThat(result.remindViaPush()).isTrue();

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        ClassMarketplaceJob saved = jobCaptor.getValue();
        assertThat(saved.getServiceType()).isEqualTo(ClassServiceType.ONLINE);
        assertThat(saved.getPreferredInstructorUuid()).isEqualTo(request.preferredInstructorUuid());
        assertThat(saved.getTargetGroups()).containsExactly("Grade 1", "Grade 2");
    }

    @Test
    void createJobSnapshotsTargetGroupNamesFromOrganisationGroups() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID groupOne = UUID.randomUUID();
        UUID groupTwo = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request =
                withTargetGroupUuids(sampleRequest(null, programUuid), List.of(groupOne, groupTwo));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(studentGroupLookupService.filterGroupsInOrganisation(request.organisationUuid(), List.of(groupOne, groupTwo)))
                .thenReturn(List.of(groupOne, groupTwo));
        when(studentGroupLookupService.getGroupNames(List.of(groupOne, groupTwo)))
                .thenReturn(List.of("Grade 9 Stream A", "Grade 9 Stream B"));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.targetGroupUuids()).containsExactly(groupOne, groupTwo);
        assertThat(result.targetGroups()).containsExactly("Grade 9 Stream A", "Grade 9 Stream B");

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getTargetGroupUuids()).containsExactly(groupOne, groupTwo);
    }

    @Test
    void createJobRejectsTargetGroupsOwnedByAnotherOrganisation() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID foreignGroup = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request =
                withTargetGroupUuids(sampleRequest(null, programUuid), List.of(foreignGroup));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(studentGroupLookupService.filterGroupsInOrganisation(request.organisationUuid(), List.of(foreignGroup)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target groups");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    @Test
    void createJobPersistsTheCategoryAClassFallsUnder() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withCategory(sampleRequest(null, programUuid), categoryUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseInfoService.categoryExists(categoryUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.categoryUuid()).isEqualTo(categoryUuid);

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getCategoryUuid()).isEqualTo(categoryUuid);
    }

    @Test
    void createJobRejectsAnUnknownCategory() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withCategory(sampleRequest(null, programUuid), categoryUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseInfoService.categoryExists(categoryUuid)).thenReturn(false);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    @Test
    void createJobTakesTheTrainingFeeFromTheApprovedRateNotTheRequest() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withTrainingFee(sampleRequest(null, programUuid), null);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(courseTrainingApprovalSpi.resolveOrganisationProgramRate(
                eq(programUuid), eq(request.organisationUuid()), any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("512.00")));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        var result = service.createJob(request);

        assertThat(result.salePrice()).isEqualByComparingTo(new BigDecimal("512.00"));

        ArgumentCaptor<ClassMarketplaceJob> jobCaptor = ArgumentCaptor.forClass(ClassMarketplaceJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getSalePrice()).isEqualByComparingTo(new BigDecimal("512.00"));
    }

    @Test
    void createJobRejectsInstructorPayAboveTheSalePrice() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withPricing(
                sampleRequest(null, programUuid), new BigDecimal("500.00"), new BigDecimal("900.00"));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(courseTrainingApprovalSpi.resolveOrganisationProgramRate(
                eq(programUuid), eq(request.organisationUuid()), any(), any(), any()))
                .thenReturn(Optional.of(new BigDecimal("512.00")));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed the sale price");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    @Test
    void createJobRejectsWhenTheCourseCreatorApprovedNoRate() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(courseTrainingApprovalSpi.resolveOrganisationProgramRate(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No approved training rate");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    @Test
    void createJobWithPreferredInstructorAssignsDirectly() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request =
                withPreferredInstructor(sampleRequest(null, programUuid), instructorUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());
        // A direct hire now affiliates like every other hire path, so the instructor needs a user.
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    if (job.getUuid() == null) {
                        job.setUuid(UUID.randomUUID());
                    }
                    return job;
                });
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(
                        createdClassDefinition(classDefinitionUuid, instructorUuid, sampleJob())));

        var result = service.createJob(request);

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        assertThat(result.assignedInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(result.assignedClassDefinitionUuid()).isEqualTo(classDefinitionUuid);

        ArgumentCaptor<ClassDefinitionDTO> classCaptor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(classCaptor.capture());
        assertThat(classCaptor.getValue().defaultInstructorUuid()).isEqualTo(instructorUuid);
    }

    @Test
    void createJobRejectsMissingLearningContext() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, null);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of course_uuid or program_uuid is required");
    }

    @Test
    void createJobRejectsMultipleLearningContexts() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(UUID.randomUUID(), UUID.randomUUID());

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one of course_uuid or program_uuid is required");
    }

    @Test
    void hiringAnApplicantRejectsInstructorWithoutProgramApproval() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleProgramJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());
        application.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid())).thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApprovedForProgram(job.getProgramUuid(), application.getInstructorUuid())).thenReturn(false);

        assertThatThrownBy(() -> service.hireApplication(
                job.getUuid(),
                application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Needs program approval first", null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only instructors with approved training program delivery access can be hired");
    }

    @Test
    void hiringLeavesTheJobReadyAndCreatingTheClassAssignsAndFillsIt() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication hiredApplication = sampleApplication(job.getUuid(), instructorUuid);
        hiredApplication.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        ClassMarketplaceJobApplication otherApplication = sampleApplication(job.getUuid(), UUID.randomUUID());
        otherApplication.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);

        ClassMarketplaceJobSessionTemplate sessionTemplate = sampleSessionTemplate(job.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hiredApplication.getUuid()))
                .thenReturn(Optional.of(hiredApplication));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of(sessionTemplate));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any()))
                .thenReturn(List.of(otherApplication));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(UUID.randomUUID()));

        LocalDateTime beforeHire = LocalDateTime.now(ZoneOffset.UTC);
        var hired = service.hireApplication(job.getUuid(), hiredApplication.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Best of the shortlist", null));
        LocalDateTime afterHire = LocalDateTime.now(ZoneOffset.UTC);

        // The hire is the last decision: nothing is assigned yet, but the job waits only for its class.
        assertThat(hired.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.AWAITING_CLASS);
        assertThat(job.getAssignedApplicationUuid()).isEqualTo(hiredApplication.getUuid());
        assertThat(job.getAssignedInstructorUuid()).isNull();
        assertThat(hiredApplication.getReviewedAt()).isBetween(beforeHire, afterHire);

        ClassDefinitionDTO created = service.createClassForJob(job.getUuid());

        assertThat(created.uuid()).isEqualTo(classDefinitionUuid);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        assertThat(job.getAssignedInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(job.getAssignedClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(hiredApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        assertThat(otherApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);

        ArgumentCaptor<ClassDefinitionDTO> classCaptor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(classCaptor.capture());
        ClassDefinitionDTO forwarded = classCaptor.getValue();
        assertThat(forwarded.defaultInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(forwarded.organisationUuid()).isEqualTo(job.getOrganisationUuid());
        assertThat(forwarded.courseUuid()).isEqualTo(job.getCourseUuid());
        assertThat(forwarded.programUuid()).isNull();
        assertThat(forwarded.salePrice()).isNull();
        assertThat(forwarded.sessionTemplates()).hasSize(1);
    }

    @Test
    void rejectApplicationNotifiesUnsuccessfulInstructor() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID recipientUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid))
                .thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(recipientUserUuid));
        when(userLookupService.getUserEmail(recipientUserUuid))
                .thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.getUserFullName(recipientUserUuid))
                .thenReturn(Optional.of("Jane Instructor"));

        service.rejectApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Not a fit this time", null));

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.REJECTED);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_REJECTED".equals(e.notificationType())
                        && e.deliveryChannels().contains("in_app"));
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_REJECTED".equals(e.notificationType())
                        && e.deliveryChannels().contains("email"));
    }

    @Test
    void hiringAnApplicantNotifiesTheInstructor() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID recipientUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(recipientUserUuid));
        when(userLookupService.getUserEmail(recipientUserUuid)).thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.getUserFullName(recipientUserUuid)).thenReturn(Optional.of("Jane Instructor"));

        service.hireApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Looks great", null));

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_HIRED".equals(e.notificationType()));
    }

    @Test
    void moveApplicationToStageNotifiesTheInstructorOfTheNewStage() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID recipientUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(recipientUserUuid));
        when(userLookupService.getUserEmail(recipientUserUuid)).thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.getUserFullName(recipientUserUuid)).thenReturn(Optional.of("Jane Instructor"));

        service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.SHORTLISTED, null);

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_SHORTLISTED".equals(e.notificationType()));
    }

    @Test
    void moveApplicationToInterviewRequiresAndNotifiesInterviewDate() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID recipientUserUuid = UUID.randomUUID();
        LocalDateTime interviewAt = LocalDateTime.of(2026, 9, 1, 9, 30);

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(recipientUserUuid));
        when(userLookupService.getUserEmail(recipientUserUuid)).thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.getUserFullName(recipientUserUuid)).thenReturn(Optional.of("Jane Instructor"));

        assertThatThrownBy(() -> service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.INTERVIEWING, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interview_at is required");

        service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                new ClassMarketplaceJobDecisionRequestDTO("Please prepare a demo lesson.", interviewAt));

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.INTERVIEWING);
        assertThat(application.getInterviewAt()).isEqualTo(interviewAt);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_INTERVIEWING".equals(e.notificationType())
                        && e.body() != null
                        && e.body().contains("Interview scheduled")
                        && String.valueOf(e.templateVariables().get("interview_at")).contains("2026"));
    }

    @Test
    void creatingTheClassAssignsTheHiredInstructorAndNotifiesThem() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID recipientUserUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication hiredApplication = sampleApplication(job.getUuid(), instructorUuid);
        hiredApplication.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(hiredApplication.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hiredApplication.getUuid()))
                .thenReturn(Optional.of(hiredApplication));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(recipientUserUuid));
        when(userLookupService.getUserEmail(recipientUserUuid)).thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.getUserFullName(recipientUserUuid)).thenReturn(Optional.of("Jane Instructor"));

        service.createClassForJob(job.getUuid());

        assertThat(hiredApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        assertThat(job.getAssignedInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_ASSIGNED".equals(e.notificationType()));
    }

    @Test
    void withdrawApplicationClosesItAndNotifiesTheOrganisation() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID creatorUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        job.setCreatedBy("manager@org.test");
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("instructor@example.com"));
        when(userLookupService.findUserUuidByEmail("manager@org.test")).thenReturn(Optional.of(creatorUserUuid));
        when(userLookupService.getUserEmail(creatorUserUuid)).thenReturn(Optional.of("manager@org.test"));
        when(userLookupService.getUserFullName(creatorUserUuid)).thenReturn(Optional.of("Org Manager"));

        service.withdrawApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Schedule no longer works", null));

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.WITHDRAWN);
        assertThat(application.getReviewNotes()).isEqualTo("Schedule no longer works");

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> "CLASS_MARKETPLACE_JOB_APPLICATION_WITHDRAWN".equals(e.notificationType()));
    }

    @Test
    void withdrawApplicationRefusesOnceTheInstructorHasBeenAssigned() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.ASSIGNED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);

        assertThatThrownBy(() -> service.withdrawApplication(job.getUuid(), application.getUuid(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been assigned");
    }

    @Test
    void withdrawApplicationRefusesToTouchAnotherInstructorsApplication() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);

        assertThatThrownBy(() -> service.withdrawApplication(job.getUuid(), application.getUuid(), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void applyingAgainWhileStillInTheFunnelIsRejected() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication existing = sampleApplication(job.getUuid(), instructorUuid);
        existing.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.applyToJob(job.getUuid(),
                new ClassMarketplaceJobApplicationRequestDTO("Let me in")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active application");
    }

    @Test
    void aWithdrawnApplicantCanApplyAgain() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication existing = sampleApplication(job.getUuid(), instructorUuid);
        existing.setStatus(ClassMarketplaceJobApplicationStatus.WITHDRAWN);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid))
                .thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Back again"));

        assertThat(existing.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
        assertThat(existing.getReviewNotes()).isNull();
    }

    @Test
    void creatingTheClassClosesOutEveryoneStillInTheFunnel() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication hiredApplication = sampleApplication(job.getUuid(), instructorUuid);
        hiredApplication.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(hiredApplication.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hiredApplication.getUuid()))
                .thenReturn(Optional.of(hiredApplication));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.createClassForJob(job.getUuid());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClassMarketplaceJobApplicationStatus>> statuses =
                ArgumentCaptor.forClass(List.class);
        verify(applicationRepository).findByJobUuidAndStatusIn(eq(job.getUuid()), statuses.capture());
        assertThat(statuses.getValue()).contains(
                ClassMarketplaceJobApplicationStatus.SHORTLISTED,
                ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                ClassMarketplaceJobApplicationStatus.OFFERED);
    }

    @Test
    void creatingTheClassForAProgramJobAssignsAndClosesIt() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassProgramJob();
        ClassMarketplaceJobApplication hiredApplication = sampleApplication(job.getUuid(), instructorUuid);
        hiredApplication.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(hiredApplication.getUuid());

        ClassMarketplaceJobSessionTemplate sessionTemplate = sampleSessionTemplate(job.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hiredApplication.getUuid()))
                .thenReturn(Optional.of(hiredApplication));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(courseTrainingApprovalSpi.isInstructorApprovedForProgram(job.getProgramUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of(sessionTemplate));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any()))
                .thenReturn(List.of());
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(UUID.randomUUID()));

        ClassDefinitionDTO created = service.createClassForJob(job.getUuid());
        assertThat(created.uuid()).isEqualTo(classDefinitionUuid);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        assertThat(job.getAssignedInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(hiredApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);

        ArgumentCaptor<ClassDefinitionDTO> classCaptor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(classCaptor.capture());
        ClassDefinitionDTO forwarded = classCaptor.getValue();
        assertThat(forwarded.defaultInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(forwarded.organisationUuid()).isEqualTo(job.getOrganisationUuid());
        assertThat(forwarded.courseUuid()).isNull();
        assertThat(forwarded.programUuid()).isEqualTo(job.getProgramUuid());
    }

    @Test
    void applyToJobReopensRejectedApplication() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.REJECTED);
        application.setReviewNotes("Previous cohort already staffed");

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Available for the revised dates"));

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
        assertThat(result.applicationNote()).isEqualTo("Available for the revised dates");
        assertThat(application.getReviewNotes()).isNull();
    }

    @Test
    void applyToJobRejectsUnverifiedInstructor() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(false));

        assertThatThrownBy(() -> service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen to teach")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verified by an administrator");

        verify(applicationRepository, never()).save(any(ClassMarketplaceJobApplication.class));
    }

    @Test
    void applyToJobRejectsInstructorWithoutTrainingApproval() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen to teach")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved to deliver");

        verify(applicationRepository, never()).save(any(ClassMarketplaceJobApplication.class));
    }

    @Test
    void getMyJobEligibilityReturnsFlagsWithoutThrowing() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(false);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.empty());

        var eligibility = service.getMyJobEligibility(job.getUuid());

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.instructorVerified()).isTrue();
        assertThat(eligibility.trainingApproved()).isFalse();
        assertThat(eligibility.alreadyApplied()).isFalse();
        assertThat(eligibility.reason()).contains("not approved to deliver");
    }

    @Test
    void getMyJobEligibilityReportsEligibleInstructor() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.empty());

        var eligibility = service.getMyJobEligibility(job.getUuid());

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.instructorVerified()).isTrue();
        assertThat(eligibility.trainingApproved()).isTrue();
        assertThat(eligibility.alreadyApplied()).isFalse();
        assertThat(eligibility.applicationStatus()).isNull();
        assertThat(eligibility.canReapply()).isFalse();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    void getMyJobEligibilityBlocksAnInstructorWhoAlreadyHasALiveApplication() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication existing = sampleApplication(job.getUuid(), instructorUuid);
        existing.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.of(existing));

        var eligibility = service.getMyJobEligibility(job.getUuid());

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.alreadyApplied()).isTrue();
        assertThat(eligibility.applicationStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.SHORTLISTED);
        assertThat(eligibility.canReapply()).isFalse();
        assertThat(eligibility.reason()).contains("active application");
    }

    @Test
    void getMyJobEligibilityLetsAWithdrawnApplicantApplyAgain() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication existing = sampleApplication(job.getUuid(), instructorUuid);
        existing.setStatus(ClassMarketplaceJobApplicationStatus.WITHDRAWN);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.of(existing));

        var eligibility = service.getMyJobEligibility(job.getUuid());

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.alreadyApplied()).isTrue();
        assertThat(eligibility.applicationStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.WITHDRAWN);
        assertThat(eligibility.canReapply()).isTrue();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    void listJobApplicationsResolvesTheApprovedRateInTheJobsContractedBasis() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        job.setRateBasis(apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_DAY);
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        PageRequest pageable = PageRequest.of(0, 20);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidOrderByCreatedDateDesc(job.getUuid(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.resolveInstructorRate(
                job.getCourseUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType(),
                apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_DAY))
                .thenReturn(Optional.of(new BigDecimal("9000.00")));

        var dto = service.listJobApplications(job.getUuid(), null, pageable).getContent().getFirst();

        assertThat(dto.approvedRate()).isEqualByComparingTo(new BigDecimal("9000.00"));
    }

    @Test
    void listJobApplicationsEnrichesVerificationApprovalAndRate() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        PageRequest pageable = PageRequest.of(0, 20);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidOrderByCreatedDateDesc(job.getUuid(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.resolveInstructorRate(
                eq(job.getCourseUuid()), eq(instructorUuid), eq(job.getSessionFormat()), eq(job.getLocationType()),
                any()))
                .thenReturn(Optional.of(new BigDecimal("300.00")));

        var page = service.listJobApplications(job.getUuid(), null, pageable);

        assertThat(page.getContent()).hasSize(1);
        var dto = page.getContent().getFirst();
        assertThat(dto.instructorAdminVerified()).isTrue();
        assertThat(dto.trainingApproved()).isTrue();
        assertThat(dto.approvedRate()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void listInstructorApplicationsReturnsEmptyPageForCallerWhoManagesNothing() {
        UUID instructorUuid = UUID.randomUUID();
        UUID currentUserUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(userLookupService.getUserOrganizations(currentUserUuid)).thenReturn(List.of());

        var result = service.listInstructorApplications(instructorUuid, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(applicationRepository, never())
                .findByInstructorUuidOrderByCreatedDateDesc(any(), any());
        verify(applicationRepository, never())
                .findByInstructorUuidAndJobOrganisations(any(), any(), any(), any());
    }

    @Test
    void listInstructorApplicationsLimitsManagerToTheirOwnOrganisationsJobs() {
        UUID instructorUuid = UUID.randomUUID();
        UUID currentUserUuid = UUID.randomUUID();
        UUID managedOrganisationUuid = UUID.randomUUID();
        UUID otherOrganisationUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        ClassMarketplaceJobApplication application = sampleApplication(UUID.randomUUID(), instructorUuid);

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(userLookupService.getUserOrganizations(currentUserUuid))
                .thenReturn(List.of(managedOrganisationUuid, otherOrganisationUuid));
        when(domainSecurityService.managesOrganisation(managedOrganisationUuid)).thenReturn(true);
        when(domainSecurityService.managesOrganisation(otherOrganisationUuid)).thenReturn(false);
        when(applicationRepository.findByInstructorUuidAndJobOrganisations(
                instructorUuid, null, List.of(managedOrganisationUuid), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        var result = service.listInstructorApplications(instructorUuid, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(applicationRepository).findByInstructorUuidAndJobOrganisations(
                instructorUuid, null, List.of(managedOrganisationUuid), pageable);
    }

    @Test
    void listJobsHidesInstructorPayFromCallersWhoAreNotVerifiedInstructorsOrManagers() {
        ClassMarketplaceJob job = sampleJob();
        job.setInstructorPay(new BigDecimal("18000.00"));
        PageRequest pageable = PageRequest.of(0, 20);

        when(jobRepository.search(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(job), pageable, 1));

        var page = service.listJobs(null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().salePrice()).isEqualTo(job.getSalePrice());
        assertThat(page.getContent().getFirst().instructorPay()).isNull();
    }

    @Test
    void listJobsKeepsInstructorPayForVerifiedInstructors() {
        ClassMarketplaceJob job = sampleJob();
        job.setInstructorPay(new BigDecimal("18000.00"));
        PageRequest pageable = PageRequest.of(0, 20);

        when(domainSecurityService.isVerifiedInstructor()).thenReturn(true);
        when(jobRepository.search(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(job), pageable, 1));

        var page = service.listJobs(null, null, null, null, pageable);

        assertThat(page.getContent().getFirst().instructorPay())
                .isEqualByComparingTo(new BigDecimal("18000.00"));
    }

    @Test
    void listJobsKeepsInstructorPayForTheOrganisationThatPostedIt() {
        ClassMarketplaceJob job = sampleJob();
        job.setInstructorPay(new BigDecimal("18000.00"));
        PageRequest pageable = PageRequest.of(0, 20);

        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
        when(jobRepository.search(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(job), pageable, 1));

        var page = service.listJobs(null, null, null, null, pageable);

        assertThat(page.getContent().getFirst().instructorPay())
                .isEqualByComparingTo(new BigDecimal("18000.00"));
    }

    @Test
    void listJobsDoesNotApplyHiddenStatusFilterWhenStatusMissing() {
        UUID organisationUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        when(jobRepository.search(
                organisationUuid,
                courseUuid,
                null,
                null,
                PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(sampleJob()), PageRequest.of(0, 20), 1));

        var page = service.listJobs(
                organisationUuid,
                courseUuid,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(jobRepository).search(
                organisationUuid,
                courseUuid,
                null,
                null,
                PageRequest.of(0, 20)
        );
    }

    private ClassMarketplaceJobRequestDTO sampleRequest(UUID courseUuid, UUID programUuid) {
        return new ClassMarketplaceJobRequestDTO(
                UUID.randomUUID(),
                courseUuid,
                programUuid,
                "Weekend Data Analysis Bootcamp",
                "School advert for an approved class slot",
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 5, 1),
                30,
                "#1F6FEB",
                LocationType.HYBRID,
                "Nairobi Campus - Lab 2",
                new BigDecimal("-1.292066"),
                new BigDecimal("36.821945"),
                "https://meet.google.com/abc-defg-hij",
                24,
                true,
                new BigDecimal("240.00"),
                new BigDecimal("240.00"),
                null,
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 5, 2, 9, 0),
                        LocalDateTime.of(2026, 5, 2, 12, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "SATURDAY",
                                null,
                                null,
                                6
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                ClassServiceType.ONLINE,
                null,
                List.of("Grade 1", "Grade 2"),
                null,
                null,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE
        );
    }

    private ClassMarketplaceJobRequestDTO withSchedule(ClassMarketplaceJobRequestDTO base,
                                                       LocalDateTime defaultStartTime,
                                                       LocalDateTime defaultEndTime,
                                                       LocalDateTime templateStartTime,
                                                       LocalDateTime templateEndTime) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), defaultStartTime, defaultEndTime,
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(),
                List.of(new ClassSessionTemplateDTO(
                        null,
                        templateStartTime,
                        templateEndTime,
                        null,
                        "Africa/Nairobi",
                        ConflictResolutionStrategy.FAIL
                )),
                base.resources(), base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(),
                base.targetGroupUuids(), base.categoryUuid(), base.remindStudents(), base.remindInstructor(),
                base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJobRequestDTO withTargetGroupUuids(ClassMarketplaceJobRequestDTO base, List<UUID> groupUuids) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(), base.sessionTemplates(), base.resources(),
                base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(), groupUuids, base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJobRequestDTO withPricing(ClassMarketplaceJobRequestDTO base,
                                                      BigDecimal salePrice,
                                                      BigDecimal instructorPay) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), salePrice, instructorPay,
                null, base.sessionTemplates(),
                base.resources(), base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(),
                base.targetGroupUuids(), base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJobRequestDTO withTrainingFee(ClassMarketplaceJobRequestDTO base, BigDecimal trainingFee) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), trainingFee, trainingFee, base.rateBasis(), base.sessionTemplates(), base.resources(),
                base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(), base.targetGroupUuids(),
                base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJobRequestDTO withCategory(ClassMarketplaceJobRequestDTO base, UUID categoryUuid) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(), base.sessionTemplates(), base.resources(),
                base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(), base.targetGroupUuids(),
                categoryUuid, base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJobRequestDTO withPreferredInstructor(ClassMarketplaceJobRequestDTO base, UUID instructorUuid) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(), base.sessionTemplates(), base.resources(),
                base.serviceType(), instructorUuid, base.targetGroups(), base.targetGroupUuids(), base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ClassMarketplaceJob sampleJob() {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setOrganisationUuid(UUID.randomUUID());
        job.setCourseUuid(UUID.randomUUID());
        job.setTitle("Weekend Data Analysis Bootcamp");
        job.setDescription("Advert for a school-owned bootcamp");
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setClassVisibility(ClassVisibility.PUBLIC);
        job.setSessionFormat(SessionFormat.GROUP);
        job.setDefaultStartTime(LocalDateTime.of(2026, 5, 2, 9, 0));
        job.setDefaultEndTime(LocalDateTime.of(2026, 5, 2, 12, 0));
        job.setAcademicPeriodStartDate(LocalDate.of(2026, 5, 2));
        job.setAcademicPeriodEndDate(LocalDate.of(2026, 6, 6));
        job.setRegistrationPeriodStartDate(LocalDate.of(2026, 4, 20));
        job.setRegistrationPeriodEndDate(LocalDate.of(2026, 5, 1));
        job.setClassReminderMinutes(30);
        job.setClassColor("#1F6FEB");
        job.setLocationType(LocationType.HYBRID);
        job.setLocationName("Nairobi Campus - Lab 2");
        job.setLocationLatitude(new BigDecimal("-1.292066"));
        job.setLocationLongitude(new BigDecimal("36.821945"));
        job.setMeetingLink("https://meet.google.com/abc-defg-hij");
        job.setMaxParticipants(24);
        job.setAllowWaitlist(true);
        return job;
    }

    private ClassMarketplaceJob sampleProgramJob() {
        ClassMarketplaceJob job = sampleJob();
        job.setCourseUuid(null);
        job.setProgramUuid(UUID.randomUUID());
        return job;
    }

    /** The shape a hire leaves behind: waiting for its class, with the winning application recorded. */
    private ClassMarketplaceJob awaitingClassJob() {
        ClassMarketplaceJob job = sampleJob();
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        return job;
    }

    private ClassMarketplaceJob awaitingClassProgramJob() {
        ClassMarketplaceJob job = sampleProgramJob();
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        return job;
    }


    // ===== resource holds on posting =====

    @Test
    void createJobWithResourcesPlacesHoldsForEveryExpandedOccurrence() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID venueUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO base = sampleRequest(null, programUuid);
        ClassMarketplaceJobRequestDTO request = withResources(base,
                List.of(new ClassMarketplaceJobResourceDTO(venueUuid, null)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(venueUuid)).thenReturn(Optional.of(
                venueSummary(venueUuid, request.organisationUuid(), 30, true)));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class)))
                .thenAnswer(invocation -> List.of(sampleSessionTemplate(invocation.getArgument(0))));

        service.createJob(request);

        ArgumentCaptor<List<ResourceBookingRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceBookingService).holdResourcesForJob(any(UUID.class), eq(request.organisationUuid()), requestsCaptor.capture());
        List<ResourceBookingRequest> holdRequests = requestsCaptor.getValue();
        assertThat(holdRequests).hasSize(1);
        assertThat(holdRequests.getFirst().resourceUuid()).isEqualTo(venueUuid);
        assertThat(holdRequests.getFirst().quantity()).isEqualTo(1);
        // weekly Saturday template with occurrence_count 6 expands to 6 windows
        assertThat(holdRequests.getFirst().windows()).hasSize(6);
        assertThat(holdRequests.getFirst().windows().getFirst().start())
                .isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
    }

    @Test
    void createJobWithoutResourcesPlacesNoHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    job.setUuid(UUID.randomUUID());
                    return job;
                });
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());

        service.createJob(request);

        verify(resourceBookingService, never()).holdResourcesForJob(any(), any(), any());
    }

    @Test
    void createJobRejectsResourceOfAnotherOrganisation() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID venueUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withResources(sampleRequest(null, programUuid),
                List.of(new ClassMarketplaceJobResourceDTO(venueUuid, null)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(venueUuid)).thenReturn(Optional.of(
                venueSummary(venueUuid, UUID.randomUUID(), 30, true)));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to organisation");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJobRejectsDeactivatedResource() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID venueUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withResources(sampleRequest(null, programUuid),
                List.of(new ClassMarketplaceJobResourceDTO(venueUuid, null)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(venueUuid)).thenReturn(Optional.of(
                venueSummary(venueUuid, request.organisationUuid(), 30, false)));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void createJobRejectsMoreThanOneVenue() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID venueA = UUID.randomUUID();
        UUID venueB = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withResources(sampleRequest(null, programUuid),
                List.of(new ClassMarketplaceJobResourceDTO(venueA, null),
                        new ClassMarketplaceJobResourceDTO(venueB, null)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(venueA)).thenReturn(Optional.of(
                venueSummary(venueA, request.organisationUuid(), 30, true)));
        when(resourceLookupService.getResource(venueB)).thenReturn(Optional.of(
                venueSummary(venueB, request.organisationUuid(), 30, true)));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most one venue");
    }

    @Test
    void createJobRejectsVenueSmallerThanMaxParticipants() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID venueUuid = UUID.randomUUID();
        // sampleRequest uses max_participants 24
        ClassMarketplaceJobRequestDTO request = withResources(sampleRequest(null, programUuid),
                List.of(new ClassMarketplaceJobResourceDTO(venueUuid, null)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(venueUuid)).thenReturn(Optional.of(
                venueSummary(venueUuid, request.organisationUuid(), 20, true)));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the seat capacity");
    }

    @Test
    void createJobRejectsEquipmentQuantityAbovePoolTotal() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID poolUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withResources(sampleRequest(null, programUuid),
                List.of(new ClassMarketplaceJobResourceDTO(poolUuid, 40)));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(resourceLookupService.getResource(poolUuid)).thenReturn(Optional.of(new ResourceSummary(
                poolUuid, request.organisationUuid(), null, ResourceType.EQUIPMENT_POOL, "Laptops", null, 25, true)));

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the total");
    }

    @Test
    void cancelJobReleasesResourceHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(jobRepository.save(any(ClassMarketplaceJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of());
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());

        service.cancelJob(job.getUuid());

        verify(resourceBookingService).releaseHoldsForJob(job.getUuid(), "Job cancelled");
    }

    @Test
    void cancelJobAwaitingClassReleasesTheAssignedApplication() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        job.setAssignedInstructorUuid(instructorUuid);

        ClassMarketplaceJobApplication assigned = sampleApplication(job.getUuid(), instructorUuid);
        assigned.setStatus(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        job.setAssignedApplicationUuid(assigned.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), assigned.getUuid()))
                .thenReturn(Optional.of(assigned));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of());
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());

        var result = service.cancelJob(job.getUuid());

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobStatus.CANCELLED);
        assertThat(assigned.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
        assertThat(job.getAssignedInstructorUuid()).isNull();
        assertThat(job.getAssignedApplicationUuid()).isNull();
        verify(resourceBookingService).releaseHoldsForJob(job.getUuid(), "Job cancelled");
    }

    @Test
    void createClassForJobIsUnreachableWithoutAHire() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());

        assertThatThrownBy(() -> service.createClassForJob(job.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("once an applicant has been hired");

        verify(classDefinitionService, never()).createClassDefinition(any(ClassDefinitionDTO.class));
        verifyNoInteractions(organisationAffiliationService);
    }

    @Test
    void createClassForJobRefusesWhenTheRecordedApplicationIsNotAHire() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication offered = sampleApplication(job.getUuid(), instructorUuid);
        offered.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);
        job.setAssignedApplicationUuid(offered.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), offered.getUuid()))
                .thenReturn(Optional.of(offered));

        // Assignment stays gated on the hire: an offer is not a commitment.
        assertThatThrownBy(() -> service.createClassForJob(job.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at the offered stage");

        verify(classDefinitionService, never()).createClassDefinition(any(ClassDefinitionDTO.class));
    }

    // ===== application schedule hard-block =====

    @Test
    void applyToJobRejectsInstructorWithOverlappingSchedule() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        // existing session overlaps the first Saturday occurrence (2026-05-02 09:00-12:00)
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(scheduledInstance(
                        LocalDateTime.of(2026, 5, 2, 10, 0),
                        LocalDateTime.of(2026, 5, 2, 11, 0),
                        SchedulingStatus.SCHEDULED)));

        assertThatThrownBy(() -> service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen")))
                .isInstanceOfSatisfying(SchedulingConflictException.class, ex -> {
                    assertThat(ex.getConflicts()).hasSize(1);
                    assertThat(ex.getConflicts().getFirst().requestedStart())
                            .isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
                });

        verify(applicationRepository, never()).save(any(ClassMarketplaceJobApplication.class));
    }

    @Test
    void applyToJobIgnoresCompletedAndBoundaryTouchingSessions() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        // completed session overlapping the window: ignored
                        scheduledInstance(
                                LocalDateTime.of(2026, 5, 2, 10, 0),
                                LocalDateTime.of(2026, 5, 2, 11, 0),
                                SchedulingStatus.COMPLETED),
                        // back-to-back session ending exactly at the occurrence start: no overlap
                        scheduledInstance(
                                LocalDateTime.of(2026, 5, 2, 7, 0),
                                LocalDateTime.of(2026, 5, 2, 9, 0),
                                SchedulingStatus.SCHEDULED)));
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen"));

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
    }

    @Test
    void applyToJobRejectsInstructorMarkedUnavailable() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(availabilityService.isInstructorAvailable(eq(instructorUuid), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen")))
                .isInstanceOfSatisfying(SchedulingConflictException.class, ex ->
                        assertThat(ex.getConflicts()).hasSize(6));
    }

    @Test
    void getMyJobEligibilityReportsScheduleConflicts() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.empty());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(scheduledInstance(
                        LocalDateTime.of(2026, 5, 9, 9, 0),
                        LocalDateTime.of(2026, 5, 9, 12, 0),
                        SchedulingStatus.BLOCKED)));

        var eligibility = service.getMyJobEligibility(job.getUuid());

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.instructorVerified()).isTrue();
        assertThat(eligibility.trainingApproved()).isTrue();
        assertThat(eligibility.scheduleClear()).isFalse();
        assertThat(eligibility.scheduleConflicts()).hasSize(1);
        assertThat(eligibility.scheduleConflicts().getFirst().requestedStart())
                .isEqualTo(LocalDateTime.of(2026, 5, 9, 9, 0));
        assertThat(eligibility.reason()).contains("conflicts with 1");
    }

    // ===== assignment hold conversion =====

    @Test
    void creatingTheClassConfirmsHoldsAndCopiesResources() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(application.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("manager@org.test"));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        ScheduledInstanceDTO instance = scheduledInstance(
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0),
                SchedulingStatus.SCHEDULED);
        when(timetableService.getScheduledInstancesForClassDefinition(classDefinitionUuid))
                .thenReturn(List.of(instance));
        apps.sarafrika.elimika.classes.model.ClassMarketplaceJobResource jobResource =
                new apps.sarafrika.elimika.classes.model.ClassMarketplaceJobResource();
        jobResource.setJobUuid(job.getUuid());
        jobResource.setResourceUuid(UUID.randomUUID());
        jobResource.setQuantity(1);
        when(jobResourceRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(jobResource));
        when(resourceLookupService.getResource(jobResource.getResourceUuid())).thenReturn(Optional.of(
                venueSummary(jobResource.getResourceUuid(), job.getOrganisationUuid(), 30, true)));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());

        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(UUID.randomUUID()));

        service.createClassForJob(job.getUuid());

        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);

        ArgumentCaptor<List<InstanceWindow>> windowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceBookingService).confirmHoldsForJob(eq(job.getUuid()), eq(classDefinitionUuid), windowsCaptor.capture());
        assertThat(windowsCaptor.getValue()).hasSize(1);
        assertThat(windowsCaptor.getValue().getFirst().scheduledInstanceUuid()).isEqualTo(instance.uuid());

        ArgumentCaptor<List<apps.sarafrika.elimika.classes.model.ClassDefinitionResource>> copiesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(classDefinitionResourceRepository).saveAll(copiesCaptor.capture());
        assertThat(copiesCaptor.getValue()).hasSize(1);
        assertThat(copiesCaptor.getValue().getFirst().getClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(copiesCaptor.getValue().getFirst().getResourceUuid()).isEqualTo(jobResource.getResourceUuid());

        ArgumentCaptor<ClassDefinitionDTO> definitionCaptor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue().venueResourceUuid()).isEqualTo(jobResource.getResourceUuid());
        assertThat(definitionCaptor.getValue().marketplaceJobUuid()).isEqualTo(job.getUuid());
        assertThat(definitionCaptor.getValue().sessionTemplates().getFirst().timezone())
                .isEqualTo("Africa/Nairobi");
    }

    @Test
    void creatingTheClassRefusesWhenScheduleConflictsAppearedAfterTheHire() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(application.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(scheduledInstance(
                        LocalDateTime.of(2026, 5, 16, 9, 0),
                        LocalDateTime.of(2026, 5, 16, 12, 0),
                        SchedulingStatus.SCHEDULED)));

        assertThatThrownBy(() -> service.createClassForJob(job.getUuid()))
                .isInstanceOf(SchedulingConflictException.class);

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        verify(classDefinitionService, never()).createClassDefinition(any(ClassDefinitionDTO.class));
        verify(resourceBookingService, never()).confirmHoldsForJob(any(), any(), any());
    }

    // ===== hiring affiliates on every path =====

    @Test
    void createJobWithPreferredInstructorAffiliatesTheInstructor() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request =
                withPreferredInstructor(sampleRequest(null, programUuid), instructorUuid);

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(any(UUID.class))).thenReturn(List.of());
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJob job = invocation.getArgument(0);
                    if (job.getUuid() == null) {
                        job.setUuid(UUID.randomUUID());
                    }
                    return job;
                });
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(
                        createdClassDefinition(classDefinitionUuid, instructorUuid, sampleJob())));

        var result = service.createJob(request);

        assertThat(result.assignedInstructorUuid()).isEqualTo(instructorUuid);
        // Direct hire is a hire: without this the organisation gets a class taught by a non-member.
        verify(organisationAffiliationService)
                .affiliateHiredInstructor(instructorUserUuid, request.organisationUuid(), null);
    }

    @Test
    void creatingTheClassNeverAffiliatesAgainBecauseTheHireAlreadyDid() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication hired = sampleApplication(job.getUuid(), instructorUuid);
        hired.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(hired.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hired.getUuid()))
                .thenReturn(Optional.of(hired));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.createClassForJob(job.getUuid());

        // Membership is the hire's doing; the class must never rewrite a role the instructor holds.
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        verifyNoInteractions(organisationAffiliationService);
    }

    @Test
    void hiringAnApplicantAffiliatesThemWithTheOrganisation() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(organisationAffiliationService.affiliateHiredInstructor(
                instructorUserUuid, job.getOrganisationUuid(), null)).thenReturn(true);

        var hired = service.hireApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Strong fit", null));

        // The hire is the affiliation, and it also leaves the job waiting only for its class:
        // nobody has to assign anybody for the instructor to have joined the organisation.
        assertThat(hired.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.AWAITING_CLASS);
        assertThat(job.getAssignedApplicationUuid()).isEqualTo(application.getUuid());
        verify(organisationAffiliationService)
                .affiliateHiredInstructor(instructorUserUuid, job.getOrganisationUuid(), null);
    }

    @Test
    void hiringAnInstructorWhoAlreadyBelongsLeavesTheirRoleIntact() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.OFFERED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        // Already an admin of this organisation, so tenancy reports that no new mapping was made.
        when(organisationAffiliationService.affiliateHiredInstructor(
                instructorUserUuid, job.getOrganisationUuid(), null)).thenReturn(false);

        var hired = service.hireApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Already one of ours", null));

        assertThat(hired.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        // Exactly one call, to the idempotent method: nothing here may rewrite the role they hold.
        verify(organisationAffiliationService, times(1))
                .affiliateHiredInstructor(instructorUserUuid, job.getOrganisationUuid(), null);
        verifyNoMoreInteractions(organisationAffiliationService);
    }

    // ===== instructor time holds =====

    @Test
    void applyToJobHoldsEveryOccurrenceWindow() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> {
                    ClassMarketplaceJobApplication saved = invocation.getArgument(0);
                    saved.setUuid(applicationUuid);
                    return saved;
                });
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));

        service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen"));

        ArgumentCaptor<InstructorTimeHoldRequest> holdCaptor =
                ArgumentCaptor.forClass(InstructorTimeHoldRequest.class);
        verify(instructorTimeHoldService).holdForApplication(holdCaptor.capture());
        InstructorTimeHoldRequest held = holdCaptor.getValue();
        // weekly Saturday template with occurrence_count 6 expands to 6 windows
        assertThat(held.windows()).hasSize(6);
        assertThat(held.windows().getFirst().start()).isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
        assertThat(held.windows().getFirst().end()).isEqualTo(LocalDateTime.of(2026, 5, 2, 12, 0));
        assertThat(held.jobUuid()).isEqualTo(job.getUuid());
        assertThat(held.applicationUuid()).isEqualTo(applicationUuid);
        assertThat(held.instructorUuid()).isEqualTo(instructorUuid);
        assertThat(held.instructorUserUuid()).isEqualTo(instructorUserUuid);
        assertThat(held.organisationUuid()).isEqualTo(job.getOrganisationUuid());
        assertThat(held.title()).isEqualTo(job.getTitle());
        assertThat(held.timezone()).isEqualTo("Africa/Nairobi");
    }

    @Test
    void applyToJobIsNotBlockedByAnotherApplicationsTentativeHold() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        // The instructor already holds this very window tentatively for another job. Only FIRM
        // holds are ever returned as blocking, so the diary comes back clear.
        when(instructorTimeHoldService.findBlockingHolds(
                eq(instructorUuid), any(LocalDateTime.class), any(LocalDateTime.class), eq(job.getUuid())))
                .thenReturn(List.of());
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Keen"));

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
        verify(instructorTimeHoldService).holdForApplication(any(InstructorTimeHoldRequest.class));
    }

    @Test
    void creatingTheClassIsNotBlockedByItsOwnJobsHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication hired = sampleApplication(job.getUuid(), instructorUuid);
        hired.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(hired.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), hired.getUuid()))
                .thenReturn(Optional.of(hired));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.createClassForJob(job.getUuid());

        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        // The job's own holds are excluded by uuid, so hiring never conflicts with itself.
        ArgumentCaptor<UUID> excludedJob = ArgumentCaptor.forClass(UUID.class);
        verify(instructorTimeHoldService, atLeastOnce()).findBlockingHolds(
                eq(instructorUuid), any(LocalDateTime.class), any(LocalDateTime.class), excludedJob.capture());
        assertThat(excludedJob.getAllValues()).containsOnly(job.getUuid());
    }

    @Test
    void creatingTheClassIsBlockedByAFirmHoldOnAnotherJob() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication approved = sampleApplication(job.getUuid(), instructorUuid);
        approved.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(approved.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), approved.getUuid()))
                .thenReturn(Optional.of(approved));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        // Hired elsewhere across the first Saturday occurrence (2026-05-02 09:00-12:00).
        when(instructorTimeHoldService.findBlockingHolds(
                eq(instructorUuid), any(LocalDateTime.class), any(LocalDateTime.class), eq(job.getUuid())))
                .thenReturn(List.of(firmHold(
                        LocalDateTime.of(2026, 5, 2, 10, 0),
                        LocalDateTime.of(2026, 5, 2, 11, 0))));

        assertThatThrownBy(() -> service.createClassForJob(job.getUuid()))
                .isInstanceOfSatisfying(SchedulingConflictException.class, ex -> {
                    assertThat(ex.getConflicts()).hasSize(1);
                    assertThat(ex.getConflicts().getFirst().requestedStart())
                            .isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
                    assertThat(ex.getConflicts().getFirst().reasons())
                            .contains("Instructor is already committed to another class job in this window");
                });

        verify(instructorTimeHoldService, never()).firmOrCreateHoldsForApplication(any());
    }

    @Test
    void creatingTheClassFirmsTheHiredHoldsAndReleasesTheRest() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication approved = sampleApplication(job.getUuid(), instructorUuid);
        approved.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        job.setAssignedApplicationUuid(approved.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), approved.getUuid()))
                .thenReturn(Optional.of(approved));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.createClassForJob(job.getUuid());

        ArgumentCaptor<InstructorTimeHoldRequest> hireCaptor =
                ArgumentCaptor.forClass(InstructorTimeHoldRequest.class);
        InOrder holds = inOrder(instructorTimeHoldService);
        holds.verify(instructorTimeHoldService).firmOrCreateHoldsForApplication(hireCaptor.capture());
        holds.verify(instructorTimeHoldService).releaseHoldsForJobExcept(
                job.getUuid(), approved.getUuid(), "Another instructor was hired");
        // The windows travel with the hire so an application that predates the holds table still
        // ends up with a firm claim on the diary rather than nothing to promote.
        InstructorTimeHoldRequest hired = hireCaptor.getValue();
        assertThat(hired.applicationUuid()).isEqualTo(approved.getUuid());
        assertThat(hired.jobUuid()).isEqualTo(job.getUuid());
        assertThat(hired.windows()).hasSize(6);
    }

    @Test
    void rejectApplicationReleasesItsHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.rejectApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Not a fit this time", null));

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.REJECTED);
        verify(instructorTimeHoldService)
                .releaseHoldsForApplication(application.getUuid(), "Application rejected");
    }

    @Test
    void withdrawApplicationReleasesItsHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("instructor@example.com"));

        service.withdrawApplication(job.getUuid(), application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Schedule no longer works", null));

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.WITHDRAWN);
        verify(instructorTimeHoldService)
                .releaseHoldsForApplication(application.getUuid(), "Instructor withdrew");
    }

    @Test
    void updateJobRebuildsHoldsForActiveApplications() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = sampleRequest(null, programUuid);

        ClassMarketplaceJob job = sampleProgramJob();
        job.setOrganisationUuid(request.organisationUuid());
        job.setProgramUuid(programUuid);

        ClassMarketplaceJobApplication live = sampleApplication(job.getUuid(), instructorUuid);
        live.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any()))
                .thenReturn(List.of(live));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.updateJob(job.getUuid(), request);

        // Stale windows go first, then every application still in the funnel is re-held.
        InOrder holds = inOrder(instructorTimeHoldService);
        holds.verify(instructorTimeHoldService)
                .releaseHoldsForJob(job.getUuid(), "Job updated; holds re-evaluated");
        ArgumentCaptor<InstructorTimeHoldRequest> holdCaptor =
                ArgumentCaptor.forClass(InstructorTimeHoldRequest.class);
        holds.verify(instructorTimeHoldService).holdForApplication(holdCaptor.capture());
        assertThat(holdCaptor.getValue().applicationUuid()).isEqualTo(live.getUuid());
        assertThat(holdCaptor.getValue().windows()).hasSize(6);
    }

    @Test
    void createClassForJobConfirmsMatchingHoldsAndReleasesTheRest() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        job.setAssignedInstructorUuid(instructorUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(sampleSessionTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(
                        createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        ScheduledInstanceDTO instance = scheduledInstance(
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0),
                SchedulingStatus.SCHEDULED);
        when(timetableService.getScheduledInstancesForClassDefinition(classDefinitionUuid))
                .thenReturn(List.of(instance));
        when(jobRepository.save(any(ClassMarketplaceJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createClassForJob(job.getUuid());

        // Same windows the resource holds are confirmed against; unscheduled ones are
        // released inside the hold service, which InstructorTimeHoldServiceImplTest pins.
        ArgumentCaptor<List<InstanceWindow>> windowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(instructorTimeHoldService)
                .confirmHoldsForJob(eq(job.getUuid()), eq(classDefinitionUuid), windowsCaptor.capture());
        assertThat(windowsCaptor.getValue()).hasSize(1);
        assertThat(windowsCaptor.getValue().getFirst().scheduledInstanceUuid()).isEqualTo(instance.uuid());
        assertThat(windowsCaptor.getValue().getFirst().startTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 2, 9, 0));
        verify(resourceBookingService)
                .confirmHoldsForJob(eq(job.getUuid()), eq(classDefinitionUuid), any());
    }

    @Test
    void cancelJobReleasesInstructorHolds() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(jobRepository.save(any(ClassMarketplaceJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of());
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), any())).thenReturn(List.of());

        service.cancelJob(job.getUuid());

        verify(instructorTimeHoldService).releaseHoldsForJob(job.getUuid(), "Job cancelled");
    }

    // ===== the funnel refuses skipped decisions =====

    /** Every stage an application can still be moved out of. */
    private static final List<ClassMarketplaceJobApplicationStatus> LIVE_STAGES = List.of(
            ClassMarketplaceJobApplicationStatus.PENDING,
            ClassMarketplaceJobApplicationStatus.SHORTLISTED,
            ClassMarketplaceJobApplicationStatus.INTERVIEWING,
            ClassMarketplaceJobApplicationStatus.OFFERED,
            ClassMarketplaceJobApplicationStatus.HIRED);

    /** The stages an application reaches while its job is still open; a hire ends that. */
    private static final List<ClassMarketplaceJobApplicationStatus> STAGES_WHILE_JOB_IS_OPEN = List.of(
            ClassMarketplaceJobApplicationStatus.PENDING,
            ClassMarketplaceJobApplicationStatus.SHORTLISTED,
            ClassMarketplaceJobApplicationStatus.INTERVIEWING,
            ClassMarketplaceJobApplicationStatus.OFFERED);

    @Test
    void theFunnelIsWalkedOneStageAtATime() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        LocalDateTime interviewAt = LocalDateTime.of(2026, 4, 25, 9, 30);

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));

        service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.SHORTLISTED, null);
        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                new ClassMarketplaceJobDecisionRequestDTO(null, interviewAt));
        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.INTERVIEWING);

        service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.OFFERED, null);
        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.OFFERED);

        service.hireApplication(job.getUuid(), application.getUuid(), null);
        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.AWAITING_CLASS);
    }

    @Test
    void hiringAnApplicantWhoWasNeverOfferedIsRefusedNamingBothStages() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.hireApplication(job.getUuid(), application.getUuid(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending")
                .hasMessageContaining("hired")
                .hasMessageContaining("offered");

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
        verifyNoInteractions(organisationAffiliationService);
    }

    @Test
    void skippingTheInterviewIsRefusedNamingBothStages() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());
        application.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.moveApplicationToStage(job.getUuid(), application.getUuid(),
                ClassMarketplaceJobApplicationStatus.OFFERED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shortlisted")
                .hasMessageContaining("offered")
                .hasMessageContaining("interviewing");

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.SHORTLISTED);
        verify(applicationRepository, never()).save(any(ClassMarketplaceJobApplication.class));
    }

    @Test
    void refusingIsReachableFromEveryStageTheJobIsStillOpenIn() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));

        for (ClassMarketplaceJobApplicationStatus stage : STAGES_WHILE_JOB_IS_OPEN) {
            ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
            application.setStatus(stage);
            when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                    .thenReturn(Optional.of(application));

            service.rejectApplication(job.getUuid(), application.getUuid(), null);

            assertThat(application.getStatus()).as("rejected from %s", stage)
                    .isEqualTo(ClassMarketplaceJobApplicationStatus.REJECTED);
        }
    }

    @Test
    void refusingAHiredApplicantIsRefusedBecauseTheHireTookTheJobOutOfOpen() {
        // The only job a hired application can sit under is the AWAITING_CLASS one the hire
        // left behind, and reject is shut from there - releasing a hire is a different action.
        ClassMarketplaceJob job = awaitingClassJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());
        application.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.rejectApplication(job.getUuid(), application.getUuid(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only open marketplace class jobs");

        assertThat(application.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void withdrawingIsReachableFromEveryLiveStage() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("instructor@example.com"));

        for (ClassMarketplaceJobApplicationStatus stage : LIVE_STAGES) {
            // Withdrawal belongs to the applicant, so it outlives the hire that closes the job.
            ClassMarketplaceJob job = stage == ClassMarketplaceJobApplicationStatus.HIRED
                    ? awaitingClassJob()
                    : sampleJob();
            ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), instructorUuid);
            application.setStatus(stage);
            when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
            when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid()))
                    .thenReturn(Optional.of(application));

            service.withdrawApplication(job.getUuid(), application.getUuid(), null);

            assertThat(application.getStatus()).as("withdrawn from %s", stage)
                    .isEqualTo(ClassMarketplaceJobApplicationStatus.WITHDRAWN);
        }
    }

    // ===== a recruitment window must outlive the day it opens =====

    @Test
    void createJobRefusesARegistrationWindowThatClosesTheDayItOpens() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withRegistrationWindow(
                sampleRequest(null, programUuid), LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 20));

        allowOrganisationAccess(currentUserUuid, request.organisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opens and closes on the same day (2026-04-20)");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    @Test
    void updateJobRefusesARegistrationWindowThatClosesTheDayItOpens() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        ClassMarketplaceJobRequestDTO request = withRegistrationWindow(
                sampleRequest(null, programUuid), LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 20));

        ClassMarketplaceJob job = sampleProgramJob();
        job.setOrganisationUuid(request.organisationUuid());
        job.setProgramUuid(programUuid);

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        allowOrganisationAccess(currentUserUuid, job.getOrganisationUuid());
        when(courseInfoService.trainingProgramExists(programUuid)).thenReturn(true);
        when(courseInfoService.isTrainingProgramApproved(programUuid)).thenReturn(true);
        when(courseTrainingApprovalSpi.isOrganisationApprovedForProgram(programUuid, request.organisationUuid()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateJob(job.getUuid(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opens and closes on the same day (2026-04-20)");

        verify(jobRepository, never()).save(any(ClassMarketplaceJob.class));
    }

    private ClassMarketplaceJobRequestDTO withRegistrationWindow(ClassMarketplaceJobRequestDTO base,
                                                                 LocalDate start,
                                                                 LocalDate end) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), start, end,
                base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(),
                base.sessionTemplates(), base.resources(), base.serviceType(), base.preferredInstructorUuid(),
                base.targetGroups(), base.targetGroupUuids(), base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private InstructorTimeHoldDTO firmHold(LocalDateTime start, LocalDateTime end) {
        return new InstructorTimeHoldDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Another School",
                "Grade 5 Piano - Term 2",
                start,
                end,
                "UTC",
                InstructorTimeHoldStatus.FIRM,
                null,
                null
        );
    }

    private ClassMarketplaceJobRequestDTO withResources(ClassMarketplaceJobRequestDTO base,
                                                        List<ClassMarketplaceJobResourceDTO> resources) {
        return new ClassMarketplaceJobRequestDTO(
                base.organisationUuid(), base.courseUuid(), base.programUuid(), base.title(), base.description(),
                base.classVisibility(), base.sessionFormat(), base.defaultStartTime(), base.defaultEndTime(),
                base.academicPeriodStartDate(), base.academicPeriodEndDate(), base.registrationPeriodStartDate(),
                base.registrationPeriodEndDate(), base.classReminderMinutes(), base.classColor(), base.locationType(),
                base.locationName(), base.locationLatitude(), base.locationLongitude(), base.meetingLink(),
                base.maxParticipants(), base.allowWaitlist(), base.salePrice(), base.instructorPay(), base.rateBasis(), base.sessionTemplates(), resources,
                base.serviceType(), base.preferredInstructorUuid(), base.targetGroups(), base.targetGroupUuids(), base.categoryUuid(), base.remindStudents(),
                base.remindInstructor(), base.remindViaEmail(), base.remindViaSms(), base.remindViaPush());
    }

    private ResourceSummary venueSummary(UUID resourceUuid, UUID organisationUuid, int seatCapacity, boolean active) {
        return new ResourceSummary(resourceUuid, organisationUuid, null, ResourceType.VENUE,
                "Physics Lab", seatCapacity, null, active);
    }

    private ScheduledInstanceDTO scheduledInstance(LocalDateTime start, LocalDateTime end, SchedulingStatus status) {
        return new ScheduledInstanceDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                start,
                end,
                "UTC",
                "Existing session",
                "ONLINE",
                null,
                null,
                null,
                25,
                status,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void allowOrganisationAccess(UUID currentUserUuid, UUID organisationUuid) {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);
    }

    private ClassMarketplaceJobApplication sampleApplication(UUID jobUuid, UUID instructorUuid) {
        ClassMarketplaceJobApplication application = new ClassMarketplaceJobApplication();
        application.setUuid(UUID.randomUUID());
        application.setJobUuid(jobUuid);
        application.setInstructorUuid(instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);
        application.setApplicationNote("Ready to deliver");
        return application;
    }

    private ClassMarketplaceJobSessionTemplate sampleSessionTemplate(UUID jobUuid) {
        ClassMarketplaceJobSessionTemplate template = new ClassMarketplaceJobSessionTemplate();
        template.setUuid(UUID.randomUUID());
        template.setJobUuid(jobUuid);
        template.setStartTime(LocalDateTime.of(2026, 5, 2, 9, 0));
        template.setEndTime(LocalDateTime.of(2026, 5, 2, 12, 0));
        template.setTimezone("Africa/Nairobi");
        template.setRecurrenceType("WEEKLY");
        template.setIntervalValue(1);
        template.setDaysOfWeek("SATURDAY");
        template.setOccurrenceCount(6);
        template.setConflictResolution(ConflictResolutionStrategy.FAIL.name());
        return template;
    }

    private ClassDefinitionDTO createdClassDefinition(UUID classDefinitionUuid,
                                                      UUID instructorUuid,
                                                      ClassMarketplaceJob job) {
        return new ClassDefinitionDTO(
                classDefinitionUuid,
                job.getTitle(),
                job.getDescription(),
                instructorUuid,
                job.getOrganisationUuid(),
                null,
                job.getCourseUuid(),
                job.getProgramUuid(),
                new BigDecimal("2500.00"),
                new BigDecimal("2500.00"),
                job.getRateBasis(),
                job.getClassVisibility(),
                job.getSessionFormat(),
                job.getDefaultStartTime(),
                job.getDefaultEndTime(),
                job.getAcademicPeriodStartDate(),
                job.getAcademicPeriodEndDate(),
                job.getRegistrationPeriodStartDate(),
                job.getRegistrationPeriodEndDate(),
                job.getClassReminderMinutes(),
                job.getClassColor(),
                job.getLocationType(),
                job.getLocationName(),
                job.getLocationLatitude(),
                job.getLocationLongitude(),
                job.getMeetingLink(),
                job.getMaxParticipants(),
                job.getAllowWaitlist(),
                true,
                List.of(new ClassSessionTemplateDTO(
                        job.getDefaultStartTime(),
                        job.getDefaultEndTime(),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "SATURDAY",
                                null,
                                null,
                                6
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                null,
                null,
                null
        );
    }
}
