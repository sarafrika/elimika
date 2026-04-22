package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.LocationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassMarketplaceJobServiceImpl implements ClassMarketplaceJobServiceInterface {

    private static final String JOB_NOT_FOUND_TEMPLATE = "Marketplace class job with UUID %s not found";
    private static final String APPLICATION_NOT_FOUND_TEMPLATE = "Marketplace job application %s not found for job %s";
    private static final int DEFAULT_MAX_PARTICIPANTS = 50;

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobApplicationRepository applicationRepository;
    private final ClassMarketplaceJobSessionTemplateRepository sessionTemplateRepository;
    private final CourseInfoService courseInfoService;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    private final UserLookupService userLookupService;
    private final InstructorLookupService instructorLookupService;
    private final DomainSecurityService domainSecurityService;
    private final ClassDefinitionServiceInterface classDefinitionService;

    @Override
    public ClassMarketplaceJobDTO createJob(ClassMarketplaceJobRequestDTO request) {
        requireOrganisationManagerAccess(request.organisationUuid());
        validateJobDraft(request);

        ClassMarketplaceJob job = new ClassMarketplaceJob();
        applyJobDraft(job, request);
        job.setStatus(ClassMarketplaceJobStatus.OPEN);

        ClassMarketplaceJob saved = jobRepository.save(job);
        replaceSessionTemplates(saved.getUuid(), request.sessionTemplates());

        return toJobDTO(saved);
    }

    @Override
    public ClassMarketplaceJobDTO updateJob(UUID jobUuid, ClassMarketplaceJobRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        if (!job.getOrganisationUuid().equals(request.organisationUuid())) {
            throw new IllegalArgumentException("organisation_uuid cannot be changed after a marketplace job has been created");
        }

        validateJobDraft(request);
        applyJobDraft(job, request);
        ClassMarketplaceJob saved = jobRepository.save(job);
        replaceSessionTemplates(saved.getUuid(), request.sessionTemplates());

        return toJobDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassMarketplaceJobDTO getJob(UUID jobUuid) {
        return toJobDTO(getJobEntity(jobUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobDTO> listJobs(UUID organisationUuid,
                                                 UUID courseUuid,
                                                 ClassMarketplaceJobStatus status,
                                                 org.springframework.data.domain.Pageable pageable) {
        return jobRepository.search(organisationUuid, courseUuid, status, pageable)
                .map(this::toJobDTO);
    }

    @Override
    public ClassMarketplaceJobDTO cancelJob(UUID jobUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        job.setStatus(ClassMarketplaceJobStatus.CANCELLED);
        ClassMarketplaceJob saved = jobRepository.save(job);
        markOtherApplicationsAsNotSelected(jobUuid, null);
        return toJobDTO(saved);
    }

    @Override
    public ClassMarketplaceJobApplicationDTO applyToJob(UUID jobUuid, ClassMarketplaceJobApplicationRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);

        UUID instructorUuid = resolveCurrentInstructorUuid();

        ClassMarketplaceJobApplication application = applicationRepository.findByJobUuidAndInstructorUuid(jobUuid, instructorUuid)
                .map(existing -> reopenApplication(existing, request))
                .orElseGet(() -> createApplication(jobUuid, instructorUuid, request));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        return toApplicationDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listJobApplications(UUID jobUuid,
                                                                       ClassMarketplaceJobApplicationStatus status,
                                                                       org.springframework.data.domain.Pageable pageable) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        requireOrganisationManagerAccess(job.getOrganisationUuid());
        if (status == null) {
            return applicationRepository.findByJobUuidOrderByCreatedDateDesc(jobUuid, pageable)
                    .map(this::toApplicationDTO);
        }
        return applicationRepository.findByJobUuidAndStatusOrderByCreatedDateDesc(jobUuid, status, pageable)
                .map(this::toApplicationDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listMyApplications(ClassMarketplaceJobApplicationStatus status,
                                                                      org.springframework.data.domain.Pageable pageable) {
        UUID instructorUuid = resolveCurrentInstructorUuid();
        if (status == null) {
            return applicationRepository.findByInstructorUuidOrderByCreatedDateDesc(instructorUuid, pageable)
                    .map(this::toApplicationDTO);
        }
        return applicationRepository.findByInstructorUuidAndStatusOrderByCreatedDateDesc(instructorUuid, status, pageable)
                .map(this::toApplicationDTO);
    }

    @Override
    public ClassMarketplaceJobApplicationDTO approveApplication(UUID jobUuid,
                                                                UUID applicationUuid,
                                                                ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureApplicationReviewable(application);

        if (!courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), application.getInstructorUuid())) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is not approved to deliver course %s. Only instructors with approved course delivery access can be approved for this job.",
                    application.getInstructorUuid(),
                    job.getCourseUuid()));
        }

        application.setStatus(ClassMarketplaceJobApplicationStatus.APPROVED);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toApplicationDTO(applicationRepository.save(application));
    }

    @Override
    public ClassMarketplaceJobApplicationDTO rejectApplication(UUID jobUuid,
                                                               UUID applicationUuid,
                                                               ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureApplicationReviewable(application);

        application.setStatus(ClassMarketplaceJobApplicationStatus.REJECTED);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toApplicationDTO(applicationRepository.save(application));
    }

    @Override
    public ClassMarketplaceJobAssignmentResponseDTO assignInstructor(UUID jobUuid,
                                                                     ClassMarketplaceJobAssignmentRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, request.applicationUuid());
        if (application.getStatus() != ClassMarketplaceJobApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only approved applications can be assigned to create a class.");
        }

        if (!courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), application.getInstructorUuid())) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is no longer approved to deliver course %s.",
                    application.getInstructorUuid(),
                    job.getCourseUuid()));
        }

        ClassDefinitionDTO classDefinition = classDefinitionService
                .createClassDefinition(buildClassDefinitionRequest(job, application.getInstructorUuid()))
                .classDefinition();

        application.setStatus(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        application.setReviewNotes(resolveAssignedReviewNotes(application.getReviewNotes()));
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        applicationRepository.save(application);

        job.setStatus(ClassMarketplaceJobStatus.FILLED);
        job.setAssignedInstructorUuid(application.getInstructorUuid());
        job.setAssignedApplicationUuid(application.getUuid());
        job.setAssignedClassDefinitionUuid(classDefinition.uuid());
        job.setFilledAt(LocalDateTime.now(ZoneOffset.UTC));
        ClassMarketplaceJob savedJob = jobRepository.save(job);

        markOtherApplicationsAsNotSelected(jobUuid, application.getUuid());

        return new ClassMarketplaceJobAssignmentResponseDTO(toJobDTO(savedJob), classDefinition);
    }

    private void applyJobDraft(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request) {
        job.setOrganisationUuid(request.organisationUuid());
        job.setCourseUuid(request.courseUuid());
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setClassVisibility(request.classVisibility());
        job.setSessionFormat(request.sessionFormat());
        job.setDefaultStartTime(request.defaultStartTime());
        job.setDefaultEndTime(request.defaultEndTime());
        job.setAcademicPeriodStartDate(request.academicPeriodStartDate());
        job.setAcademicPeriodEndDate(request.academicPeriodEndDate());
        job.setRegistrationPeriodStartDate(request.registrationPeriodStartDate());
        job.setRegistrationPeriodEndDate(request.registrationPeriodEndDate());
        job.setClassReminderMinutes(request.classReminderMinutes());
        job.setClassColor(request.classColor());
        job.setLocationType(request.locationType());
        job.setLocationName(request.locationName());
        job.setLocationLatitude(request.locationLatitude());
        job.setLocationLongitude(request.locationLongitude());
        job.setMeetingLink(request.meetingLink());
        job.setMaxParticipants(request.maxParticipants() != null ? request.maxParticipants() : DEFAULT_MAX_PARTICIPANTS);
        job.setAllowWaitlist(request.allowWaitlist() != null ? request.allowWaitlist() : Boolean.TRUE);
    }

    private void validateJobDraft(ClassMarketplaceJobRequestDTO request) {
        if (!courseInfoService.courseExists(request.courseUuid())) {
            throw new ResourceNotFoundException(String.format("Course with UUID %s not found", request.courseUuid()));
        }

        if (!courseInfoService.isCourseApproved(request.courseUuid())) {
            throw new IllegalStateException(String.format(
                    "Course %s is not approved for delivery. Organisations may only advertise classes for approved courses.",
                    request.courseUuid()));
        }

        if (!courseTrainingApprovalSpi.isOrganisationApproved(request.courseUuid(), request.organisationUuid())) {
            throw new IllegalStateException(String.format(
                    "Organisation %s is not approved to deliver course %s. Approve the organisation's course training application before posting marketplace class jobs.",
                    request.organisationUuid(),
                    request.courseUuid()));
        }

        validateLocationRequirements(request.locationType(), request.locationName(), request.locationLatitude(), request.locationLongitude());
        validateSessionTemplates(request.sessionTemplates());
    }

    private void validateLocationRequirements(LocationType locationType,
                                              String locationName,
                                              BigDecimal locationLatitude,
                                              BigDecimal locationLongitude) {
        if (locationType == null || LocationType.ONLINE.equals(locationType)) {
            return;
        }

        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("location_name is required when location_type is IN_PERSON or HYBRID");
        }
        if (locationLatitude == null || locationLongitude == null) {
            throw new IllegalArgumentException("location_latitude and location_longitude are required when location_type is IN_PERSON or HYBRID");
        }
        if (locationLatitude.compareTo(new BigDecimal("-90")) < 0 || locationLatitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("location_latitude must be between -90 and 90 degrees");
        }
        if (locationLongitude.compareTo(new BigDecimal("-180")) < 0 || locationLongitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException("location_longitude must be between -180 and 180 degrees");
        }
    }

    private void validateSessionTemplates(List<ClassSessionTemplateDTO> sessionTemplates) {
        if (sessionTemplates == null || sessionTemplates.isEmpty()) {
            throw new IllegalArgumentException("At least one session template must be provided");
        }

        for (ClassSessionTemplateDTO template : sessionTemplates) {
            if (template == null || template.startTime() == null || template.endTime() == null) {
                throw new IllegalArgumentException("Session templates require both start_time and end_time");
            }
            if (!template.startTime().isBefore(template.endTime())) {
                throw new IllegalArgumentException("Session template start_time must be before end_time");
            }
        }
    }

    private void replaceSessionTemplates(UUID jobUuid, List<ClassSessionTemplateDTO> sessionTemplates) {
        sessionTemplateRepository.deleteByJobUuid(jobUuid);

        List<ClassMarketplaceJobSessionTemplate> entities = new ArrayList<>();
        for (ClassSessionTemplateDTO templateDTO : sessionTemplates) {
            ClassMarketplaceJobSessionTemplate template = new ClassMarketplaceJobSessionTemplate();
            template.setJobUuid(jobUuid);
            template.setStartTime(templateDTO.startTime());
            template.setEndTime(templateDTO.endTime());
            if (templateDTO.recurrence() != null && templateDTO.recurrence().recurrenceType() != null) {
                template.setRecurrenceType(templateDTO.recurrence().recurrenceType().name());
                template.setIntervalValue(templateDTO.recurrence().intervalValue());
                template.setDaysOfWeek(templateDTO.recurrence().daysOfWeek());
                template.setDayOfMonth(templateDTO.recurrence().dayOfMonth());
                template.setEndDate(templateDTO.recurrence().endDate());
                template.setOccurrenceCount(templateDTO.recurrence().occurrenceCount());
            }
            template.setConflictResolution(Optional.ofNullable(templateDTO.conflictResolution())
                    .orElse(apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy.FAIL)
                    .name());
            entities.add(template);
        }

        sessionTemplateRepository.saveAll(entities);
    }

    private ClassMarketplaceJobApplication reopenApplication(ClassMarketplaceJobApplication existing,
                                                             ClassMarketplaceJobApplicationRequestDTO request) {
        if (existing.getStatus() == ClassMarketplaceJobApplicationStatus.PENDING
                || existing.getStatus() == ClassMarketplaceJobApplicationStatus.APPROVED) {
            throw new IllegalStateException("You already have an active application for this marketplace job.");
        }
        if (existing.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
            throw new IllegalStateException("You have already been assigned to this marketplace job.");
        }

        existing.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);
        existing.setApplicationNote(request == null ? null : request.applicationNote());
        existing.setReviewNotes(null);
        existing.setReviewedBy(null);
        existing.setReviewedAt(null);
        return existing;
    }

    private ClassMarketplaceJobApplication createApplication(UUID jobUuid,
                                                             UUID instructorUuid,
                                                             ClassMarketplaceJobApplicationRequestDTO request) {
        ClassMarketplaceJobApplication application = new ClassMarketplaceJobApplication();
        application.setJobUuid(jobUuid);
        application.setInstructorUuid(instructorUuid);
        application.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);
        application.setApplicationNote(request == null ? null : request.applicationNote());
        return application;
    }

    private void ensureJobOpen(ClassMarketplaceJob job) {
        if (job.getStatus() != ClassMarketplaceJobStatus.OPEN) {
            throw new IllegalStateException("Only open marketplace class jobs can accept this action.");
        }
    }

    private void ensureApplicationReviewable(ClassMarketplaceJobApplication application) {
        if (application.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
            throw new IllegalStateException("Assigned applications cannot be reviewed again.");
        }
        if (application.getStatus() == ClassMarketplaceJobApplicationStatus.NOT_SELECTED) {
            throw new IllegalStateException("Applications already marked as not selected cannot be reviewed again.");
        }
    }

    private void requireOrganisationManagerAccess(UUID organisationUuid) {
        UUID currentUserUuid = requireCurrentUserUuid();

        boolean hasOrganisationUserAccess = userLookupService.userBelongsToOrganizationWithDomain(
                currentUserUuid,
                organisationUuid,
                UserDomain.organisation_user
        );
        boolean hasAdminAccess = userLookupService.userBelongsToOrganizationWithDomain(
                currentUserUuid,
                organisationUuid,
                UserDomain.admin
        );

        if (!hasOrganisationUserAccess && !hasAdminAccess) {
            throw new AccessDeniedException(String.format(
                    "User %s is not allowed to manage marketplace jobs for organisation %s.",
                    currentUserUuid,
                    organisationUuid));
        }
    }

    private UUID resolveCurrentInstructorUuid() {
        UUID currentUserUuid = requireCurrentUserUuid();
        if (!domainSecurityService.isInstructor()) {
            throw new AccessDeniedException("Only instructors can apply to marketplace class jobs.");
        }

        return instructorLookupService.findInstructorUuidByUserUuid(currentUserUuid)
                .orElseThrow(() -> new AccessDeniedException("The current user does not have an instructor profile."));
    }

    private UUID requireCurrentUserUuid() {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        if (currentUserUuid == null) {
            throw new AccessDeniedException("An authenticated user is required for this action.");
        }
        return currentUserUuid;
    }

    private String resolveReviewer() {
        UUID currentUserUuid = requireCurrentUserUuid();
        return userLookupService.getUserEmail(currentUserUuid).orElse(currentUserUuid.toString());
    }

    private ClassMarketplaceJob getJobEntity(UUID jobUuid) {
        return jobRepository.findByUuid(jobUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_TEMPLATE, jobUuid)));
    }

    private ClassMarketplaceJobApplication getApplication(UUID jobUuid, UUID applicationUuid) {
        return applicationRepository.findByJobUuidAndUuid(jobUuid, applicationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, jobUuid)));
    }

    private void markOtherApplicationsAsNotSelected(UUID jobUuid, UUID selectedApplicationUuid) {
        List<ClassMarketplaceJobApplication> openApplications = applicationRepository.findByJobUuidAndStatusIn(
                jobUuid,
                List.of(
                        ClassMarketplaceJobApplicationStatus.PENDING,
                        ClassMarketplaceJobApplicationStatus.APPROVED
                )
        );

        List<ClassMarketplaceJobApplication> toUpdate = new ArrayList<>();
        for (ClassMarketplaceJobApplication application : openApplications) {
            if (selectedApplicationUuid != null && application.getUuid().equals(selectedApplicationUuid)) {
                continue;
            }
            application.setStatus(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
            if (application.getReviewNotes() == null || application.getReviewNotes().isBlank()) {
                application.setReviewNotes("Another instructor was selected for this class job.");
            }
            application.setReviewedBy(resolveReviewer());
            application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
            toUpdate.add(application);
        }

        if (!toUpdate.isEmpty()) {
            applicationRepository.saveAll(toUpdate);
        }
    }

    private ClassDefinitionDTO buildClassDefinitionRequest(ClassMarketplaceJob job, UUID instructorUuid) {
        return new ClassDefinitionDTO(
                null,
                job.getTitle(),
                job.getDescription(),
                instructorUuid,
                job.getOrganisationUuid(),
                job.getCourseUuid(),
                null,
                null,
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
                Boolean.TRUE,
                loadSessionTemplates(job.getUuid()),
                null,
                null,
                null,
                null
        );
    }

    private List<ClassSessionTemplateDTO> loadSessionTemplates(UUID jobUuid) {
        return sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid)
                .stream()
                .map(this::toSessionTemplateDTO)
                .toList();
    }

    private ClassSessionTemplateDTO toSessionTemplateDTO(ClassMarketplaceJobSessionTemplate entity) {
        ClassRecurrenceDTO recurrence = null;
        if (entity.getRecurrenceType() != null) {
            recurrence = new ClassRecurrenceDTO(
                    ClassRecurrenceDTO.RecurrenceType.valueOf(entity.getRecurrenceType()),
                    entity.getIntervalValue(),
                    entity.getDaysOfWeek(),
                    entity.getDayOfMonth(),
                    entity.getEndDate(),
                    entity.getOccurrenceCount()
            );
        }

        return new ClassSessionTemplateDTO(
                entity.getStartTime(),
                entity.getEndTime(),
                recurrence,
                apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy.valueOf(
                        Optional.ofNullable(entity.getConflictResolution()).orElse("FAIL")
                )
        );
    }

    private ClassMarketplaceJobDTO toJobDTO(ClassMarketplaceJob job) {
        return new ClassMarketplaceJobDTO(
                job.getUuid(),
                job.getOrganisationUuid(),
                job.getCourseUuid(),
                job.getTitle(),
                job.getDescription(),
                job.getStatus(),
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
                job.getAssignedInstructorUuid(),
                job.getAssignedApplicationUuid(),
                job.getAssignedClassDefinitionUuid(),
                job.getFilledAt(),
                loadSessionTemplates(job.getUuid()),
                job.getCreatedDate(),
                job.getLastModifiedDate(),
                job.getCreatedBy(),
                job.getLastModifiedBy()
        );
    }

    private ClassMarketplaceJobApplicationDTO toApplicationDTO(ClassMarketplaceJobApplication application) {
        return new ClassMarketplaceJobApplicationDTO(
                application.getUuid(),
                application.getJobUuid(),
                application.getInstructorUuid(),
                application.getStatus(),
                application.getApplicationNote(),
                application.getReviewNotes(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedDate(),
                application.getLastModifiedDate(),
                application.getCreatedBy(),
                application.getLastModifiedBy()
        );
    }

    private String resolveAssignedReviewNotes(String existingReviewNotes) {
        if (existingReviewNotes == null || existingReviewNotes.isBlank()) {
            return "Application selected for class assignment.";
        }
        return existingReviewNotes;
    }
}
