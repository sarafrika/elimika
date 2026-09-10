package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expires open marketplace jobs whose recruitment window has passed and releases the holds
 * they were keeping, so venues, equipment and applicants' diaries become bookable again
 * without manual intervention. A hire the job already made is left standing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ClassMarketplaceJobExpiryScheduler {

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobApplicationRepository applicationRepository;
    private final ResourceBookingService resourceBookingService;
    private final InstructorTimeHoldService instructorTimeHoldService;
    private final UserLookupService userLookupService;
    private final InstructorLookupService instructorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    void expireLapsedJobs() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<ClassMarketplaceJob> lapsedJobs = jobRepository.findExpiredOpenJobs(today);
        if (lapsedJobs.isEmpty()) {
            return;
        }

        for (ClassMarketplaceJob job : lapsedJobs) {
            String reason = job.getStatus() == ClassMarketplaceJobStatus.AWAITING_CLASS
                    ? "Job expired before its class was created"
                    : "Job expired";
            boolean holdsHire = closeOutstandingApplications(job);
            if (holdsHire) {
                // The window closing ends recruitment, and recruitment is over: somebody was hired.
                // What remains is the class, which only AWAITING_CLASS can create and which has no
                // deadline, so the sweep leaves the job, its holds and its hire exactly as they are.
                continue;
            }
            job.setStatus(ClassMarketplaceJobStatus.EXPIRED);
            resourceBookingService.releaseHoldsForJob(job.getUuid(), reason);
            instructorTimeHoldService.releaseHoldsForJob(job.getUuid(), reason);
            notifyJobCreator(job, false);
        }
        jobRepository.saveAll(lapsedJobs);
        log.info("Expired {} lapsed marketplace class jobs and released their resource and instructor holds",
                lapsedJobs.size());
    }

    /**
     * Closes every application still moving through an expired job's funnel and reports whether
     * the job holds a hire. Without this the applicants stay PENDING forever and are never told
     * the job lapsed; hires are exempt because a lapsed window is not a decision to un-hire.
     */
    private boolean closeOutstandingApplications(ClassMarketplaceJob job) {
        List<ClassMarketplaceJobApplication> live = applicationRepository.findByJobUuidAndStatusIn(
                job.getUuid(),
                List.of(
                        ClassMarketplaceJobApplicationStatus.PENDING,
                        ClassMarketplaceJobApplicationStatus.SHORTLISTED,
                        ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                        ClassMarketplaceJobApplicationStatus.OFFERED,
                        ClassMarketplaceJobApplicationStatus.HIRED,
                        ClassMarketplaceJobApplicationStatus.ASSIGNED
                )
        );

        List<ClassMarketplaceJobApplication> outstanding = live.stream()
                .filter(application -> !application.getStatus().isHire())
                .toList();
        boolean holdsHire = outstanding.size() != live.size();
        if (outstanding.isEmpty()) {
            return holdsHire;
        }

        for (ClassMarketplaceJobApplication application : outstanding) {
            application.setStatus(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
            if (application.getReviewNotes() == null || application.getReviewNotes().isBlank()) {
                application.setReviewNotes("This class job expired before an instructor was confirmed.");
            }
            application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        applicationRepository.saveAll(outstanding);

        for (ClassMarketplaceJobApplication application : outstanding) {
            notifyApplicantOfExpiry(job, application);
        }
        return holdsHire;
    }

    private void notifyApplicantOfExpiry(ClassMarketplaceJob job, ClassMarketplaceJobApplication application) {
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

            String contextName = job.getTitle() == null ? "the class" : job.getTitle();
            NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_APPLICATION_CANCELLED;

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipientUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    "Your application to train " + contextName + " closed because the job expired.",
                    "/dashboard/instructor/opportunities/my-applications",
                    Map.of(
                            "job_uuid", job.getUuid(),
                            "application_uuid", application.getUuid(),
                            "context_name", contextName,
                            "review_notes", application.getReviewNotes() == null ? "" : application.getReviewNotes()
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
                            "statusLabel", "closed because the job expired",
                            "reviewNotes", application.getReviewNotes() == null ? "" : application.getReviewNotes()
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to publish expiry notification to applicant {}: {}",
                    application.getUuid(), e.getMessage());
        }
    }

    private void notifyJobCreator(ClassMarketplaceJob job, boolean holdsHire) {
        try {
            UUID creatorUserUuid = job.getCreatedBy() == null
                    ? null
                    : userLookupService.findUserUuidByEmail(job.getCreatedBy()).orElse(null);
            if (creatorUserUuid == null) {
                log.debug("No resolvable creator for expired marketplace job {}; skipping notification", job.getUuid());
                return;
            }

            NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_EXPIRED;
            String message = holdsHire
                    ? String.format("Your class job '%s' expired before its class was created. The instructor you hired keeps their place with your organisation, but the job's venue and equipment reservations have been released.", job.getTitle())
                    : String.format("Your class job '%s' expired without an instructor being hired. Its venue and equipment reservations have been released.", job.getTitle());
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    creatorUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    message,
                    "/dashboard/organisation/opportunities",
                    Map.of(
                            "job_uuid", job.getUuid(),
                            "job_title", job.getTitle() == null ? "" : job.getTitle()
                    ),
                    "class-marketplace-job-expired:" + job.getUuid()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish expiry notification for marketplace job {}: {}", job.getUuid(), e.getMessage());
        }
    }
}
