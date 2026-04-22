package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
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
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
    private CourseInfoService courseInfoService;

    @Mock
    private CourseTrainingApprovalSpi courseTrainingApprovalSpi;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private DomainSecurityService domainSecurityService;

    @Mock
    private ClassDefinitionServiceInterface classDefinitionService;

    private ClassMarketplaceJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassMarketplaceJobServiceImpl(
                jobRepository,
                applicationRepository,
                sessionTemplateRepository,
                courseInfoService,
                courseTrainingApprovalSpi,
                userLookupService,
                instructorLookupService,
                domainSecurityService,
                classDefinitionService
        );
    }

    @Test
    void approveApplicationRejectsInstructorWithoutCourseApproval() {
        UUID currentUserUuid = UUID.randomUUID();
        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication application = sampleApplication(job.getUuid(), UUID.randomUUID());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(userLookupService.userBelongsToOrganizationWithDomain(currentUserUuid, job.getOrganisationUuid(), UserDomain.organisation_user))
                .thenReturn(true);
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), application.getUuid())).thenReturn(Optional.of(application));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), application.getInstructorUuid())).thenReturn(false);

        assertThatThrownBy(() -> service.approveApplication(
                job.getUuid(),
                application.getUuid(),
                new ClassMarketplaceJobDecisionRequestDTO("Needs course approval first")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only instructors with approved course delivery access can be approved");
    }

    @Test
    void assignInstructorCreatesClassAndClosesJob() {
        UUID currentUserUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        ClassMarketplaceJob job = sampleJob();
        ClassMarketplaceJobApplication approvedApplication = sampleApplication(job.getUuid(), instructorUuid);
        approvedApplication.setStatus(ClassMarketplaceJobApplicationStatus.APPROVED);

        ClassMarketplaceJobApplication otherApplication = sampleApplication(job.getUuid(), UUID.randomUUID());
        otherApplication.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);

        ClassMarketplaceJobSessionTemplate sessionTemplate = sampleSessionTemplate(job.getUuid());

        when(jobRepository.findByUuid(job.getUuid())).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobUuidAndUuid(job.getUuid(), approvedApplication.getUuid()))
                .thenReturn(Optional.of(approvedApplication));
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(currentUserUuid);
        when(userLookupService.userBelongsToOrganizationWithDomain(currentUserUuid, job.getOrganisationUuid(), UserDomain.organisation_user))
                .thenReturn(true);
        when(userLookupService.getUserEmail(currentUserUuid)).thenReturn(Optional.of("org-user@example.com"));
        when(courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid)).thenReturn(true);
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid())).thenReturn(List.of(sessionTemplate));
        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(createdClassDefinition(classDefinitionUuid, instructorUuid, job)));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(ClassMarketplaceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByJobUuidAndStatusIn(
                job.getUuid(),
                List.of(ClassMarketplaceJobApplicationStatus.PENDING, ClassMarketplaceJobApplicationStatus.APPROVED)))
                .thenReturn(List.of(otherApplication));

        var response = service.assignInstructor(
                job.getUuid(),
                new ClassMarketplaceJobAssignmentRequestDTO(approvedApplication.getUuid())
        );

        assertThat(response.classDefinition().uuid()).isEqualTo(classDefinitionUuid);
        assertThat(response.job().status()).isEqualTo(ClassMarketplaceJobStatus.FILLED);
        assertThat(response.job().assignedInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(response.job().assignedClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(approvedApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        assertThat(otherApplication.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);

        ArgumentCaptor<ClassDefinitionDTO> classCaptor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(classCaptor.capture());
        ClassDefinitionDTO forwarded = classCaptor.getValue();
        assertThat(forwarded.defaultInstructorUuid()).isEqualTo(instructorUuid);
        assertThat(forwarded.organisationUuid()).isEqualTo(job.getOrganisationUuid());
        assertThat(forwarded.courseUuid()).isEqualTo(job.getCourseUuid());
        assertThat(forwarded.trainingFee()).isNull();
        assertThat(forwarded.sessionTemplates()).hasSize(1);
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
        when(instructorLookupService.findInstructorUuidByUserUuid(currentUserUuid)).thenReturn(Optional.of(instructorUuid));
        when(applicationRepository.findByJobUuidAndInstructorUuid(job.getUuid(), instructorUuid)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(ClassMarketplaceJobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyToJob(job.getUuid(), new ClassMarketplaceJobApplicationRequestDTO("Available for the revised dates"));

        assertThat(result.status()).isEqualTo(ClassMarketplaceJobApplicationStatus.PENDING);
        assertThat(result.applicationNote()).isEqualTo("Available for the revised dates");
        assertThat(application.getReviewNotes()).isNull();
    }

    @Test
    void listJobsDoesNotApplyHiddenStatusFilterWhenStatusMissing() {
        UUID organisationUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        when(jobRepository.search(
                organisationUuid,
                courseUuid,
                null,
                PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(sampleJob()), PageRequest.of(0, 20), 1));

        var page = service.listJobs(
                organisationUuid,
                courseUuid,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(jobRepository).search(
                organisationUuid,
                courseUuid,
                null,
                PageRequest.of(0, 20)
        );
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
                job.getCourseUuid(),
                null,
                new BigDecimal("2500.00"),
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
