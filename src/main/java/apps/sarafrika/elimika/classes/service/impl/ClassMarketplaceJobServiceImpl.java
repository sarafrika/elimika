package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationEventDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
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
import apps.sarafrika.elimika.classes.internal.AuditUserResolver;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver.ResolvedLocation;
import apps.sarafrika.elimika.classes.internal.MarketplaceApplicationHistory;
import apps.sarafrika.elimika.classes.internal.MarketplaceHireClashNotifier;
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
import apps.sarafrika.elimika.classes.util.RateWording;
import apps.sarafrika.elimika.classes.util.RecurrencePatterns;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationEventType;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingRequest;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceSummary;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceExpander;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldRequest;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.spi.InstructorTrainingApprovals;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.shared.storage.util.MediaOwnerType;
import org.springframework.web.multipart.MultipartFile;
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
import org.springframework.data.domain.Pageable;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final int MAX_ELIGIBILITY_BATCH = 50;
    private static final String CACHE_PAY_VISIBLE = "marketplaceJob.payVisible";
    private static final String CACHE_BRANCH_NAME_PREFIX = "marketplaceJob.branchName.";
    private static final String CACHE_RESOURCE_PREFIX = "marketplaceJob.resource.";
    /** Derived from the enum so a new recruitment stage is covered without editing this list. */
    private static final List<ClassMarketplaceJobApplicationStatus> ACTIVE_APPLICATION_STATUSES =
            Arrays.stream(ClassMarketplaceJobApplicationStatus.values())
                    .filter(ClassMarketplaceJobApplicationStatus::isActive)
                    .toList();

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
    private final RequestScopedCache requestScopedCache;
    private final ClassDefinitionServiceInterface classDefinitionService;
    private final ResourceBookingService resourceBookingService;
    private final InstructorTimeHoldService instructorTimeHoldService;
    private final ResourceLookupService resourceLookupService;
    private final AvailabilityService availabilityService;
    private final ObjectProvider<TimetableService> timetableServiceProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaStorageService mediaStorageService;
    private final MediaValidationService mediaValidationService;
    private final StorageProperties storageProperties;
    private final BranchLocationResolver branchLocationResolver;
    private final MarketplaceHireClashNotifier hireClashNotifier;
    private final AuditUserResolver auditUserResolver;
    private final MarketplaceApplicationHistory applicationHistory;

    @Override
    public ClassMarketplaceJobDTO createJob(ClassMarketplaceJobRequestDTO request) {
        requireOrganisationManagerAccess(request.organisationUuid());
        ResolvedLocation location = validateJobDraft(request);

        ClassMarketplaceJob job = new ClassMarketplaceJob();
        applyJobDraft(job, request, location);
        job.setStatus(ClassMarketplaceJobStatus.OPEN);

        ClassMarketplaceJob saved = jobRepository.save(job);
        replaceSessionTemplates(saved.getUuid(), request.sessionTemplates());
        replaceJobResources(saved.getUuid(), request.resources());
        holdJobResources(saved, request.resources());

        if (request.preferredInstructorUuid() != null) {
            hireInstructorForJob(saved, request.preferredInstructorUuid());
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

        ResolvedLocation location = validateJobDraft(request);
        applyJobDraft(job, request, location);
        ClassMarketplaceJob saved = jobRepository.save(job);
        replaceSessionTemplates(saved.getUuid(), request.sessionTemplates());
        resourceBookingService.releaseHoldsForJob(jobUuid, "Job updated; holds re-evaluated");
        instructorTimeHoldService.releaseHoldsForJob(jobUuid, "Job updated; holds re-evaluated");
        replaceJobResources(saved.getUuid(), request.resources());
        holdJobResources(saved, request.resources());
        rebuildInstructorHoldsForActiveApplications(saved);

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
                                                 UUID branchUuid,
                                                 ClassMarketplaceJobStatus status,
                                                 org.springframework.data.domain.Pageable pageable) {
        Page<ClassMarketplaceJob> jobs =
                jobRepository.search(organisationUuid, courseUuid, programUuid, branchUuid, status, pageable);
        JobReadContext context = loadJobReadContext(jobs.getContent());
        return jobs.map(job -> toJobDTO(job, context));
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
        instructorTimeHoldService.releaseHoldsForJob(jobUuid, "Job cancelled");
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

        Optional<ClassMarketplaceJobApplication> existing =
                applicationRepository.findByJobUuidAndInstructorUuid(jobUuid, instructorUuid);
        ClassMarketplaceJobApplication application = existing
                .map(closed -> reopenApplication(closed, request))
                .orElseGet(() -> createApplication(jobUuid, instructorUuid, request));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        applicationHistory.record(saved, existing.isPresent()
                        ? ClassMarketplaceJobApplicationEventType.REAPPLIED
                        : ClassMarketplaceJobApplicationEventType.APPLIED,
                saved.getApplicationNote(), null);
        // Applying pencils the job's sessions into the instructor's diary. A reopened
        // application replaces its old holds, so re-applying never doubles them up.
        holdInstructorTimeForApplication(job, saved);
        return toApplicationDTO(saved, job);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassMarketplaceJobEligibilityDTO getMyJobEligibility(UUID jobUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        UUID instructorUuid = resolveCurrentInstructorUuid();
        return assessEligibility(List.of(job), instructorUuid).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassMarketplaceJobEligibilityDTO> getMyJobsEligibility(Collection<UUID> jobUuids) {
        List<UUID> requested = jobUuids == null ? List.of() : jobUuids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requested.size() > MAX_ELIGIBILITY_BATCH) {
            throw new IllegalArgumentException(String.format(
                    "At most %d job_uuids can be checked in one call; %d were sent.",
                    MAX_ELIGIBILITY_BATCH, requested.size()));
        }
        UUID instructorUuid = resolveCurrentInstructorUuid();
        if (requested.isEmpty()) {
            return List.of();
        }

        Map<UUID, ClassMarketplaceJob> jobsByUuid = new HashMap<>();
        jobRepository.findByUuidIn(requested).forEach(job -> jobsByUuid.put(job.getUuid(), job));
        List<ClassMarketplaceJob> jobs = requested.stream()
                .map(jobsByUuid::get)
                .filter(Objects::nonNull)
                .toList();
        return assessEligibility(jobs, instructorUuid);
    }

    // The one eligibility path: the instructor's facts load once for all the jobs; only the clash check runs per job.
    private List<ClassMarketplaceJobEligibilityDTO> assessEligibility(List<ClassMarketplaceJob> jobs, UUID instructorUuid) {
        if (jobs.isEmpty()) {
            return List.of();
        }
        boolean instructorVerified = isInstructorAdminVerified(instructorUuid);
        InstructorTrainingApprovals approvals = courseTrainingApprovalSpi.findInstructorApprovals(instructorUuid);
        Map<UUID, ClassMarketplaceJobApplicationStatus> applicationStatuses = new HashMap<>();
        applicationRepository.findByInstructorUuidAndJobUuidIn(instructorUuid,
                        jobs.stream().map(ClassMarketplaceJob::getUuid).toList())
                .forEach(application -> applicationStatuses.put(application.getJobUuid(), application.getStatus()));
        return jobs.stream()
                .map(job -> assessEligibility(job, instructorUuid, instructorVerified, approvals,
                        applicationStatuses.get(job.getUuid())))
                .toList();
    }

    private ClassMarketplaceJobEligibilityDTO assessEligibility(ClassMarketplaceJob job,
                                                               UUID instructorUuid,
                                                               boolean instructorVerified,
                                                               InstructorTrainingApprovals approvals,
                                                               ClassMarketplaceJobApplicationStatus applicationStatus) {
        boolean trainingApproved = job.getCourseUuid() != null
                ? approvals.approvedForCourse(job.getCourseUuid())
                : approvals.approvedForProgram(job.getProgramUuid());
        Optional<BigDecimal> approvedRate = job.getCourseUuid() != null
                ? approvals.courseRate(job.getCourseUuid(), job.getSessionFormat(), job.getLocationType(), job.getRateBasis())
                : approvals.programRate(job.getProgramUuid(), job.getSessionFormat(), job.getLocationType(), job.getRateBasis());
        InstructorRateCheck rateCheck = new InstructorRateCheck(pricedRate(approvedRate).orElse(null), job.getInstructorPay());
        boolean alreadyApplied = applicationStatus != null;
        boolean canReapply = applicationStatus != null && applicationStatus.allowsReapplication();
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        boolean scheduleClear = scheduleConflicts.isEmpty();
        // An application that is still live blocks a fresh one; a closed one does not.
        boolean blockedByExistingApplication = applicationStatus != null && !applicationStatus.allowsReapplication();
        boolean eligible = instructorVerified && trainingApproved && rateCheck.payCoversRate()
                && scheduleClear && !blockedByExistingApplication;

        String reason = null;
        if (!instructorVerified) {
            reason = "Your instructor profile must be verified by an administrator before applying to marketplace class jobs.";
        } else if (!trainingApproved) {
            reason = String.format(
                    "You are not approved to deliver this %s. Submit a training application and wait for approval before applying.",
                    learningContextType(job));
        } else if (!rateCheck.payCoversRate()) {
            reason = instructorRateRefusal(job, rateCheck);
        } else if (blockedByExistingApplication) {
            reason = applicationStatus == ClassMarketplaceJobApplicationStatus.ASSIGNED
                    ? "You have already been assigned to this class job."
                    : "You already have an active application for this class job.";
        } else if (!scheduleClear) {
            reason = String.format(
                    "Your existing schedule conflicts with %d of this job's planned sessions.",
                    scheduleConflicts.size());
        }

        return new ClassMarketplaceJobEligibilityDTO(job.getUuid(), eligible, instructorVerified, trainingApproved,
                rateCheck.payCoversRate(), rateCheck.approvedRate(), alreadyApplied, applicationStatus, canReapply,
                scheduleClear, scheduleClear ? null : scheduleConflicts, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listJobApplications(UUID jobUuid,
                                                                       ClassMarketplaceJobApplicationStatus status,
                                                                       org.springframework.data.domain.Pageable pageable) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        requireOrganisationApplicationReadAccess(job.getOrganisationUuid());
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
        return findInstructorApplications(instructorUuid, status, pageable).map(this::toApplicationDTO);
    }

    /**
     * An instructor's applications carry review notes and rate figures, so who is asking decides
     * what comes back. The instructor sees all of their own and so does a platform admin, which is
     * what the admin console's user dossier reads. An organisation manager sees only the ones made
     * to their own organisations' jobs — the same rows {@link #listJobApplications} would already
     * give them, job by job. Anyone else gets an empty page rather than a refusal, so the route
     * confirms nothing about an instructor it will not show.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClassMarketplaceJobApplicationDTO> listInstructorApplications(UUID instructorUuid,
                                                                              ClassMarketplaceJobApplicationStatus status,
                                                                              org.springframework.data.domain.Pageable pageable) {
        UUID currentUserUuid = requireCurrentUserUuid();
        if (domainSecurityService.isInstructorWithUuid(instructorUuid) || domainSecurityService.isPlatformAdmin()) {
            return findInstructorApplications(instructorUuid, status, pageable).map(this::toApplicationDTO);
        }

        // Resolving what the caller manages first means a signed-in stranger — the common case on a
        // route addressed by instructor uuid — costs one membership lookup and never touches the
        // applications table.
        List<UUID> managedOrganisations = organisationsManagedBy(currentUserUuid);
        if (managedOrganisations.isEmpty()) {
            return Page.empty(pageable);
        }
        return applicationRepository
                .findByInstructorUuidAndJobOrganisations(instructorUuid, status, managedOrganisations, pageable)
                .map(this::toApplicationDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassMarketplaceJobApplicationDTO getJobApplication(UUID jobUuid, UUID applicationUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        requireApplicationReadAccess(job, application);
        return toApplicationDTO(application, job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassMarketplaceJobApplicationEventDTO> listApplicationEvents(UUID jobUuid, UUID applicationUuid) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        requireApplicationReadAccess(job, application);
        return applicationHistory.history(application.getUuid());
    }

    /** One application is the business of its applicant, the posting organisation's managers and platform admins. */
    private void requireApplicationReadAccess(ClassMarketplaceJob job, ClassMarketplaceJobApplication application) {
        requireCurrentUserUuid();
        if (domainSecurityService.isInstructorWithUuid(application.getInstructorUuid())
                || domainSecurityService.isPlatformAdmin()
                || domainSecurityService.managesOrganisation(job.getOrganisationUuid())) {
            return;
        }
        throw new AccessDeniedException(
                "You can only read your own applications, or applications to your organisation's jobs.");
    }

    private Page<ClassMarketplaceJobApplication> findInstructorApplications(UUID instructorUuid,
                                                                            ClassMarketplaceJobApplicationStatus status,
                                                                            Pageable pageable) {
        if (status == null) {
            return applicationRepository.findByInstructorUuidOrderByCreatedDateDesc(instructorUuid, pageable);
        }
        return applicationRepository.findByInstructorUuidAndStatusOrderByCreatedDateDesc(instructorUuid, status, pageable);
    }

    /**
     * The organisations the given user may act for, drawn from their own memberships so the answer
     * is bounded by who they are rather than by how many organisations exist.
     */
    private List<UUID> organisationsManagedBy(UUID userUuid) {
        return userLookupService.getUserOrganizations(userUuid).stream()
                .filter(domainSecurityService::managesOrganisation)
                .toList();
    }

    @Override
    public ClassMarketplaceJobApplicationDTO hireApplication(UUID jobUuid,
                                                             UUID applicationUuid,
                                                             ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureTransitionAllowed(application, ClassMarketplaceJobApplicationStatus.HIRED);
        ensureInstructorApprovedToDeliver(job, application.getInstructorUuid());
        refuseUncoveredInstructorRate(job, application.getInstructorUuid());
        // The diary can fill up after applying (another hire, a booked session, a blocked day),
        // so the clash is re-checked here, before anything about the hire is written.
        refuseClashingHire(job, application.getUuid(), application.getInstructorUuid());

        application.setStatus(ClassMarketplaceJobApplicationStatus.HIRED);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        // The hire is what makes the instructor a member, so it affiliates here rather than at
        // assignment - an organisation that hires and never assigns still gained an instructor.
        affiliateHiredInstructor(job, application.getInstructorUuid());

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        applicationHistory.record(saved, ClassMarketplaceJobApplicationEventType.HIRED, saved.getReviewNotes(), null);

        // The hire is the last decision, so nothing about it stays implicit: the job records
        // which application won and is left waiting only for its class to be created.
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        job.setAssignedApplicationUuid(saved.getUuid());
        ClassMarketplaceJob savedJob = jobRepository.save(job);

        // A hire commits the instructor's time now, so their calendar shows it as blocked
        // before the class exists; nobody else can be hired for this job.
        firmInstructorTimeForApplication(savedJob, saved);
        instructorTimeHoldService.releaseHoldsForJobExcept(jobUuid, saved.getUuid(),
                "Another instructor was hired");

        notifyApplicant(savedJob, saved,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_HIRED,
                "was successful - you have been hired");
        return toApplicationDTO(saved, savedJob);
    }

    private void ensureInstructorApprovedToDeliver(ClassMarketplaceJob job, UUID instructorUuid) {
        if (isInstructorApprovedForJob(job, instructorUuid)) {
            return;
        }
        throw new IllegalStateException(String.format(
                "Instructor %s is not approved to deliver %s %s. Only instructors with approved %s delivery access can be hired for this job.",
                instructorUuid,
                learningContextType(job),
                learningContextUuid(job),
                learningContextType(job)));
    }

    @Override
    public ClassMarketplaceJobApplicationDTO rejectApplication(UUID jobUuid,
                                                               UUID applicationUuid,
                                                               ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureTransitionAllowed(application, ClassMarketplaceJobApplicationStatus.REJECTED);

        application.setStatus(ClassMarketplaceJobApplicationStatus.REJECTED);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        applicationHistory.record(saved, ClassMarketplaceJobApplicationEventType.REJECTED, saved.getReviewNotes(), null);
        instructorTimeHoldService.releaseHoldsForApplication(saved.getUuid(), "Application rejected");
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
                    + " is not a movable recruitment stage. Use hire or reject for final decisions.");
        }

        ClassMarketplaceJob job = getJobEntity(jobUuid);
        ensureJobOpen(job);
        requireOrganisationManagerAccess(job.getOrganisationUuid());

        ClassMarketplaceJobApplication application = getApplication(jobUuid, applicationUuid);
        ensureTransitionAllowed(application, targetStage);
        LocalDateTime interviewAt = resolveInterviewAt(targetStage, request, application);

        application.setStatus(targetStage);
        application.setReviewNotes(request == null ? null : request.reviewNotes());
        application.setInterviewAt(interviewAt);
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ClassMarketplaceJobApplication saved = applicationRepository.save(application);
        applicationHistory.record(saved, ClassMarketplaceJobApplicationEventType.forReviewStage(targetStage),
                saved.getReviewNotes(),
                targetStage == ClassMarketplaceJobApplicationStatus.INTERVIEWING ? saved.getInterviewAt() : null);
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
        applicationHistory.record(saved, ClassMarketplaceJobApplicationEventType.WITHDRAWN,
                request == null ? null : request.reviewNotes(), null);
        instructorTimeHoldService.releaseHoldsForApplication(saved.getUuid(), "Instructor withdrew");
        releaseWithdrawnHire(job, saved);
        notifyOrganisationOfWithdrawal(job, saved);
        return toApplicationDTO(saved, job);
    }

    /** A hire that pulls out leaves the job holding nobody, so the posting goes back on the market
     * rather than waiting forever on a class its withdrawn applicant will never teach. */
    private void releaseWithdrawnHire(ClassMarketplaceJob job, ClassMarketplaceJobApplication application) {
        if (!application.getUuid().equals(job.getAssignedApplicationUuid())) {
            return;
        }

        job.setAssignedApplicationUuid(null);
        job.setAssignedInstructorUuid(null);
        // A job that lapsed or was cancelled keeps the status it closed at; only one still waiting
        // on this hire's class has anywhere to go back to.
        if (job.getStatus() == ClassMarketplaceJobStatus.AWAITING_CLASS) {
            job.setStatus(ClassMarketplaceJobStatus.OPEN);
        }
        jobRepository.save(job);

        log.info("Marketplace job {} lost its hire: instructor {} withdrew before its class was created",
                job.getUuid(), application.getInstructorUuid());
    }

    private void ensureApplicationWithdrawable(ClassMarketplaceJobApplication application) {
        ClassMarketplaceJobApplicationStatus status = application.getStatus();
        // Withdrawal is an exit, so the enum opens it from every live stage; only the wording
        // of the refusal is the applicant's business rather than the funnel's.
        if (ClassMarketplaceJobApplicationStatus.WITHDRAWN.isReachableFrom(status)) {
            return;
        }
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
            UUID creatorUserUuid = auditUserResolver.userOf(job.getCreatedBy()).orElse(null);
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
        return resolveInstructorName(instructorUuid, "An instructor");
    }

    private String resolveInstructorName(UUID instructorUuid, String fallback) {
        if (instructorUuid == null) {
            return fallback;
        }
        return instructorLookupService.getInstructorUserUuid(instructorUuid)
                .flatMap(userLookupService::getUserFullName)
                .orElse(fallback);
    }

    /**
     * Performs the assignment the class creation stands for: the hired applicant becomes the job's
     * instructor, their diary claim turns firm, everyone else is closed out. Private on purpose -
     * assigning is no longer a decision anybody takes, only a consequence of creating the class.
     */
    private void assignHiredInstructor(ClassMarketplaceJob job) {
        UUID hiredApplicationUuid = job.getAssignedApplicationUuid();
        if (hiredApplicationUuid == null) {
            throw new IllegalStateException("This job holds no hire, so there is nobody to assign to its class.");
        }

        UUID jobUuid = job.getUuid();
        ClassMarketplaceJobApplication application = getApplication(jobUuid, hiredApplicationUuid);
        if (application.getStatus() != ClassMarketplaceJobApplicationStatus.HIRED) {
            throw new IllegalStateException(String.format(
                    "The application this job hired is at the %s stage, so its class cannot be created.",
                    application.getStatus().getValue()));
        }

        if (!isInstructorApprovedForJob(job, application.getInstructorUuid())) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is no longer approved to deliver %s %s.",
                    application.getInstructorUuid(),
                    learningContextType(job),
                    learningContextUuid(job)));
        }

        refuseUncoveredInstructorRate(job, application.getInstructorUuid());

        selectInstructorForJob(job, application.getInstructorUuid());

        application.setStatus(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        application.setReviewNotes(resolveAssignedReviewNotes(application.getReviewNotes()));
        application.setReviewedBy(resolveReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        ClassMarketplaceJobApplication savedApplication = applicationRepository.save(application);
        applicationHistory.record(savedApplication, ClassMarketplaceJobApplicationEventType.ASSIGNED, null, null);

        ClassMarketplaceJob savedJob = jobRepository.save(job);

        // The hire is what makes the claim exclusive: the hired diary is reserved and every
        // other applicant gets theirs back, mirroring markOtherApplicationsAsNotSelected.
        firmInstructorTimeForApplication(savedJob, savedApplication);
        instructorTimeHoldService.releaseHoldsForJobExcept(jobUuid, application.getUuid(),
                "Another instructor was hired");

        markOtherApplicationsAsNotSelected(jobUuid, application.getUuid(),
                "Another instructor was selected for this class job.");

        notifyApplicant(savedJob, savedApplication,
                NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_ASSIGNED,
                "has produced your class - it is on your timetable",
                "/dashboard/instructor/classes");
    }

    /**
     * Hires the instructor a job was posted for by name: no application exists to walk through the
     * funnel, so the post and the affiliation happen together and the class follows immediately.
     */
    private void hireInstructorForJob(ClassMarketplaceJob job, UUID instructorUuid) {
        refuseUncoveredInstructorRate(job, instructorUuid);
        refuseClashingHire(job, null, instructorUuid);
        recordSelectedInstructor(job, instructorUuid);
        affiliateHiredInstructor(job, instructorUuid);
    }

    /** Refuses a hire whose sessions clash with the instructor's diary; nothing is written first. */
    private void refuseClashingHire(ClassMarketplaceJob job, UUID applicationUuid, UUID instructorUuid) {
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        if (scheduleConflicts.isEmpty()) {
            return;
        }
        alertBlockedHire(job, applicationUuid, instructorUuid, scheduleConflicts);
        throw new SchedulingConflictException(String.format(
                "Instructor %s cannot be hired: their schedule clashes with %d of this job's planned sessions.",
                instructorUuid, scheduleConflicts.size()),
                scheduleConflicts);
    }

    private void alertBlockedHire(ClassMarketplaceJob job,
                                  UUID applicationUuid,
                                  UUID instructorUuid,
                                  List<ClassSchedulingConflictDTO> scheduleConflicts) {
        try {
            hireClashNotifier.notifyHireBlocked(job, applicationUuid, instructorUuid,
                    domainSecurityService.getCurrentUserUuid(), scheduleConflicts);
        } catch (RuntimeException e) {
            // An alert that fails to send must not turn the refusal into a server error.
            log.warn("Failed to alert the parties to the blocked hire on marketplace job {}: {}",
                    job.getUuid(), e.getMessage());
        }
    }

    /**
     * Attaches the hired instructor to the hiring organisation in the {@code instructor}
     * domain. An instructor who already belongs to the organisation keeps whatever role
     * they hold - a hire must never quietly overwrite an existing affiliation.
     */
    private void affiliateHiredInstructor(ClassMarketplaceJob job, UUID instructorUuid) {
        if (instructorUuid == null || job.getOrganisationUuid() == null) {
            return;
        }

        UUID instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Instructor %s has no user account, so they cannot be affiliated with the organisation.",
                        instructorUuid)));

        boolean created = organisationAffiliationService.affiliateHiredInstructor(
                instructorUserUuid, job.getOrganisationUuid(), job.getBranchUuid());

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
                    "A class can only be created once an applicant has been hired for this job.");
        }

        // A job posted with a preferred instructor, or one assigned before assignment folded into
        // this step, already names its instructor; only a fresh hire still needs assigning.
        if (job.getAssignedInstructorUuid() == null) {
            assignHiredInstructor(job);
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
        recordSelectedInstructor(job, instructorUuid);
    }

    private void recordSelectedInstructor(ClassMarketplaceJob job, UUID instructorUuid) {
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        job.setAssignedInstructorUuid(instructorUuid);
    }

    private void applyJobDraft(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request, ResolvedLocation location) {
        job.setOrganisationUuid(request.organisationUuid());
        job.setBranchUuid(request.branchUuid());
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
        job.setLocationName(location.locationName());
        job.setLocationLatitude(location.latitude());
        job.setLocationLongitude(location.longitude());
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

    // Price and pay share one basis: the price may not undercut the organisation's approved rate
    // for it, and the pay may not exceed the price. The difference is the organisation's margin.
    private void applyJobPricing(ClassMarketplaceJob job, ClassMarketplaceJobRequestDTO request) {
        RateBasis basis = request.rateBasis();
        if (basis == null) {
            throw new IllegalArgumentException("rate_basis is required");
        }
        BigDecimal approvedRate = resolveOrganisationRateForRequest(request, basis)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Your rate card has no approved %s rate for %s %s classes of this %s. Add it to your rate card first.",
                        RateWording.basis(basis),
                        RateWording.format(request.sessionFormat()),
                        RateWording.location(request.locationType()),
                        request.courseUuid() != null ? "course" : "training program")));

        BigDecimal salePrice = request.salePrice();
        if (salePrice == null) {
            throw new IllegalArgumentException("sale_price is required");
        }
        if (salePrice.compareTo(approvedRate) < 0) {
            throw new IllegalArgumentException(String.format("Sale price is below your approved rate of %s %s.",
                    RateWording.money(approvedRate), RateWording.basis(basis)));
        }

        BigDecimal instructorPay = request.instructorPay();
        if (instructorPay == null) {
            throw new IllegalArgumentException("instructor_pay is required");
        }
        if (instructorPay.signum() <= 0) {
            throw new IllegalArgumentException("Instructor pay must be greater than zero.");
        }
        if (instructorPay.compareTo(salePrice) > 0) {
            throw new IllegalArgumentException("Instructor pay cannot exceed the sale price.");
        }

        BigDecimal minimumTrainingFee = resolveMinimumTrainingFeeForRequest(request);
        if (minimumTrainingFee != null && salePrice.compareTo(minimumTrainingFee) < 0) {
            throw new IllegalArgumentException("Sale price cannot be less than the course minimum training fee.");
        }

        job.setSalePrice(salePrice);
        job.setInstructorPay(instructorPay);
        job.setRateBasis(basis);
    }

    private BigDecimal resolveMinimumTrainingFeeForRequest(ClassMarketplaceJobRequestDTO request) {
        if (request.courseUuid() != null) {
            return courseInfoService.getMinimumTrainingFee(request.courseUuid()).orElse(null);
        }
        return courseInfoService.getProgramMinimumTrainingFee(request.programUuid()).orElse(null);
    }

    private Optional<BigDecimal> resolveOrganisationRateForRequest(ClassMarketplaceJobRequestDTO request, RateBasis basis) {
        Optional<BigDecimal> rate = request.courseUuid() != null
                ? courseTrainingApprovalSpi.resolveOrganisationRate(
                        request.courseUuid(), request.organisationUuid(), request.sessionFormat(), request.locationType(),
                        basis)
                : courseTrainingApprovalSpi.resolveOrganisationProgramRate(
                        request.programUuid(), request.organisationUuid(), request.sessionFormat(), request.locationType(),
                        basis);
        return pricedRate(rate);
    }

    /** An unpriced rate card cell can still read as zero, which is "not offered", never "free". */
    private static Optional<BigDecimal> pricedRate(Optional<BigDecimal> rate) {
        return rate.filter(value -> value.signum() > 0);
    }

    private ResolvedLocation validateJobDraft(ClassMarketplaceJobRequestDTO request) {
        validateLearningContext(request);
        validateRegistrationWindow(request);
        if (request.branchUuid() == null) {
            throw new IllegalArgumentException("branch_uuid is required");
        }
        ResolvedLocation location = branchLocationResolver.resolve(request.organisationUuid(), request.branchUuid(),
                request.locationType(), request.locationName(), request.locationLatitude(), request.locationLongitude(), true);
        validateLocationRequirements(request.locationType(), location.locationName(), location.latitude(), location.longitude());
        validateSessionTemplates(request.sessionTemplates());
        validateJobResources(request);
        return location;
    }

    /**
     * A recruitment window that shuts the day it opens is dead by that night: the advert expires
     * before any instructor has had a working day to find it and answer it.
     */
    private void validateRegistrationWindow(ClassMarketplaceJobRequestDTO request) {
        LocalDate start = request.registrationPeriodStartDate();
        LocalDate end = request.registrationPeriodEndDate();
        if (start == null || end == null) {
            return;
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(String.format(
                    "Registration closes on %s, before it opens on %s. "
                            + "registration_period_end_date must be after registration_period_start_date.",
                    end, start));
        }
        if (end.isEqual(start)) {
            throw new IllegalArgumentException(String.format(
                    "Registration opens and closes on the same day (%s), leaving instructors no time to apply. "
                            + "registration_period_end_date must be at least one day after registration_period_start_date.",
                    start));
        }
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
            if (summary.branchUuid() == null) {
                throw new IllegalArgumentException(String.format(
                        "Resource '%s' is not assigned to a branch; assign it to the job's branch first", summary.name()));
            }
            if (!summary.branchUuid().equals(request.branchUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Resource '%s' is not at the job's branch", summary.name()));
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
        InstructorRateCheck rateCheck = checkInstructorRate(job, instructorUuid);
        if (!rateCheck.payCoversRate()) {
            throw new IllegalStateException(instructorRateRefusal(job, rateCheck));
        }
        List<ClassSchedulingConflictDTO> scheduleConflicts = findInstructorScheduleConflicts(job, instructorUuid);
        if (!scheduleConflicts.isEmpty()) {
            throw new SchedulingConflictException(
                    "Your existing schedule conflicts with this job's planned sessions.",
                    scheduleConflicts);
        }
    }

    /**
     * Expands the job's session templates and reports every occurrence that clashes with the
     * instructor's existing schedule (completed instances aside), a firm hold they carry for
     * another class job, or declared unavailability.
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

        // Only firm holds come back, and never this job's own, so a pending application
        // elsewhere never freezes a diary and a job never conflicts with itself.
        List<InstructorTimeHoldDTO> blockingHolds = instructorTimeHoldService.findBlockingHolds(
                instructorUuid, minDate.atStartOfDay(), maxDate.plusDays(1).atStartOfDay(), job.getUuid());

        List<ClassSchedulingConflictDTO> conflicts = new ArrayList<>();
        for (OccurrenceWindow occurrence : occurrences) {
            List<String> reasons = new ArrayList<>();
            boolean overlapsSchedule = existingSchedule.stream().anyMatch(instance ->
                    instance.startTime().isBefore(occurrence.end()) && instance.endTime().isAfter(occurrence.start()));
            if (overlapsSchedule) {
                reasons.add("Instructor already has a scheduled session or blocked time overlapping this window");
            }
            boolean overlapsHold = blockingHolds.stream().anyMatch(hold ->
                    hold.startTime().isBefore(occurrence.end()) && hold.endTime().isAfter(occurrence.start()));
            if (overlapsHold) {
                reasons.add("Instructor is already committed to another class job in this window");
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
        return expandSessionTemplates(loadSessionTemplates(jobUuid));
    }

    // Expanded exactly the way class creation expands it, template zone included: a rule authored as
    // "every Wednesday" has to mean Wednesday where it was written, or holds and scheduled instances
    // land on different days whenever a session sits within the zone's offset of midnight.
    private List<OccurrenceWindow> expandSessionTemplates(List<ClassSessionTemplateDTO> templates) {
        List<OccurrenceWindow> occurrences = new ArrayList<>();
        for (ClassSessionTemplateDTO template : templates) {
            occurrences.addAll(RecurrenceExpander.expand(
                    template.startTime(),
                    template.endTime(),
                    RecurrencePatterns.fromRecurrenceDTO(template.recurrence()),
                    normalizeTimezone(template.timezone())));
        }
        return occurrences;
    }

    /**
     * Pencils the job's session windows into the applicant's diary. The holds stay tentative
     * until the organisation hires, so overlapping applications to different jobs all stand.
     */
    private void holdInstructorTimeForApplication(ClassMarketplaceJob job,
                                                  ClassMarketplaceJobApplication application) {
        List<ClassSessionTemplateDTO> templates = loadSessionTemplates(job.getUuid());
        holdInstructorTime(job, application, expandSessionTemplates(templates), resolveHoldTimezone(templates));
    }

    private void holdInstructorTime(ClassMarketplaceJob job,
                                    ClassMarketplaceJobApplication application,
                                    List<OccurrenceWindow> occurrences,
                                    String timezone) {
        if (occurrences.isEmpty()) {
            return;
        }
        instructorTimeHoldService.holdForApplication(buildHoldRequest(job, application, occurrences, timezone));
    }

    /**
     * Reserves the hired instructor's diary, rebuilding the windows from the job when the
     * application has nothing to promote. Every application older than the holds table is in that
     * state, and a hire with no firm hold is one a second organisation can book straight over.
     */
    private void firmInstructorTimeForApplication(ClassMarketplaceJob job,
                                                  ClassMarketplaceJobApplication application) {
        List<ClassSessionTemplateDTO> templates = loadSessionTemplates(job.getUuid());
        instructorTimeHoldService.firmOrCreateHoldsForApplication(buildHoldRequest(
                job, application, expandSessionTemplates(templates), resolveHoldTimezone(templates)));
    }

    private InstructorTimeHoldRequest buildHoldRequest(ClassMarketplaceJob job,
                                                       ClassMarketplaceJobApplication application,
                                                       List<OccurrenceWindow> occurrences,
                                                       String timezone) {
        UUID instructorUuid = application.getInstructorUuid();
        return new InstructorTimeHoldRequest(
                instructorUuid,
                instructorLookupService.getInstructorUserUuid(instructorUuid).orElse(null),
                job.getOrganisationUuid(),
                job.getUuid(),
                application.getUuid(),
                job.getTitle(),
                timezone,
                occurrences);
    }

    /**
     * Rebuilds the tentative holds of every application still in the funnel after the
     * organisation moved the job's dates.
     */
    private void rebuildInstructorHoldsForActiveApplications(ClassMarketplaceJob job) {
        List<ClassMarketplaceJobApplication> activeApplications =
                applicationRepository.findByJobUuidAndStatusIn(job.getUuid(), ACTIVE_APPLICATION_STATUSES);
        if (activeApplications.isEmpty()) {
            return;
        }

        List<ClassSessionTemplateDTO> templates = loadSessionTemplates(job.getUuid());
        List<OccurrenceWindow> occurrences = expandSessionTemplates(templates);
        String timezone = resolveHoldTimezone(templates);
        // A tentative hold consults nobody else's diary, so re-holding cannot clash and has nothing
        // to recover from. Catching here would only hide a bug behind a rollback-only transaction.
        for (ClassMarketplaceJobApplication application : activeApplications) {
            if (application.getInstructorUuid() == null) {
                log.warn("Application {} on job {} names no instructor; skipping its time hold",
                        application.getUuid(), job.getUuid());
                continue;
            }
            holdInstructorTime(job, application, occurrences, timezone);
        }
    }

    /**
     * The timezone the job's windows were authored in; the first template wins because a job's
     * sessions are posted as one schedule.
     */
    private String resolveHoldTimezone(List<ClassSessionTemplateDTO> templates) {
        return templates.stream()
                .map(ClassSessionTemplateDTO::timezone)
                .filter(timezone -> timezone != null && !timezone.isBlank())
                .findFirst()
                .orElse(DEFAULT_SCHEDULE_TIMEZONE);
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

    /**
     * Holds the funnel to its order: a decision is taken only on a candidate who has been
     * through every decision before it. The enum owns the order; this only reports the refusal,
     * naming the stage the applicant is in and the one the caller reached for.
     */
    private void ensureTransitionAllowed(ClassMarketplaceJobApplication application,
                                         ClassMarketplaceJobApplicationStatus target) {
        ClassMarketplaceJobApplicationStatus current = application.getStatus();
        if (target.isReachableFrom(current)) {
            return;
        }
        if (current == null || !current.isActive()) {
            throw new IllegalStateException(String.format(
                    "This application is closed at the %s stage and cannot be moved to %s.",
                    current == null ? "unknown" : current.getValue(), target.getValue()));
        }
        ClassMarketplaceJobApplicationStatus required = target.previousStage();
        throw new IllegalStateException(String.format(
                "This application is at the %s stage and cannot be moved to %s. %s follows %s only.",
                current.getValue(),
                target.getValue(),
                target.getValue(),
                required == null ? "no stage" : required.getValue()));
    }

    private void requireOrganisationManagerAccess(UUID organisationUuid) {
        UUID currentUserUuid = requireCurrentUserUuid();
        // managesOrganisation is itself memoised per organisation, so the several guarded steps of
        // one job action share a single membership query.
        if (!domainSecurityService.managesOrganisation(organisationUuid)) {
            throw new AccessDeniedException(String.format(
                    "User %s is not allowed to manage marketplace jobs for organisation %s.",
                    currentUserUuid,
                    organisationUuid));
        }
    }

    /**
     * Reading a job's applications is open to the posting organisation's managers and to platform
     * admins, who reach the same rows from the other direction through
     * {@link #listInstructorApplications}. Kept separate from
     * {@link #requireOrganisationManagerAccess} so that admitting admins to a read does not admit
     * them to posting, cancelling or deciding on another organisation's jobs.
     */
    private void requireOrganisationApplicationReadAccess(UUID organisationUuid) {
        if (domainSecurityService.isPlatformAdmin()) {
            return;
        }
        requireOrganisationManagerAccess(organisationUuid);
    }

    private UUID resolveCurrentInstructorUuid() {
        requireCurrentUserUuid(); // rejects an unauthenticated caller before anything else
        if (!domainSecurityService.isInstructor()) {
            throw new AccessDeniedException("Only instructors can apply to marketplace class jobs.");
        }

        // The caller's own instructor identity, resolved once per request rather than per step.
        UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
        if (instructorUuid == null) {
            throw new AccessDeniedException("The current user does not have an instructor profile.");
        }
        return instructorUuid;
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

        // A hire that never reached its class is just as committed as an assignment, and the
        // instructor was already told so.
        applicationRepository.findByJobUuidAndUuid(job.getUuid(), assignedApplicationUuid)
                .filter(application -> application.getStatus().isHire())
                .ifPresent(application -> {
                    application.setStatus(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
                    application.setReviewNotes("This class job was cancelled before its class was created.");
                    application.setReviewedBy(resolveReviewer());
                    application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
                    ClassMarketplaceJobApplication saved = applicationRepository.save(application);
                    applicationHistory.record(saved, ClassMarketplaceJobApplicationEventType.NOT_SELECTED,
                            saved.getReviewNotes(), null);
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
                        ClassMarketplaceJobApplicationStatus.HIRED
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
            applicationHistory.recordAll(toUpdate, ClassMarketplaceJobApplicationEventType.NOT_SELECTED, closureReason);
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
     * Converts the job's recruitment holds - resources and the hired instructor's diary alike -
     * into confirmed rows linked to the scheduled instances the class creation just produced.
     * Holds whose window was not scheduled (skipped or rolled over occurrences) are released.
     */
    private void convertHoldsToConfirmedBookings(ClassMarketplaceJob job, UUID classDefinitionUuid) {
        List<InstanceWindow> instanceWindows = timetableService()
                .getScheduledInstancesForClassDefinition(classDefinitionUuid)
                .stream()
                .map(instance -> new InstanceWindow(instance.uuid(), instance.startTime(), instance.endTime()))
                .toList();
        resourceBookingService.confirmHoldsForJob(job.getUuid(), classDefinitionUuid, instanceWindows);
        instructorTimeHoldService.confirmHoldsForJob(job.getUuid(), classDefinitionUuid, instanceWindows);
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
                job.getBranchUuid(),
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

    private List<ClassMarketplaceJobResourceDTO> loadJobResources(UUID jobUuid,
                                                                  Map<UUID, ResourceBookingStatus> bookingStatuses) {
        return jobResourceRepository.findByJobUuidOrderByCreatedDateAsc(jobUuid)
                .stream()
                .map(resource -> {
                    Optional<ResourceSummary> summary = lookupResource(resource.getResourceUuid());
                    return new ClassMarketplaceJobResourceDTO(
                            resource.getResourceUuid(),
                            resource.getQuantity(),
                            summary.map(ResourceSummary::name).orElse(null),
                            summary.map(ResourceSummary::resourceType).orElse(null),
                            bookingStatuses.get(resource.getResourceUuid()));
                })
                .toList();
    }

    /** Memoised per resource so a page of jobs sharing a venue resolves it once. */
    private Optional<ResourceSummary> lookupResource(UUID resourceUuid) {
        if (resourceUuid == null) {
            return Optional.empty();
        }
        return requestScopedCache.get(CACHE_RESOURCE_PREFIX + resourceUuid,
                () -> resourceLookupService.getResource(resourceUuid));
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

    /**
     * Next to the sale price, the pay an advert offers gives away the organisation's margin. It
     * belongs to the people who set it — the posting organisation's managers and platform admins —
     * and to the audience the advert is written for: instructors who could actually take the work.
     * <p>
     * That audience is deliberately the <em>verified</em> instructors rather than everyone holding
     * the {@code instructor} domain. The domain is self-service, so gating on it would leave the
     * figure readable by any signed-in user who first created an instructor profile; verification
     * is an administrator's decision, and it is already the bar
     * {@link #ensureInstructorEligibleToApply} sets before an instructor may apply, so nobody who
     * can act on the number is denied it. Every other signed-in browser of the marketplace —
     * learners, guardians, course creators, unverified applicants and managers of rival
     * organisations — reads the advert without it. The DTO omits null fields, so the figure is
     * absent from their JSON rather than present and empty.
     */
    private boolean canSeeInstructorPay(ClassMarketplaceJob job) {
        return payVisibleToCallerEverywhere()
                || domainSecurityService.managesOrganisation(job.getOrganisationUuid());
    }

    /**
     * The half of {@link #canSeeInstructorPay} that does not depend on which job is being read,
     * memoised because a marketplace page asks it once per advert.
     */
    private boolean payVisibleToCallerEverywhere() {
        return requestScopedCache.get(CACHE_PAY_VISIBLE,
                () -> domainSecurityService.isVerifiedInstructor() || domainSecurityService.isPlatformAdmin());
    }

    /** Per-page read data loaded in bulk so a job list never queries once per row. */
    private record JobReadContext(Map<UUID, Long> applicationCounts,
                                  Map<UUID, UUID> instructorsByApplication,
                                  Map<UUID, Map<UUID, ResourceBookingStatus>> resourceBookingStatuses) {
    }

    private JobReadContext loadJobReadContext(List<ClassMarketplaceJob> jobs) {
        List<UUID> jobUuids = jobs.stream()
                .map(ClassMarketplaceJob::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Long> applicationCounts = new HashMap<>();
        if (!jobUuids.isEmpty()) {
            applicationRepository.countByJobUuidInExcludingStatus(jobUuids, ClassMarketplaceJobApplicationStatus.WITHDRAWN)
                    .forEach(count -> applicationCounts.put(count.jobUuid(), count.applicationCount()));
        }

        // A hire records its application on the job; the instructor itself is only stamped once the class exists.
        List<UUID> hiredApplicationUuids = jobs.stream()
                .filter(job -> job.getAssignedInstructorUuid() == null && job.getAssignedApplicationUuid() != null)
                .map(ClassMarketplaceJob::getAssignedApplicationUuid)
                .distinct()
                .toList();
        Map<UUID, UUID> instructorsByApplication = new HashMap<>();
        if (!hiredApplicationUuids.isEmpty()) {
            applicationRepository.findByUuidIn(hiredApplicationUuids)
                    .forEach(application -> instructorsByApplication.put(application.getUuid(), application.getInstructorUuid()));
        }
        Map<UUID, Map<UUID, ResourceBookingStatus>> resourceBookingStatuses = jobUuids.isEmpty()
                ? Map.of()
                : resourceBookingService.summariseJobBookings(jobUuids);
        return new JobReadContext(applicationCounts, instructorsByApplication, resourceBookingStatuses);
    }

    private ClassMarketplaceJobDTO toJobDTO(ClassMarketplaceJob job) {
        return toJobDTO(job, loadJobReadContext(List.of(job)));
    }

    private ClassMarketplaceJobDTO toJobDTO(ClassMarketplaceJob job, JobReadContext context) {
        UUID hiredInstructorUuid = job.getAssignedInstructorUuid() != null
                ? job.getAssignedInstructorUuid()
                : context.instructorsByApplication().get(job.getAssignedApplicationUuid());
        return new ClassMarketplaceJobDTO(
                job.getUuid(),
                job.getOrganisationUuid(),
                job.getCourseUuid(),
                job.getProgramUuid(),
                job.getTitle(),
                job.getDescription(),
                job.getSalePrice(),
                canSeeInstructorPay(job) ? job.getInstructorPay() : null,
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
                loadJobResources(job.getUuid(),
                        context.resourceBookingStatuses().getOrDefault(job.getUuid(), Map.of())),
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
                job.getRemindViaPush(),
                job.getBranchUuid(),
                resolveBranchName(job.getBranchUuid()),
                context.applicationCounts().getOrDefault(job.getUuid(), 0L),
                hiredInstructorUuid
        );
    }

    /** Memoised per branch so a page of jobs looks each distinct branch up once. */
    private String resolveBranchName(UUID branchUuid) {
        if (branchUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_BRANCH_NAME_PREFIX + branchUuid,
                () -> branchLocationResolver.branchName(branchUuid).orElse(null));
    }

    private ClassMarketplaceJobApplicationDTO toApplicationDTO(ClassMarketplaceJobApplication application) {
        return toApplicationDTO(application, null);
    }

    private ClassMarketplaceJobApplicationDTO toApplicationDTO(ClassMarketplaceJobApplication application,
                                                               ClassMarketplaceJob job) {
        Boolean instructorAdminVerified = null;
        Boolean trainingApproved = null;
        BigDecimal approvedRate = null;
        Boolean rateCoversPay = null;
        if (job != null) {
            instructorAdminVerified = isInstructorAdminVerified(application.getInstructorUuid());
            trainingApproved = isInstructorApprovedForJob(job, application.getInstructorUuid());
            InstructorRateCheck rateCheck = checkInstructorRate(job, application.getInstructorUuid());
            approvedRate = rateCheck.approvedRate();
            rateCoversPay = rateCheck.payCoversRate();
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
                rateCoversPay,
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedDate(),
                application.getLastModifiedDate(),
                application.getCreatedBy(),
                application.getLastModifiedBy()
        );
    }

    private Optional<BigDecimal> resolveInstructorRateForJob(ClassMarketplaceJob job, UUID instructorUuid) {
        Optional<BigDecimal> rate = job.getCourseUuid() != null
                ? courseTrainingApprovalSpi.resolveInstructorRate(
                        job.getCourseUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType(),
                        job.getRateBasis())
                : courseTrainingApprovalSpi.resolveInstructorProgramRate(
                        job.getProgramUuid(), instructorUuid, job.getSessionFormat(), job.getLocationType(),
                        job.getRateBasis());
        return pricedRate(rate);
    }

    /** The instructor's approved rate for the job's cell, or null when they have none. */
    private record InstructorRateCheck(BigDecimal approvedRate, BigDecimal pay) {

        boolean payCoversRate() {
            return approvedRate != null && pay != null && pay.compareTo(approvedRate) >= 0;
        }
    }

    private InstructorRateCheck checkInstructorRate(ClassMarketplaceJob job, UUID instructorUuid) {
        return new InstructorRateCheck(resolveInstructorRateForJob(job, instructorUuid).orElse(null),
                job.getInstructorPay());
    }

    private String instructorRateRefusal(ClassMarketplaceJob job, InstructorRateCheck rateCheck) {
        String basis = RateWording.basis(job.getRateBasis());
        if (rateCheck.approvedRate() == null) {
            return String.format(
                    "You don't have an approved %s rate for %s %s classes of this %s. Add it to your rate card.",
                    basis, RateWording.format(job.getSessionFormat()), RateWording.location(job.getLocationType()),
                    learningContextType(job));
        }
        return String.format("Your approved rate of %s %s is above this job's pay of %s.",
                RateWording.money(rateCheck.approvedRate()), basis, RateWording.money(rateCheck.pay()));
    }

    /** Refuses to hire or assign an instructor whose approved rate the job's pay does not cover. */
    private void refuseUncoveredInstructorRate(ClassMarketplaceJob job, UUID instructorUuid) {
        InstructorRateCheck rateCheck = checkInstructorRate(job, instructorUuid);
        if (rateCheck.payCoversRate()) {
            return;
        }
        String instructorName = resolveInstructorName(instructorUuid, "This instructor");
        String basis = RateWording.basis(job.getRateBasis());
        if (rateCheck.approvedRate() == null) {
            throw new IllegalStateException(String.format("%s has no approved %s rate for this %s yet.",
                    instructorName, basis, learningContextType(job)));
        }
        throw new IllegalStateException(String.format("%s's approved rate of %s %s is above this job's pay of %s.",
                instructorName, RateWording.money(rateCheck.approvedRate()), basis,
                RateWording.money(rateCheck.pay())));
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
