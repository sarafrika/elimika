package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.model.TrainingApplicationRecord;
import apps.sarafrika.elimika.course.model.TrainingRateUpdate;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells the owner a rate update awaits review, and the applicant how it was decided. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingRateUpdateNotifier {

    public static final String SUBMITTED = "TRAINING_RATE_UPDATE_SUBMITTED";
    public static final String APPROVED = "TRAINING_RATE_UPDATE_APPROVED";
    public static final String REJECTED = "TRAINING_RATE_UPDATE_REJECTED";

    static final String OWNER_REVIEW_URL = "/dashboard/course-creator/training-applications?tab=rate-updates";
    static final String INSTRUCTOR_RATE_CARD_URL = "/dashboard/instructor/rate-card";
    static final String ORGANISATION_APPROVAL_URL = "/dashboard/organisation/approvals/";

    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final TrainingSubmitters submitters;
    private final ApplicationEventPublisher eventPublisher;

    /** The course or program an application targets, as the notifications name it. */
    public record Subject(TrainingApplicationType type, UUID parentUuid, String parentName, UUID ownerUserUuid) {
    }

    public void submitted(Subject subject, TrainingApplicationRecord application, TrainingRateUpdate update,
                          String applicantName) {
        if (subject.ownerUserUuid() == null) {
            return;
        }
        try {
            String who = applicantName == null || applicantName.isBlank() ? "An approved trainer" : applicantName;
            Map<String, Object> metadata = metadata(subject, application, update);
            metadata.put("applicant_name", applicantName == null ? "" : applicantName);
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    subject.ownerUserUuid(),
                    SUBMITTED,
                    "INBOX",
                    "Rate update to review",
                    who + " proposed new rates for " + subject.parentName() + ".",
                    OWNER_REVIEW_URL,
                    metadata,
                    "training-rate-update-submitted:" + update.getUuid()));
        } catch (Exception e) {
            log.warn("Failed to notify the owner of rate update {}: {}", update.getUuid(), e.getMessage());
        }
    }

    public void decided(Subject subject, TrainingApplicationRecord application, TrainingRateUpdate update) {
        boolean approved = update.getStatus() == TrainingRateUpdateStatus.APPROVED;
        boolean organisation = CourseTrainingApplicantType.ORGANISATION.equals(application.getApplicantType());
        try {
            UUID recipient = organisation
                    ? submitters.userOf(update.getCreatedBy())
                            .or(() -> submitters.userOf(application.getCreatedBy())).orElse(null)
                    : instructorLookupService.getInstructorUserUuid(application.getApplicantUuid()).orElse(null);
            if (recipient == null) {
                return;
            }
            String type = approved ? APPROVED : REJECTED;
            String actionUrl = organisation ? ORGANISATION_APPROVAL_URL + application.getUuid() : INSTRUCTOR_RATE_CARD_URL;
            String body = approved
                    ? "Your new rates for " + subject.parentName() + " were approved and now apply."
                    : "Your proposed rates for " + subject.parentName() + " were not accepted; your current rates still apply.";
            Map<String, Object> metadata = metadata(subject, application, update);
            metadata.put("review_notes", update.getReviewNotes() == null ? "" : update.getReviewNotes());

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipient,
                    type,
                    "INBOX",
                    approved ? "Rate update approved" : "Rate update rejected",
                    body,
                    actionUrl,
                    metadata,
                    "training-rate-update-decision:" + update.getUuid() + ":" + type,
                    organisation ? "organisation_user" : null));
            email(recipient, type, subject, update, approved, actionUrl);
        } catch (Exception e) {
            log.warn("Failed to notify the applicant of rate update {}: {}", update.getUuid(), e.getMessage());
        }
    }

    private void email(UUID recipient, String type, Subject subject, TrainingRateUpdate update, boolean approved,
                       String actionUrl) {
        String address = userLookupService.getUserEmail(recipient).orElse(null);
        if (address == null || address.isBlank()) {
            return;
        }
        String name = userLookupService.getUserFullName(recipient).orElse(address);
        eventPublisher.publishEvent(NotificationRequestedEvent.email(recipient, address, name, type, Map.of(
                "recipientName", name,
                "contextType", subject.type() == TrainingApplicationType.PROGRAM ? "programme" : "course",
                "contextName", subject.parentName(),
                "approved", approved,
                "decisionLabel", approved ? "approved" : "not accepted",
                "reviewNotes", update.getReviewNotes() == null ? "" : update.getReviewNotes(),
                "actionPath", actionUrl)));
    }

    private static Map<String, Object> metadata(Subject subject, TrainingApplicationRecord application,
                                                TrainingRateUpdate update) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("application_type", subject.type().getValue());
        metadata.put(subject.type() == TrainingApplicationType.PROGRAM ? "program_uuid" : "course_uuid", subject.parentUuid());
        metadata.put(subject.type() == TrainingApplicationType.PROGRAM ? "program_title" : "course_name", subject.parentName());
        metadata.put("application_uuid", application.getUuid());
        metadata.put("rate_update_uuid", update.getUuid());
        metadata.put("applicant_type", application.getApplicantType().getValue());
        metadata.put("applicant_uuid", application.getApplicantUuid());
        return metadata;
    }
}
