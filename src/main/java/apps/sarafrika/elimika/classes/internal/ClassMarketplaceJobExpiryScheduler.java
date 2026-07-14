package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
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
    private final ResourceBookingService resourceBookingService;
    private final UserLookupService userLookupService;
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
            job.setStatus(ClassMarketplaceJobStatus.EXPIRED);
            resourceBookingService.releaseHoldsForJob(job.getUuid(), "Job expired");
            notifyJobCreator(job);
        }
        jobRepository.saveAll(lapsedJobs);
        log.info("Expired {} lapsed marketplace class jobs and released their resource holds", lapsedJobs.size());
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
                    "/dashboard/organisation/jobs",
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
