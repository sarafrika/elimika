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
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
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
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.shared.storage.util.MediaOwnerType;
import org.springframework.web.multipart.MultipartFile;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService;
import apps.sarafrika.elimika.tenancy.spi.StudentGroupLookupService;
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
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    /** Where a recruitment notification drops the instructor - their own application list. */
    private static final String INSTRUCTOR_APPLICATIONS_URL = "/dashboard/instructor/opportunities/my-applications";
    /** Where a recruitment notification drops the organisation - the jobs it has posted. */
    private static final String ORGANISATION_JOBS_URL = "/dashboard/organisation/opportunities";

    private static final String JOB_NOT_FOUND_TEMPLATE = "Marketplace class job with UUID %s not found";
    private static final String APPLICATION_NOT_FOUND_TEMPLATE = "Marketplace job application %s not found for job %s";
    private static final int DEFAULT_MAX_PARTICIPANTS = 50;
    private static final String DEFAULT_SCHEDULE_TIMEZONE = "UTC";
    private static final DateTimeFormatter INTERVIEW_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm 'UTC'");

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobApplicationRepository applicationRepository;
    private final ClassMarketplaceJobSessionTemplateRepository sessionTemplateRepository;
    private final ClassMarketplaceJobResourceRepository jobResourceRepository;
    private final ClassDefinitionResourceRepository classDefinitionResourceRepository;
    private final CourseInfoService courseInfoService;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    private final UserLookupService userLookupService;
    private final OrganisationAffiliationService organisationAffiliationService;
    private final StudentGroupLookupService studentGroupLookupService;
    private final InstructorLookupService instructorLookupService;
    private final DomainSecurityService domainSecurityService;
    private final ClassDefinitionServiceInterface classDefinitionService;
    private final ResourceBookingService resourceBookingService;
    private final ResourceLookupService resourceLookupService;
    private final AvailabilityService availabilityService;
    private final ObjectProvider<TimetableService> timetableServiceProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaStorageService mediaStorageService;
    private final MediaValidationService mediaValidationService;
    private final StorageProperties storageProperties;

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

        if (request.preferredInstructorUuid() != null) {
            selectInstructorForJob(saved, request.preferredInstructorUuid());
            saved = jobRepository.save(saved);
            provisionClassForJob(saved);
        }

        return toJobDTO(saved);
    }

    @Override
    public ClassMarketplaceJobDTO uploadJobThumbnail(UUID jobUuid, MultipartFile thumbnail) {
        log.debug("Uploading thumbnail for marketplace class job: {}", jobUuid);
        ClassMarketplaceJob job = jobRepository.findByUuid(jobUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(JOB_NOT_FOUND_TEMPLATE, jobUuid)));
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        mediaValidationService.validate(thumbnail, MediaCategory.THUMBNAIL);
        try {
            String folder = storageProperties.getFolders().getClassThumbnails() + "/jobs/" + jobUuid;
            String key = mediaStorageService.store(new MediaUploadRequest(
                    thumbnail, MediaCategory.THUMBNAIL, folder,
                    MediaOwnerType.JOB_THUMBNAIL, jobUuid, job.getThumbnailUrl())).key();
            job.setThumbnailUrl(key);
            return toJobDTO(jobRepository.save(job));
        } catch (Exception ex) {
            log.error("Failed to upload marketplace job thumbnail for UUID: {}", jobUuid, ex);
            throw new RuntimeException("Failed to upload job thumbnail: " + ex.getMessage(), ex);
        }
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
        ensureJobCancellable(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        releaseAssignedApplication(job);
        job.setStatus(ClassMarketplaceJobStatus.CANCELLED);
        ClassMarketplaceJob saved = jobRepository.save(job);
        resourceBookingService.releaseHoldsForJob(jobUuid, "Job cancelled");
        markOtherApplicationsAsNotSelected(jobUuid, null,
                "This class job was cancelled by the organisation.");
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
        ClassMarketplaceJobApplicationStatus applicationStatus = applicationRepository
                .findByJobUuidAndInstructorUuid(jobUuid, instructorUuid)
                .map(ClassMarketplaceJobApplication::getStatus)
                .orElse(null);
        boolean alreadyApplied = applicationStatus != null;
        boolean canReapply = applicationStatus != null && applicationStatus.allowsReapplication();
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        boolean scheduleClear = scheduleConflicts.isEmpty();
        // An application that is still live blocks a fresh one; a closed one does not.
        boolean blockedByExistingApplication = applicationStatus != null && !applicationStatus.allowsReapplication();
        boolean eligible = instructorVerified && trainingApproved && scheduleClear && !blockedByExistingApplication;

        String reason = null;
        if (!instructorVerified) {
            reason = "Your instructor profile must be verified by an administrator before applying to marketplace class jobs.";
        } else if (!trainingApproved) {
            reason = String.format(
                    "You are not approved to deliver this %s. Submit a training application and wait for approval before applying.",
                    learningContextType(job));
        } else if (blockedByExistingApplication) {
            reason = applicationStatus == ClassMarketplaceJobApplicationStatus.ASSIGNED
                    ? "You have already been assigned to this class job."
                    : "You already have an active application for this class job.";
        } else if (!scheduleClear) {
            reason = String.format(
                    "Your existing schedule conflicts with %d of this job's planned sessions.",
                    scheduleConflicts.size());
        }

        return new ClassMarketplaceJobEligibilityDTO(eligible, instructorVerified, trainingApproved, alreadyApplied,
                applicationStatus, canReapply, scheduleClear, scheduleClear ? null : scheduleConflicts, reason);
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

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        notifyApplicant(job, saved,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_APPROVED, "was approved");
        return toApplicationDTO(saved, job);
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
        notifyApplicant(job, saved,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_REJECTED, "was not successful");
        return toApplicationDTO(saved, job);
    }

    @Override
    public ClassMarketplaceJobApplicationDTO moveApplicationToStage(UUID jobUuid,
                                                                    UUID applicationUuid,
                                                                    ClassMarketplaceJobApplicationStatus targetStage,
                                                                    ClassMarketplaceJobDecisionRequestDTO request) {
        if (targetStage != ClassMarketplaceJobApplicationStatus.SHORTLISTED
                && targetStage != ClassMarketplaceJobApplicationStatus.INTERVIEWING
                && targetStage != ClassMarketplaceJobApplicationStatus.OFFERED) {
            throw new IllegalArgumentException("Stage " + targetStage
                    + " is not a movable recruitment stage. Use approve, reject or assign for final decisions.");
        }

        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureApplicationReviewable(application);
        LocalDateTime interviewAt = resolveInterviewAt(targetStage, request, application);

        application.setStatus(targetStage);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setInterviewAt(interviewAt);
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        notifyApplicant(job, saved, stageNotificationType(targetStage), stageStatusLabel(targetStage));
        return toApplicationDTO(saved, job);
    }

    // Both switches name every movable stage explicitly and throw on anything else. A silent
    // `default` would quietly tell a candidate they had an offer if a new stage were ever added.
    private NotificationType stageNotificationType(ClassMarketplaceJobApplicationStatus stage) {
        return switch (stage) {
            case SHORTLISTED -> NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_SHORTLISTED;
            case INTERVIEWING -> NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_INTERVIEWING;
            case OFFERED -> NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_OFFERED;
            default -> throw new IllegalArgumentException("No notification defined for stage " + stage);
        };
    }

    private String stageStatusLabel(ClassMarketplaceJobApplicationStatus stage) {
        return switch (stage) {
            case SHORTLISTED -> "has been shortlisted";
            case INTERVIEWING -> "has moved to the interview stage";
            case OFFERED -> "has received an offer";
            default -> throw new IllegalArgumentException("No status label defined for stage " + stage);
        };
    }

    @Override
    public ClassMarketplaceJobApplicationDTO withdrawApplication(UUID jobUuid,
                                                                  UUID applicationUuid,
                                                                  ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);

        // Withdrawal belongs to the applicant, not the organisation.
        UUID instructorUuid = resolveCurrentInstructorUuid();
        if (!instructorUuid.equals(application.getInstructorUuid())) {
            throw new AccessDeniedException("You can only withdraw your own application.");
        }

        ensureApplicationWithdrawable(application);

        application.setStatus(ClassMarketplaceJobApplicationStatus.WITHDRAWN);
        application.setReviewNotes(resolveWithdrawalNotes(request));
        application.setReviewedBy(userLookupService.getUserEmail(requireCurrentUserUuid())
                .orElse(instructorUuid.toString()));
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        notifyOrganisationOfWithdrawal(job, saved);
        return toApplicationDTO(saved, job);
    }

    private void ensureApplicationWithdrawable(ClassMarketplaceJobApplication application) {
        ClassMarketplaceJobApplicationStatus status = application.getStatus();
        if (status == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "You have already been assigned to this class job. Contact the organisation to be released from it.");
        }
        if (status == ClassMarketplaceJobApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("You have already withdrawn this application.");
        }
        if (status == ClassMarketplaceJobApplicationStatus.REJECTED
                || status == ClassMarketplaceJobApplicationStatus.NOT_SELECTED) {
            throw new IllegalStateException("This application has already been closed and cannot be withdrawn.");
        }
    }

    private String resolveWithdrawalNotes(ClassMarketplaceJobDecisionRequestDTO request) {
        String reason = request == null ? null : request.reviewNotes();
        return reason == null || reason.isBlank()
                ? "The instructor withdrew this application."
                : reason;
    }

    /**
     * Tells the organisation that an applicant pulled out, so a candidate silently
     * disappearing from the funnel is never a surprise.
     */
    private void notifyOrganisationOfWithdrawal(ClassMarketplaceJob job,
                                                ClassMarketplaceJobApplication application) {
        try {
            UUID creatorUserUuid = job.getCreatedBy() == null
                    ? null
                    : userLookupService.findUserUuidByEmail(job.getCreatedBy()).orElse(null);
            if (creatorUserUuid == null) {
                log.debug("No resolvable creator for marketplace job {}; skipping withdrawal notification",
                        job.getUuid());
                return;
            }

            String jobTitle = job.getTitle() == null ? "your class job" : job.getTitle();
            String instructorName = resolveInstructorDisplayName(application.getInstructorUuid());
            NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_WITHDRAWN;

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    creatorUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    instructorName + " withdrew their application for " + jobTitle + ".",
                    ORGANISATION_JOBS_URL,
                    Map.of(
                            "job_uuid", job.getUuid(),
                            "application_uuid", application.getUuid(),
                            "job_title", jobTitle,
                            "instructor_name", instructorName
                    ),
                    "class-marketplace-job-application-withdrawn:" + application.getUuid()
            ));

            String recipientEmail = userLookupService.getUserEmail(creatorUserUuid).orElse(null);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                return;
            }
            String recipientName = userLookupService.getUserFullName(creatorUserUuid).orElse(recipientEmail);
            eventPublisher.publishEvent(NotificationRequestedEvent.email(
                    creatorUserUuid,
                    recipientEmail,
                    recipientName,
                    type.getValue(),
                    Map.of(
                            "recipientName", recipientName,
                            "instructorName", instructorName,
                            "contextName", jobTitle,
                            "reviewNotes", application.getReviewNotes() == null ? "" : application.getReviewNotes()
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to publish withdrawal notification for application {}: {}",
                    application.getUuid(), e.getMessage());
        }
    }

    private String resolveInstructorDisplayName(UUID instructorUuid) {
        if (instructorUuid == null) {
            return "An instructor";
        }
        return instructorLookupService.getInstructorUserUuid(instructorUuid)
                .flatMap(userLookupService::getUserFullName)
                .orElse("An instructor");
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

        resolveInstructorRateForJob(job, application.getInstructorUuid()).ifPresent(approvedRate -> {
            if (job.getInstructorPay() != null && job.getInstructorPay().compareTo(approvedRate) < 0) {
                throw new IllegalArgumentException(String.format(
                        "This posting offers %s %s, which is below the instructor's approved rate of %s "
                                + "for %s %s sessions.",
                        job.getInstructorPay(),
                        (job.getRateBasis() == null ? RateBasis.PER_HOUR : job.getRateBasis()).getValue(),
                        approvedRate, job.getSessionFormat(), job.getLocationType()));
            }
        });

        selectInstructorForJob(job, application.getInstructorUuid());

        application.setStatus(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        application.setReviewNotes(resolveAssignedReviewNotes(application.getReviewNotes()));
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        ClassMarketplaceJobApplication savedApplication = applicationRepository.save(application);

        job.setAssignedApplicationUuid(application.getUuid());
        ClassMarketplaceJob savedJob = jobRepository.save(job);

        // Hiring is what makes an instructor part of the organisation. Without this they
        // would teach the class but never appear in the organisation's instructor list.
        affiliateAssignedInstructor(savedJob, application.getInstructorUuid());

        markOtherApplicationsAsNotSelected(jobUuid, application.getUuid(),
                "Another instructor was selected for this class job.");

        notifyApplicant(savedJob, savedApplication,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_ASSIGNED, "was successful",
                "/dashboard/instructor/classes");

        return new ClassMarketplaceJobAssignmentResponseDTO(toJobDTO(savedJob));
    }

    /**
     * Attaches the hired instructor to the hiring organisation in the {@code instructor}
     * domain. An instructor who already belongs to the organisation keeps whatever role
     * they hold - a hire must never quietly overwrite an existing affiliation.
     */
    private void affiliateAssignedInstructor(ClassMarketplaceJob job, UUID instructorUuid) {
        if (instructorUuid == null || job.getOrganisationUuid() == null) {
            return;
        }

        UUID instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Instructor %s has no user account, so they cannot be affiliated with the organisation.",
                        instructorUuid)));

        // Marketplace jobs are posted by the organisation rather than a specific branch,
        // so the affiliation is organisation-wide.
        boolean created = organisationAffiliationService.affiliateHiredInstructor(
                instructorUserUuid, job.getOrganisationUuid(), null);

        if (created) {
            log.info("Instructor {} joined organisation {} after being hired for job {}",
                    instructorUuid, job.getOrganisationUuid(), job.getUuid());
        }
    }

    @Override
    public ClassDefinitionDTO createClassForJob(UUID jobUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        if (job.getStatus() != ClassMarketplaceJobStatus.AWAITING_CLASS) {
            throw new IllegalStateException(
                    "A class can only be created once an instructor has been assigned to this job.");
        }

        return provisionClassForJob(job);
    }

    private ClassDefinitionDTO provisionClassForJob(ClassMarketplaceJob job) {
        UUID instructorUuid = job.getAssignedInstructorUuid();
        if (instructorUuid == null) {
            throw new IllegalStateException("This job has no assigned instructor.");
        }

        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        if (!scheduleConflicts.isEmpty()) {
            throw new SchedulingConflictException(String.format(
                    "Instructor %s has schedule conflicts with this job's planned sessions.", instructorUuid),
                    scheduleConflicts);
        }

        ClassDefinitionDTO classDefinition = classDefinitionService
                .createClassDefinition(buildClassDefinitionRequest(job, instructorUuid))
                .classDefinition();

        convertHoldsToConfirmedBookings(job, classDefinition.uuid());
        copyJobResourcesToClassDefinition(job.getUuid(), classDefinition.uuid());

        job.setStatus(ClassMarketplaceJobStatus.FILLED);
        job.setAssignedClassDefinitionUuid(classDefinition.uuid());
        job.setFilledAt(LocalDateTime.now(ZoneOffset.UTC));
        jobRepository.save(job);

        return classDefinition;
    }

    private void selectInstructorForJob(ClassMarketplaceJob job, UUID instructorUuid) {
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        if (!scheduleConflicts.isEmpty()) {
            throw new SchedulingConflictException(String.format(
                    "Instructor %s has schedule conflicts with this job's planned sessions.", instructorUuid),
                    scheduleConflicts);
        }

        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        job.setAssignedInstructorUuid(instructorUuid);
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
        applyJobPricing(job, request);
        job.setServiceType(request.serviceType());
        job.setPreferredInstructorUuid(request.preferredInstructorUuid());
        applyTargetGroups(job, request);
        applyCategory(job, request);
        job.setRemindStudents(request.remindStudents());
        job.setRemindInstructor(request.remindInstructor());
        job.setRemindViaEmail(request.remindViaEmail());
        job.setRemindViaSms(request.remindViaSms());
        job.setRemindViaPush(request.remindViaPush());
    }

    /**
     * Binds the job to the organisation student groups it targets. When group identifiers are
     * supplied they are the source of truth: each is checked against the owning organisation and
     * the matching group names are snapshotted onto {@code target_groups} so adverts keep rendering
     * a label even if a group is later renamed or deleted. Callers that send only free-form labels
     * (older clients) keep working unchanged.
     */
    private void applyTargetGroups(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request) {
        List<UUID> requestedGroups = request.targetGroupUuids();
        if (requestedGroups == null || requestedGroups.isEmpty()) {
            job.setTargetGroupUuids(List.of());
            job.setTargetGroups(request.targetGroups());
            return;
        }

        List<UUID> ownedGroups = studentGroupLookupService.filterGroupsInOrganisation(request.organisationUuid(), requestedGroups);
        if (ownedGroups.size() != new HashSet<>(requestedGroups).size()) {
            throw new IllegalArgumentException("One or more target groups do not belong to organisation " + request.organisationUuid());
        }

        job.setTargetGroupUuids(ownedGroups);
        job.setTargetGroups(studentGroupLookupService.getGroupNames(ownedGroups));
    }

    /**
     * Records the category a class falls under. Course-backed classes inherit the course's own
     * categories, so nothing is stored for them; program-backed classes carry the organisation's
     * choice. An unknown category is rejected rather than silently dropped.
     */
    private void applyCategory(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request) {
        UUID categoryUuid = request.categoryUuid();
        if (categoryUuid != null && !courseInfoService.categoryExists(categoryUuid)) {
            throw new IllegalArgumentException("Category " + categoryUuid + " does not exist");
        }
        job.setCategoryUuid(categoryUuid);
    }

    /**
     * A posting declares two numbers: what a learner will be charged, and what the eventual
     * instructor will earn. The organisation does not know who it will hire when it posts, which is
     * why the pay is declared up front rather than derived from an applicant. The difference between
     * them is the organisation's margin.
     */
    private void applyJobPricing(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request) {
        BigDecimal approvedRate = resolveOrganisationRateForRequest(request)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "No approved training rate for organisation %s on this %s for %s %s sessions charged %s. "
                                + "The course creator must approve a rate card before a class can be posted.",
                        request.organisationUuid(),
                        request.courseUuid() != null ? "course" : "training program",
                        request.sessionFormat(),
                        request.locationType(),
                        (request.rateBasis() == null ? RateBasis.PER_HOUR : request.rateBasis()).getValue())));

        BigDecimal salePrice = request.salePrice() != null ? request.salePrice() : approvedRate;
        BigDecimal instructorPay = request.instructorPay() != null ? request.instructorPay() : salePrice;

        if (instructorPay.compareTo(salePrice) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Instructor pay %s cannot exceed the sale price %s.", instructorPay, salePrice));
        }

        BigDecimal minimumTrainingFee = resolveMinimumTrainingFeeForRequest(request);
        if (minimumTrainingFee != null && salePrice.compareTo(minimumTrainingFee) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Sale price %s cannot be less than the course minimum training fee %s.",
                    salePrice, minimumTrainingFee));
        }

        job.setSalePrice(salePrice);
        job.setInstructorPay(instructorPay);
        job.setRateBasis(request.rateBasis() != null
                ? request.rateBasis()
                : apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR);
    }

    private BigDecimal resolveMinimumTrainingFeeForRequest(ClassMarketplaceJobRequestDTO request) {
        if (request.courseUuid() != null) {
            return courseInfoService.getMinimumTrainingFee(request.courseUuid()).orElse(null);
        }
        return courseInfoService.getProgramMinimumTrainingFee(request.programUuid()).orElse(null);
    }

    private Optional<BigDecimal> resolveOrganisationRateForRequest(ClassMarketplaceJobRequestDTO request) {
        RateBasis basis = request.rateBasis() == null ? RateBasis.PER_HOUR : request.rateBasis();
        if (request.courseUuid() != null) {
            return courseTrainingApprovalSpi.resolveOrganisationRate(
                    request.courseUuid(), request.organisationUuid(), request.sessionFormat(), request.locationType(),
                    basis);
        }
        return courseTrainingApprovalSpi.resolveOrganisationProgramRate(
                request.programUuid(), request.organisationUuid(), request.sessionFormat(), request.locationType(),
                basis);
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
            template.setTimezone(normalizeTimezone(templateDTO.timezone()));
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
        if (existing.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
            throw new IllegalStateException("You have already been assigned to this marketplace job.");
        }
        // Reapplying while still in the funnel would silently reset the instructor's
        // stage - including a shortlisting or an offer - back to PENDING.
        if (existing.getStatus().isActive()) {
            throw new IllegalStateException("You already have an active application for this marketplace job.");
        }

        existing.setStatus(ClassMarketplaceJobApplicationStatus.PENDING);
        existing.setApplicationNote(request == null ? null : request.applicationNote());
        existing.setReviewNotes(null);
        existing.setInterviewAt(null);
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

    private void ensureJobCancellable(ClassMarketplaceJob job) {
        if (job.getStatus() != ClassMarketplaceJobStatus.OPEN
                && job.getStatus() != ClassMarketplaceJobStatus.AWAITING_CLASS) {
            throw new IllegalStateException("This marketplace class job can no longer be cancelled.");
        }
    }

    private void ensureApplicationReviewable(ClassMarketplaceJobApplication application) {
        if (application.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
            throw new IllegalStateException("Assigned applications cannot be reviewed again.");
        }
        if (application.getStatus() == ClassMarketplaceJobApplicationStatus.NOT_SELECTED) {
            throw new IllegalStateException("Applications already marked as not selected cannot be reviewed again.");
        }
        if (application.getStatus() == ClassMarketplaceJobApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("This application was withdrawn by the instructor and cannot be reviewed.");
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

    private void releaseAssignedApplication(ClassMarketplaceJob job) {
        UUID assignedApplicationUuid = job.getAssignedApplicationUuid();
        if (assignedApplicationUuid == null) {
            job.setAssignedInstructorUuid(null);
            return;
        }

        applicationRepository.findByJobUuidAndUuid(job.getUuid(), assignedApplicationUuid)
                .filter(application -> application.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED)
                .ifPresent(application -> {
                    application.setStatus(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
                    application.setReviewNotes("This class job was cancelled before its class was created.");
                    application.setReviewedBy(resolveReviewer());
                    application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
                    ClassMarketplaceJobApplication saved = applicationRepository.save(application);
                    // They had already been told they were hired - they must be told it is off.
                    notifyApplicant(job, saved,
                            NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_CANCELLED,
                            "was cancelled by the organisation");
                });

        job.setAssignedApplicationUuid(null);
        job.setAssignedInstructorUuid(null);
    }

    private void markOtherApplicationsAsNotSelected(UUID jobUuid, UUID selectedApplicationUuid, String closureReason) {
        // Everyone still moving through the funnel must be closed out, not just those at
        // the two ends of it - otherwise shortlisted and interviewing candidates wait forever.
        List<ClassMarketplaceJobApplication> openApplications = applicationRepository.findByJobUuidAndStatusIn(
                jobUuid,
                List.of(
                        ClassMarketplaceJobApplicationStatus.PENDING,
                        ClassMarketplaceJobApplicationStatus.SHORTLISTED,
                        ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                        ClassMarketplaceJobApplicationStatus.OFFERED,
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
                application.setReviewNotes(closureReason);
            }
            application.setReviewedBy(resolveReviewer());
            application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
            toUpdate.add(application);
        }

        if (!toUpdate.isEmpty()) {
            applicationRepository.saveAll(toUpdate);
            ClassMarketplaceJob job = jobRepository.findByUuid(jobUuid).orElse(null);
            for (ClassMarketplaceJobApplication application : toUpdate) {
                notifyApplicant(job, application,
                        NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_NOT_SELECTED,
                        "was not selected");
            }
        }
    }

    /**
     * Notifies an instructor that their class marketplace job application moved to a new
     * stage, both in-app and by email. Used for every transition the organisation drives -
     * shortlisting, interviewing, offers, approval, the hire itself and unsuccessful
     * outcomes. Delivery failures never block the review workflow.
     */
    private void notifyApplicant(ClassMarketplaceJob job,
                                 ClassMarketplaceJobApplication application,
                                 NotificationType type,
                                 String statusLabel) {
        notifyApplicant(job, application, type, statusLabel, INSTRUCTOR_APPLICATIONS_URL);
    }

    private void notifyApplicant(ClassMarketplaceJob job,
                                 ClassMarketplaceJobApplication application,
                                 NotificationType type,
                                 String statusLabel,
                                 String actionUrl) {
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
            String interviewAt = formatInterviewAt(application.getInterviewAt());
            String interviewSuffix = interviewAt.isBlank()
                    ? ""
                    : " Interview scheduled for " + interviewAt + ".";
            UUID jobUuid = job != null ? job.getUuid() : null;

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipientUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    "Your application to train " + contextName + " " + statusLabel + "."
                            + interviewSuffix,
                    actionUrl,
                    Map.of(
                            "job_uuid", jobUuid == null ? "" : jobUuid,
                            "application_uuid", application.getUuid(),
                            "context_name", contextName,
                            "review_notes", reviewNotes,
                            "interview_at", interviewAt
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
                            "reviewNotes", reviewNotes,
                            "interviewAt", interviewAt,
                            "interview_at", interviewAt
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to publish applicant notification for application {}: {}",
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
                job.getSalePrice(),
                job.getInstructorPay(),
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
                Boolean.TRUE,
                loadSessionTemplates(job.getUuid()),
                null,
                null,
                null,
                null
        ).withCategory(job.getCategoryUuid())
                .withResourceLinks(resolveJobVenueResourceUuid(job.getUuid()), job.getUuid());
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
                entity.getTimezone(),
                apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy.valueOf(
                        Optional.ofNullable(entity.getConflictResolution()).orElse("FAIL")
                )
        );
    }

    private String normalizeTimezone(String timezone) {
        String value = timezone == null || timezone.isBlank()
                ? DEFAULT_SCHEDULE_TIMEZONE
                : timezone.trim();
        try {
            ZoneId.of(value);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid IANA timezone: " + value, ex);
        }
        return value;
    }

    private ClassMarketplaceJobDTO toJobDTO(ClassMarketplaceJob job) {
        return new ClassMarketplaceJobDTO(
                job.getUuid(),
                job.getOrganisationUuid(),
                job.getCourseUuid(),
                job.getProgramUuid(),
                job.getTitle(),
                job.getDescription(),
                job.getSalePrice(),
                job.getInstructorPay(),
                job.getRateBasis(),
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
                FileUrlResolver.publicUrl(job.getThumbnailUrl()),
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
                job.getLastModifiedBy(),
                job.getServiceType(),
                job.getPreferredInstructorUuid(),
                job.getTargetGroups(),
                job.getTargetGroupUuids(),
                job.getCategoryUuid(),
                job.getRemindStudents(),
                job.getRemindInstructor(),
                job.getRemindViaEmail(),
                job.getRemindViaSms(),
                job.getRemindViaPush()
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
                application.getInterviewAt(),
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
        RateBasis basis = job.getRateBasis() == null ? RateBasis.PER_HOUR : job.getRateBasis();
        if (job.getCourseUuid() != null) {
            return courseTrainingApprovalSpi.resolveInstructorRate(
                    job.getCourseUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType(), basis);
        }
        return courseTrainingApprovalSpi.resolveInstructorProgramRate(
                job.getProgramUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType(), basis);
    }

    private String resolveAssignedReviewNotes(String existingReviewNotes) {
        if (existingReviewNotes == null || existingReviewNotes.isBlank()) {
            return "Application selected for class assignment.";
        }
        return existingReviewNotes;
    }

    private LocalDateTime resolveInterviewAt(ClassMarketplaceJobApplicationStatus targetStage,
                                             ClassMarketplaceJobDecisionRequestDTO request,
                                             ClassMarketplaceJobApplication application) {
        if (targetStage != ClassMarketplaceJobApplicationStatus.INTERVIEWING) {
            return application.getInterviewAt();
        }
        if (request == null || request.interviewAt() == null) {
            throw new IllegalArgumentException("interview_at is required when moving an application to interview.");
        }
        return request.interviewAt();
    }

    private String formatInterviewAt(LocalDateTime interviewAt) {
        if (interviewAt == null) {
            return "";
        }
        return INTERVIEW_DATE_FORMATTER.format(interviewAt);
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
