package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobEligibilityDTO;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobResourceDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.model.ClassDefinitionResource;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobResource;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobResourceRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.classes.util.RecurrencePatterns;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingRequest;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceSummary;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceExpander;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.LocationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ClassMarketplaceJobResourceRepository jobResourceRepository;
    private final ClassDefinitionResourceRepository classDefinitionResourceRepository;
    private final CourseInfoService courseInfoService;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    private final UserLookupService userLookupService;
    private final InstructorLookupService instructorLookupService;
    private final DomainSecurityService domainSecurityService;
    private final ClassDefinitionServiceInterface classDefinitionService;
    private final ResourceBookingService resourceBookingService;
    private final ResourceLookupService resourceLookupService;
    private final AvailabilityService availabilityService;
    private final ObjectProvider<TimetableService> timetableServiceProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ClassMarketplaceJobDTO createJob(ClassMarketplaceJobRequestDTO request) {
        requireOrganisationManagerAccess(request.organisationUuid());
        validateJobDraft(request);

        ClassMarketplaceJob job = new ClassMarketplaceJob();
        applyJobDraft(job, request);
        job.setStatus(ClassMarketplaceJobStatus.OPEN);

        ClassMarketplaceJob saved = jobRepository.save(job);
        replaceSessionTemplates(saved.getUuid(), request.sessionTemplates());
        replaceJobResources(saved.getUuid(), request.resources());
        holdJobResources(saved, request.resources());

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
        resourceBookingService.releaseHoldsForJob(jobUuid, "Job updated; holds re-evaluated");
        replaceJobResources(saved.getUuid(), request.resources());
        holdJobResources(saved, request.resources());

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
                                                 UUID programUuid,
                                                 ClassMarketplaceJobStatus status,
                                                 org.springframework.data.domain.Pageable pageable) {
        return jobRepository.search(organisationUuid, courseUuid, programUuid, status, pageable)
                .map(this::toJobDTO);
    }

    @Override
    public ClassMarketplaceJobDTO cancelJob(UUID jobUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        job.setStatus(ClassMarketplaceJobStatus.CANCELLED);
        ClassMarketplaceJob saved = jobRepository.save(job);
        resourceBookingService.releaseHoldsForJob(jobUuid, "Job cancelled");
        markOtherApplicationsAsNotSelected(jobUuid, null);
        return toJobDTO(saved);
    }

    @Override
    public ClassMarketplaceJobApplicationDTO applyToJob(UUID jobUuid, ClassMarketplaceJobApplicationRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);

        UUID instructorUuid = resolveCurrentInstructorUuid();
        ensureInstructorEligibleToApply(job, instructorUuid);

        ClassMarketplaceJobApplication application = applicationRepository.findByJobUuidAndInstructorUuid(jobUuid, instructorUuid)
                .map(existing -> reopenApplication(existing, request))
                .orElseGet(() -> createApplication(jobUuid, instructorUuid, request));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        return toApplicationDTO(saved, job);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassMarketplaceJobEligibilityDTO getMyJobEligibility(UUID jobUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        UUID instructorUuid = resolveCurrentInstructorUuid();

        boolean instructorVerified = isInstructorAdminVerified(instructorUuid);
        boolean trainingApproved = isInstructorApprovedForJob(job, instructorUuid);
        boolean alreadyApplied = applicationRepository.findByJobUuidAndInstructorUuid(jobUuid, instructorUuid).isPresent();
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        boolean scheduleClear = scheduleConflicts.isEmpty();
        boolean eligible = instructorVerified && trainingApproved && scheduleClear;

        String reason = null;
        if (!instructorVerified) {
            reason = "Your instructor profile must be verified by an administrator before applying to marketplace class jobs.";
        } else if (!trainingApproved) {
            reason = String.format(
                    "You are not approved to deliver this %s. Submit a training application and wait for approval before applying.",
                    learningContextType(job));
        } else if (!scheduleClear) {
            reason = String.format(
                    "Your existing schedule conflicts with %d of this job's planned sessions.",
                    scheduleConflicts.size());
        }

        return new ClassMarketplaceJobEligibilityDTO(eligible, instructorVerified, trainingApproved, alreadyApplied,
                scheduleClear, scheduleClear ? null : scheduleConflicts, reason);
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
                    .map(application -> toApplicationDTO(application, job));
        }
        return applicationRepository.findByJobUuidAndStatusOrderByCreatedDateDesc(jobUuid, status, pageable)
                .map(application -> toApplicationDTO(application, job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listMyApplications(ClassMarketplaceJobApplicationStatus status,
                                                                      org.springframework.data.domain.Pageable pageable) {
        UUID instructorUuid = resolveCurrentInstructorUuid();
        return listInstructorApplications(instructorUuid, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listInstructorApplications(UUID instructorUuid,
                                                                              ClassMarketplaceJobApplicationStatus status,
                                                                              org.springframework.data.domain.Pageable pageable) {
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

        if (!isInstructorApprovedForJob(job, application.getInstructorUuid())) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is not approved to deliver %s %s. Only instructors with approved %s delivery access can be approved for this job.",
                    application.getInstructorUuid(),
                    learningContextType(job),
                    learningContextUuid(job),
                    learningContextType(job)));
        }

        application.setStatus(ClassMarketplaceJobApplicationStatus.APPROVED);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toApplicationDTO(applicationRepository.save(application), job);
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

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        notifyApplicantUnsuccessful(job, saved,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_REJECTED, "was not successful");
        return toApplicationDTO(saved, job);
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

        if (!isInstructorApprovedForJob(job, application.getInstructorUuid())) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is no longer approved to deliver %s %s.",
                    application.getInstructorUuid(),
                    learningContextType(job),
                    learningContextUuid(job)));
        }

        List<ClassSchedulingConflictDTO> scheduleConflicts =
                findInstructorScheduleConflicts(job, application.getInstructorUuid());
        if (!scheduleConflicts.isEmpty()) {
            throw new SchedulingConflictException(String.format(
                    "Instructor %s has schedule conflicts with this job's planned sessions.",
                    application.getInstructorUuid()),
                    scheduleConflicts);
        }

        resolveInstructorRateForJob(job, application.getInstructorUuid()).ifPresent(approvedRate -> {
            if (job.getTrainingFee() != null && job.getTrainingFee().compareTo(approvedRate) != 0) {
                log.warn("Marketplace job {} fee {} differs from instructor {} approved rate {}",
                        job.getUuid(), job.getTrainingFee(), application.getInstructorUuid(), approvedRate);
            }
        });

        ClassDefinitionDTO classDefinition = classDefinitionService
                .createClassDefinition(buildClassDefinitionRequest(job, application.getInstructorUuid()))
                .classDefinition();

        convertHoldsToConfirmedBookings(job, classDefinition.uuid());
        copyJobResourcesToClassDefinition(job.getUuid(), classDefinition.uuid());

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
        job.setProgramUuid(request.programUuid());
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
        job.setTrainingFee(request.trainingFee());
    }

    private void validateJobDraft(ClassMarketplaceJobRequestDTO request) {
        validateLearningContext(request);
        validateLocationRequirements(request.locationType(), request.locationName(), request.locationLatitude(), request.locationLongitude());
        validateSessionTemplates(request.sessionTemplates());
        validateJobResources(request);
    }

    private void validateJobResources(ClassMarketplaceJobRequestDTO request) {
        List<ClassMarketplaceJobResourceDTO> resources = request.resources();
        if (resources == null || resources.isEmpty()) {
            return;
        }

        int effectiveMaxParticipants = request.maxParticipants() != null ? request.maxParticipants() : DEFAULT_MAX_PARTICIPANTS;
        Set<UUID> seen = new HashSet<>();
        int venueCount = 0;
        for (ClassMarketplaceJobResourceDTO resource : resources) {
            if (resource == null || resource.resourceUuid() == null) {
                throw new IllegalArgumentException("Every job resource entry requires a resource_uuid");
            }
            if (!seen.add(resource.resourceUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Resource %s is listed more than once on the job", resource.resourceUuid()));
            }

            ResourceSummary summary = resourceLookupService.getResource(resource.resourceUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(
                            "Organisation resource with UUID %s not found", resource.resourceUuid())));
            if (!summary.organisationUuid().equals(request.organisationUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Resource '%s' does not belong to organisation %s", summary.name(), request.organisationUuid()));
            }
            if (!summary.active()) {
                throw new IllegalArgumentException(String.format(
                        "Resource '%s' is deactivated and cannot be attached to a job", summary.name()));
            }

            int quantity = resolveResourceQuantity(resource);
            if (summary.resourceType() == ResourceType.VENUE) {
                venueCount++;
                if (quantity != 1) {
                    throw new IllegalArgumentException(String.format(
                            "Venue '%s' must be booked with quantity 1", summary.name()));
                }
                if (summary.seatCapacity() != null && effectiveMaxParticipants > summary.seatCapacity()) {
                    throw new IllegalArgumentException(String.format(
                            "max_participants %d exceeds the seat capacity %d of venue '%s'",
                            effectiveMaxParticipants, summary.seatCapacity(), summary.name()));
                }
            } else if (summary.totalQuantity() != null && quantity > summary.totalQuantity()) {
                throw new IllegalArgumentException(String.format(
                        "Requested quantity %d exceeds the total %d units of equipment pool '%s'",
                        quantity, summary.totalQuantity(), summary.name()));
            }
        }

        if (venueCount > 1) {
            throw new IllegalArgumentException("A marketplace job can reserve at most one venue");
        }
    }

    private void validateLearningContext(ClassMarketplaceJobRequestDTO request) {
        boolean hasCourse = request.courseUuid() != null;
        boolean hasProgram = request.programUuid() != null;
        if (hasCourse == hasProgram) {
            throw new IllegalArgumentException("Exactly one of course_uuid or program_uuid is required for marketplace class jobs");
        }

        if (hasCourse) {
            validateCourseLearningContext(request);
            return;
        }

        validateProgramLearningContext(request);
    }

    private void validateCourseLearningContext(ClassMarketplaceJobRequestDTO request) {
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
    }

    private void validateProgramLearningContext(ClassMarketplaceJobRequestDTO request) {
        if (!courseInfoService.trainingProgramExists(request.programUuid())) {
            throw new ResourceNotFoundException(String.format("Training program with UUID %s not found", request.programUuid()));
        }

        if (!courseInfoService.isTrainingProgramApproved(request.programUuid())) {
            throw new IllegalStateException(String.format(
                    "Training program %s is not approved for delivery. Organisations may only advertise classes for approved training programs.",
                    request.programUuid()));
        }

        if (!courseTrainingApprovalSpi.isOrganisationApprovedForProgram(request.programUuid(), request.organisationUuid())) {
            throw new IllegalStateException(String.format(
                    "Organisation %s is not approved to deliver training program %s. Approve the organisation's program training application before posting marketplace class jobs.",
                    request.organisationUuid(),
                    request.programUuid()));
        }
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

    private void ensureInstructorEligibleToApply(ClassMarketplaceJob job, UUID instructorUuid) {
        if (!isInstructorAdminVerified(instructorUuid)) {
            throw new IllegalStateException(
                    "Your instructor profile must be verified by an administrator before applying to marketplace class jobs.");
        }
        if (!isInstructorApprovedForJob(job, instructorUuid)) {
            throw new IllegalStateException(String.format(
                    "You are not approved to deliver %s %s. Submit a training application for this %s and wait for approval before applying.",
                    learningContextType(job),
                    learningContextUuid(job),
                    learningContextType(job)));
        }
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        if (!scheduleConflicts.isEmpty()) {
            throw new SchedulingConflictException(
                    "Your existing schedule conflicts with this job's planned sessions.",
                    scheduleConflicts);
        }
    }

    /**
     * Expands the job's session templates and reports every occurrence that clashes
     * with the instructor's existing schedule (scheduled sessions and blocked time;
     * cancelled and completed instances are ignored) or declared unavailability.
     */
    private List<ClassSchedulingConflictDTO> findInstructorScheduleConflicts(ClassMarketplaceJob job, UUID instructorUuid) {
        List<OccurrenceWindow> occurrences = expandJobOccurrences(job.getUuid());
        if (occurrences.isEmpty()) {
            return List.of();
        }

        LocalDate minDate = occurrences.stream().map(w -> w.start().toLocalDate()).min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = occurrences.stream().map(w -> w.end().toLocalDate()).max(LocalDate::compareTo).orElseThrow();
        List<ScheduledInstanceDTO> existingSchedule = timetableService()
                .getScheduleForInstructor(instructorUuid, minDate, maxDate)
                .stream()
                .filter(instance -> instance.status() != SchedulingStatus.COMPLETED)
                .toList();

        List<ClassSchedulingConflictDTO> conflicts = new ArrayList<>();
        for (OccurrenceWindow occurrence : occurrences) {
            List<String> reasons = new ArrayList<>();
            boolean overlapsSchedule = existingSchedule.stream().anyMatch(instance ->
                    instance.startTime().isBefore(occurrence.end()) && instance.endTime().isAfter(occurrence.start()));
            if (overlapsSchedule) {
                reasons.add("Instructor already has a scheduled session or blocked time overlapping this window");
            }
            if (!availabilityService.isInstructorAvailable(instructorUuid, occurrence.start(), occurrence.end())) {
                reasons.add("Instructor is marked unavailable for this window");
            }
            if (!reasons.isEmpty()) {
                conflicts.add(new ClassSchedulingConflictDTO(occurrence.start(), occurrence.end(), reasons));
            }
        }
        return conflicts;
    }

    /**
     * Expands every session template of the job into concrete occurrence windows
     * using the same expander that class creation uses, so recruitment holds line
     * up one-to-one with the scheduled instances created at assignment.
     */
    private List<OccurrenceWindow> expandJobOccurrences(UUID jobUuid) {
        List<OccurrenceWindow> occurrences = new ArrayList<>();
        for (ClassSessionTemplateDTO template : loadSessionTemplates(jobUuid)) {
            occurrences.addAll(RecurrenceExpander.expand(
                    template.startTime(),
                    template.endTime(),
                    RecurrencePatterns.fromRecurrenceDTO(template.recurrence())));
        }
        return occurrences;
    }

    private void holdJobResources(ClassMarketplaceJob job, List<ClassMarketplaceJobResourceDTO> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        List<OccurrenceWindow> occurrences = expandJobOccurrences(job.getUuid());
        List<ResourceBookingRequest> requests = resources.stream()
                .map(resource -> new ResourceBookingRequest(
                        resource.resourceUuid(),
                        resolveResourceQuantity(resource),
                        occurrences))
                .toList();
        resourceBookingService.holdResourcesForJob(job.getUuid(), job.getOrganisationUuid(), requests);
    }

    private void replaceJobResources(UUID jobUuid, List<ClassMarketplaceJobResourceDTO> resources) {
        jobResourceRepository.deleteByJobUuid(jobUuid);
        if (resources == null || resources.isEmpty()) {
            return;
        }
        List<ClassMarketplaceJobResource> entities = new ArrayList<>();
        for (ClassMarketplaceJobResourceDTO resource : resources) {
            ClassMarketplaceJobResource entity = new ClassMarketplaceJobResource();
            entity.setJobUuid(jobUuid);
            entity.setResourceUuid(resource.resourceUuid());
            entity.setQuantity(resolveResourceQuantity(resource));
            entities.add(entity);
        }
        jobResourceRepository.saveAll(entities);
    }

    private int resolveResourceQuantity(ClassMarketplaceJobResourceDTO resource) {
        return resource.quantity() != null ? resource.quantity() : 1;
    }

    private TimetableService timetableService() {
        TimetableService service = timetableServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("TimetableService is not available");
        }
        return service;
    }

    private boolean isInstructorAdminVerified(UUID instructorUuid) {
        return instructorLookupService.isInstructorAdminVerified(instructorUuid).orElse(false);
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
            ClassMarketplaceJob job = jobRepository.findByUuid(jobUuid).orElse(null);
            for (ClassMarketplaceJobApplication application : toUpdate) {
                notifyApplicantUnsuccessful(job, application,
                        NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_NOT_SELECTED,
                        "was not selected");
            }
        }
    }

    /**
     * Notifies an instructor whose class marketplace job application did not succeed,
     * both in-app and by email. Delivery failures never block the review workflow.
     */
    private void notifyApplicantUnsuccessful(ClassMarketplaceJob job,
                                             ClassMarketplaceJobApplication application,
                                             NotificationType type,
                                             String statusLabel) {
        try {
            if (application.getInstructorUuid() == null) {
                return;
            }
            UUID recipientUserUuid = instructorLookupService
                    .getInstructorUserUuid(application.getInstructorUuid())
                    .orElse(null);
            if (recipientUserUuid == null) {
                return;
            }

            String contextName = job != null && job.getTitle() != null ? job.getTitle() : "the class";
            String reviewNotes = application.getReviewNotes() == null ? "" : application.getReviewNotes();
            UUID jobUuid = job != null ? job.getUuid() : null;

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipientUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    "Your application to train " + contextName + " " + statusLabel + ".",
                    "/dashboard/instructor/applications",
                    Map.of(
                            "job_uuid", jobUuid == null ? "" : jobUuid,
                            "application_uuid", application.getUuid(),
                            "context_name", contextName,
                            "review_notes", reviewNotes
                    ),
                    "class-marketplace-job-application-decision:" + application.getUuid() + ":" + type.getValue()
            ));

            String recipientEmail = userLookupService.getUserEmail(recipientUserUuid).orElse(null);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                return;
            }
            String recipientName = userLookupService.getUserFullName(recipientUserUuid).orElse(recipientEmail);
            eventPublisher.publishEvent(NotificationRequestedEvent.email(
                    recipientUserUuid,
                    recipientEmail,
                    recipientName,
                    type.getValue(),
                    Map.of(
                            "recipientName", recipientName,
                            "contextType", "class",
                            "contextName", contextName,
                            "statusLabel", statusLabel,
                            "reviewNotes", reviewNotes
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to publish unsuccessful-applicant notification for application {}: {}",
                    application.getUuid(), e.getMessage());
        }
    }

    /**
     * Converts the job's recruitment holds into confirmed bookings linked to the
     * scheduled instances the class creation just produced. Holds whose window was
     * not scheduled (skipped or rolled over occurrences) are released.
     */
    private void convertHoldsToConfirmedBookings(ClassMarketplaceJob job, UUID classDefinitionUuid) {
        List<InstanceWindow> instanceWindows = timetableService()
                .getScheduledInstancesForClassDefinition(classDefinitionUuid)
                .stream()
                .map(instance -> new InstanceWindow(instance.uuid(), instance.startTime(), instance.endTime()))
                .toList();
        resourceBookingService.confirmHoldsForJob(job.getUuid(), classDefinitionUuid, instanceWindows);
    }

    private void copyJobResourcesToClassDefinition(UUID jobUuid, UUID classDefinitionUuid) {
        List<ClassMarketplaceJobResource> jobResources = jobResourceRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid);
        if (jobResources.isEmpty()) {
            return;
        }
        List<ClassDefinitionResource> copies = new ArrayList<>();
        for (ClassMarketplaceJobResource jobResource : jobResources) {
            ClassDefinitionResource copy = new ClassDefinitionResource();
            copy.setClassDefinitionUuid(classDefinitionUuid);
            copy.setResourceUuid(jobResource.getResourceUuid());
            copy.setQuantity(jobResource.getQuantity());
            copies.add(copy);
        }
        classDefinitionResourceRepository.saveAll(copies);
    }

    /**
     * The job's venue resource, when one is attached (jobs reserve at most one venue).
     */
    private UUID resolveJobVenueResourceUuid(UUID jobUuid) {
        return jobResourceRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid).stream()
                .map(ClassMarketplaceJobResource::getResourceUuid)
                .filter(resourceUuid -> resourceLookupService.getResource(resourceUuid)
                        .map(summary -> summary.resourceType() == ResourceType.VENUE)
                        .orElse(false))
                .findFirst()
                .orElse(null);
    }

    private ClassDefinitionDTO buildClassDefinitionRequest(ClassMarketplaceJob job, UUID instructorUuid) {
        return new ClassDefinitionDTO(
                null,
                job.getTitle(),
                job.getDescription(),
                instructorUuid,
                job.getOrganisationUuid(),
                job.getCourseUuid(),
                job.getProgramUuid(),
                job.getTrainingFee(),
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
        ).withResourceLinks(resolveJobVenueResourceUuid(job.getUuid()), job.getUuid());
    }

    private List<ClassSessionTemplateDTO> loadSessionTemplates(UUID jobUuid) {
        return sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid)
                .stream()
                .map(this::toSessionTemplateDTO)
                .toList();
    }

    private List<ClassMarketplaceJobResourceDTO> loadJobResources(UUID jobUuid) {
        return jobResourceRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid)
                .stream()
                .map(resource -> new ClassMarketplaceJobResourceDTO(resource.getResourceUuid(), resource.getQuantity()))
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
                job.getProgramUuid(),
                job.getTitle(),
                job.getDescription(),
                job.getTrainingFee(),
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
                loadJobResources(job.getUuid()),
                job.getCreatedDate(),
                job.getLastModifiedDate(),
                job.getCreatedBy(),
                job.getLastModifiedBy()
        );
    }

    private ClassMarketplaceJobApplicationDTO toApplicationDTO(ClassMarketplaceJobApplication application) {
        return toApplicationDTO(application, null);
    }

    private ClassMarketplaceJobApplicationDTO toApplicationDTO(ClassMarketplaceJobApplication application,
                                                               ClassMarketplaceJob job) {
        Boolean instructorAdminVerified = null;
        Boolean trainingApproved = null;
        BigDecimal approvedRate = null;
        if (job != null) {
            instructorAdminVerified = isInstructorAdminVerified(application.getInstructorUuid());
            trainingApproved = isInstructorApprovedForJob(job, application.getInstructorUuid());
            approvedRate = resolveInstructorRateForJob(job, application.getInstructorUuid()).orElse(null);
        }
        return new ClassMarketplaceJobApplicationDTO(
                application.getUuid(),
                application.getJobUuid(),
                application.getInstructorUuid(),
                application.getStatus(),
                application.getApplicationNote(),
                application.getReviewNotes(),
                instructorAdminVerified,
                trainingApproved,
                approvedRate,
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedDate(),
                application.getLastModifiedDate(),
                application.getCreatedBy(),
                application.getLastModifiedBy()
        );
    }

    private Optional<BigDecimal> resolveInstructorRateForJob(ClassMarketplaceJob job, UUID instructorUuid) {
        if (job.getCourseUuid() != null) {
            return courseTrainingApprovalSpi.resolveInstructorRate(
                    job.getCourseUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType());
        }
        return courseTrainingApprovalSpi.resolveInstructorProgramRate(
                job.getProgramUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType());
    }

    private String resolveAssignedReviewNotes(String existingReviewNotes) {
        if (existingReviewNotes == null || existingReviewNotes.isBlank()) {
            return "Application selected for class assignment.";
        }
        return existingReviewNotes;
    }

    private boolean isInstructorApprovedForJob(ClassMarketplaceJob job, UUID instructorUuid) {
        if (job.getCourseUuid() != null) {
            return courseTrainingApprovalSpi.isInstructorApproved(job.getCourseUuid(), instructorUuid);
        }
        return courseTrainingApprovalSpi.isInstructorApprovedForProgram(job.getProgramUuid(), instructorUuid);
    }

    private UUID learningContextUuid(ClassMarketplaceJob job) {
        return job.getCourseUuid() != null ? job.getCourseUuid() : job.getProgramUuid();
    }

    private String learningContextType(ClassMarketplaceJob job) {
        return job.getCourseUuid() != null ? "course" : "training program";
    }
}
