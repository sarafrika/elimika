package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Alerts the organisation and the instructor when a hire is refused because the instructor's schedule clashes. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketplaceHireClashNotifier {

    static final String ORGANISATION_JOBS_URL = "/dashboard/organisation/opportunities";
    static final String INSTRUCTOR_APPLICATIONS_URL = "/dashboard/instructor/opportunities/my-applications";
    private static final DateTimeFormatter CLASH_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm 'UTC'");

    private final UserLookupService userLookupService;
    private final InstructorLookupService instructorLookupService;
    private final OrganisationLookupService organisationLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditUserResolver auditUserResolver;

    // Own transaction: the refused hire rolls back, and a notification request only dispatches once the
    // transaction that published it commits. A null application means a direct hire.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyHireBlocked(ClassMarketplaceJob job,
                                  UUID applicationUuid,
                                  UUID instructorUuid,
                                  UUID hiringUserUuid,
                                  List<ClassSchedulingConflictDTO> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        Clash clash = Clash.of(job, applicationUuid, instructorUuid, conflicts);
        notifyOrganisation(clash, hiringUserUuid);
        notifyInstructor(clash);
    }

    private void notifyOrganisation(Clash clash, UUID hiringUserUuid) {
        NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_ORGANISATION;
        String instructorName = resolveInstructorName(clash.instructorUuid());
        String action = clash.jobPosted()
                ? "Choose another applicant or adjust the job's schedule."
                : "The job was not posted. Post it without naming them to take applications, or adjust its schedule.";
        String body = String.format("%s could not be hired for %s because their schedule clashes with %d of its sessions, "
                        + "the first on %s. What clashes: %s. %s",
                instructorName, clash.jobTitle(), clash.count(), clash.firstClashAt(),
                String.join("; ", clash.reasons()), action);
        String actionUrl = clash.jobPosted() ? ORGANISATION_JOBS_URL + "/" + clash.jobUuid() : ORGANISATION_JOBS_URL;
        Map<String, Object> metadata = withEntry(clash.metadata(), "instructor_name", instructorName);

        for (UUID recipient : organisationRecipients(clash.job(), hiringUserUuid)) {
            try {
                eventPublisher.publishEvent(NotificationRequestedEvent.inApp(recipient, type.getValue(), "INBOX",
                        type.getDisplayName(), body, actionUrl, metadata, clash.dedupeKey(type)));
                publishEmail(recipient, type, Map.of(
                        "instructorName", instructorName,
                        "contextName", clash.jobTitle(),
                        "clashCount", clash.count(),
                        "firstClashAt", clash.firstClashAt(),
                        "clashReasons", clash.reasons(),
                        "jobPosted", clash.jobPosted(),
                        "actionPath", actionUrl));
            } catch (Exception e) {
                log.warn("Failed to alert organisation user {} of the blocked hire on job {}: {}",
                        recipient, clash.jobUuid(), e.getMessage());
            }
        }
    }

    private void notifyInstructor(Clash clash) {
        try {
            UUID recipient = instructorLookupService.getInstructorUserUuid(clash.instructorUuid()).orElse(null);
            if (recipient == null) {
                log.debug("Instructor {} has no user account; skipping the blocked-hire alert", clash.instructorUuid());
                return;
            }
            NotificationType type = NotificationType.CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_INSTRUCTOR;
            String organisationName = organisationLookupService.findOrganisationName(clash.job().getOrganisationUuid())
                    .filter(name -> !name.isBlank())
                    .orElse("the organisation");
            String body = String.format("%s tried to hire you for %s, but your schedule clashes with %d of its sessions, "
                            + "the first on %s. What clashes: %s. Free up those times (reschedule or release the other "
                            + "commitment) so %s can hire you.",
                    Character.toUpperCase(organisationName.charAt(0)) + organisationName.substring(1),
                    clash.jobTitle(), clash.count(), clash.firstClashAt(),
                    String.join("; ", clash.reasons()), organisationName);

            Map<String, Object> metadata = withEntry(clash.metadata(), "organisation_name", organisationName);
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(recipient, type.getValue(), "INBOX",
                    type.getDisplayName(), body, INSTRUCTOR_APPLICATIONS_URL, metadata, clash.dedupeKey(type)));
            publishEmail(recipient, type, Map.of(
                    "organisationName", organisationName,
                    "contextName", clash.jobTitle(),
                    "clashCount", clash.count(),
                    "firstClashAt", clash.firstClashAt(),
                    "clashReasons", clash.reasons(),
                    "actionPath", INSTRUCTOR_APPLICATIONS_URL));
        } catch (Exception e) {
            log.warn("Failed to alert instructor {} of the blocked hire on job {}: {}",
                    clash.instructorUuid(), clash.jobUuid(), e.getMessage());
        }
    }

    // The job's creator is who hears about its applicants elsewhere; the manager who pressed hire hears too.
    private Set<UUID> organisationRecipients(ClassMarketplaceJob job, UUID hiringUserUuid) {
        Set<UUID> recipients = new LinkedHashSet<>();
        auditUserResolver.userOf(job.getCreatedBy()).ifPresent(recipients::add);
        if (hiringUserUuid != null) {
            recipients.add(hiringUserUuid);
        }
        return recipients;
    }

    private void publishEmail(UUID recipient, NotificationType type, Map<String, Object> variables) {
        String email = userLookupService.getUserEmail(recipient).orElse(null);
        if (email == null || email.isBlank()) {
            return;
        }
        String name = userLookupService.getUserFullName(recipient).orElse(email);
        eventPublisher.publishEvent(NotificationRequestedEvent.email(recipient, email, name, type.getValue(),
                withEntry(variables, "recipientName", name)));
    }

    private String resolveInstructorName(UUID instructorUuid) {
        return instructorLookupService.getInstructorUserUuid(instructorUuid)
                .flatMap(userLookupService::getUserFullName)
                .filter(name -> !name.isBlank())
                .orElse("The instructor");
    }

    private static Map<String, Object> withEntry(Map<String, Object> base, String key, Object value) {
        Map<String, Object> copy = new HashMap<>(base);
        copy.put(key, value);
        return copy;
    }

    private record Clash(ClassMarketplaceJob job,
                         UUID applicationUuid,
                         UUID instructorUuid,
                         String jobTitle,
                         int count,
                         ClassSchedulingConflictDTO first,
                         List<String> reasons) {

        static Clash of(ClassMarketplaceJob job,
                        UUID applicationUuid,
                        UUID instructorUuid,
                        List<ClassSchedulingConflictDTO> conflicts) {
            List<ClassSchedulingConflictDTO> ordered = conflicts.stream()
                    .sorted(Comparator.comparing(ClassSchedulingConflictDTO::requestedStart))
                    .toList();
            List<String> reasons = ordered.stream()
                    .flatMap(conflict -> conflict.reasons() == null ? Stream.empty() : conflict.reasons().stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            String title = job.getTitle() == null || job.getTitle().isBlank() ? "the class job" : job.getTitle();
            return new Clash(job, applicationUuid, instructorUuid, title, ordered.size(), ordered.getFirst(), reasons);
        }

        // Only a hire of an applicant leaves a posting behind; a direct hire is refused together with its job.
        boolean jobPosted() {
            return applicationUuid != null;
        }

        UUID jobUuid() {
            return job.getUuid();
        }

        String firstClashAt() {
            return CLASH_FORMATTER.format(first.requestedStart());
        }

        Map<String, Object> metadata() {
            return Map.of(
                    "job_uuid", jobPosted() && jobUuid() != null ? jobUuid() : "",
                    "application_uuid", jobPosted() ? applicationUuid : "",
                    "instructor_uuid", instructorUuid,
                    "job_title", jobTitle,
                    "clash_count", count,
                    "first_clash_start", first.requestedStart().toString(),
                    "first_clash_end", first.requestedEnd() == null ? "" : first.requestedEnd().toString(),
                    "first_clash_at", firstClashAt(),
                    "clash_reasons", reasons);
        }

        String dedupeKey(NotificationType type) {
            return "class-marketplace-job-hire-blocked:" + type.getValue() + ":" + jobUuid() + ":" + instructorUuid
                    + ":" + first.requestedStart() + ":" + count;
        }
    }
}
