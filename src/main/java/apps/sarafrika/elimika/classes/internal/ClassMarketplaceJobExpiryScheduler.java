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
 * Expires open marketplace jobs whose recruitment window has passed and releases
 * the resource holds they were keeping, so venues and equipment become bookable
 * again without manual intervention.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ClassMarketplaceJobExpiryScheduler {

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobApplicationRepository applicationRepository;
    private final ResourceBookingService resourceBookingService;
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
            job.setStatus(ClassMarketplaceJobStatus.EXPIRED);
            resourceBookingService.releaseHoldsForJob(job.getUuid(), reason);
            closeOutstandingApplications(job);
            notifyJobCreator(job);
        }
        jobRepository.saveAll(lapsedJobs);
        log.info("Expired {} lapsed marketplace class jobs and released their resource holds", lapsedJobs.size());
    }

    /**
     * Closes every application still moving through an expired job's funnel. Without this
     * the applicants stay PENDING forever and are never told the job lapsed.
     */
    private void closeOutstandingApplications(ClassMarketplaceJob job) {
        List<ClassMarketplaceJobApplication> outstanding = applicationRepository.findByJobUuidAndStatusIn(
                job.getUuid(),
                List.of(
                        ClassMarketplaceJobApplicationStatus.PENDING,
                        ClassMarketplaceJobApplicationStatus.SHORTLISTED,
                        ClassMarketplaceJobApplicationStatus.INTERVIEWING,
                        ClassMarketplaceJobApplicationStatus.OFFERED,
                        ClassMarketplaceJobApplicationStatus.APPROVED,
                        ClassMarketplaceJobApplicationStatus.ASSIGNED
                )
        );
        if (outstanding.isEmpty()) {
            return;
        }

        for (ClassMarketplaceJobApplication application : outstanding) {
            // An AWAITING_CLASS job that lapses still holds a hire. Closing the application without
            // clearing the job's pointers would leave it naming an instructor it no longer has.
            if (application.getStatus() == ClassMarketplaceJobApplicationStatus.ASSIGNED) {
                job.setAssignedApplicationUuid(null);
                job.setAssignedInstructorUuid(null);
            }
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

    private void notifyJobCreator(ClassMarketplaceJob job) {
        try {
            UUID creatorUserUuid = job.getCreatedBy() == null
                    ? null
                    : userLookupService.findUserUuidByEmail(job.getCreatedBy()).orElse(null);
            if (creatorUserUuid == null) {
                log.debug("No resolvable creator for expired marketplace job {}; skipping notification", job.getUuid());
                return;
            }

            NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_EXPIRED;
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    creatorUserUuid,
                    type.getValue(),
                    "INBOX",
                    type.getDisplayName(),
                    String.format("Your class job '%s' expired without an instructor being assigned. Its venue and equipment reservations have been released.", job.getTitle()),
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
