package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Realistic sample emails for every template: used by the render test and the admin email test. */
public final class EmailSamples {

    private EmailSamples() {
    }

    /** Notification types that have an email template on the classpath. */
    public static List<NotificationType> typesWithTemplates() {
        return Arrays.stream(NotificationType.values())
                .filter(type -> new ClassPathResource("templates/" + type.getEmailTemplatePath()).exists())
                .toList();
    }

    public static NotificationEvent event(NotificationType type, String recipientEmail, Map<String, Object> variables) {
        UUID id = UUID.randomUUID();
        return new NotificationEvent() {
            @Override public UUID getNotificationId() { return id; }
            @Override public UUID getRecipientId() { return UUID.randomUUID(); }
            @Override public String getRecipientEmail() { return recipientEmail; }
            @Override public String getRecipientName() { return "Amina Otieno"; }
            @Override public NotificationType getNotificationType() { return type; }
            @Override public NotificationPriority getPriority() { return NotificationPriority.NORMAL; }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            @Override public Map<String, Object> getTemplateVariables() { return variables; }
        };
    }

    /** Sample values shaped for one type, so previews read like the real email. */
    public static Map<String, Object> sampleVariables(NotificationType type) {
        Map<String, Object> v = sampleVariables();
        String status = switch (type) {
            case CLASS_MARKETPLACE_JOB_APPLICATION_SHORTLISTED -> "has been shortlisted";
            case CLASS_MARKETPLACE_JOB_APPLICATION_INTERVIEWING -> "has moved to the interview stage";
            case CLASS_MARKETPLACE_JOB_APPLICATION_OFFERED -> "has received an offer";
            case CLASS_MARKETPLACE_JOB_APPLICATION_ASSIGNED -> "has been assigned";
            case CLASS_MARKETPLACE_JOB_APPLICATION_NOT_SELECTED -> "was not selected";
            case CLASS_MARKETPLACE_JOB_EXPIRED -> "closed because the job expired";
            case CLASS_MARKETPLACE_JOB_APPLICATION_CANCELLED -> "closed because the job was cancelled";
            default -> "was not successful";
        };
        v.put("statusLabel", status);
        switch (type) {
            case COURSE_TRAINING_APPLICATION_REJECTED -> v.put("contextType", "course");
            case PROGRAM_TRAINING_APPLICATION_REJECTED -> {
                v.put("contextType", "programme");
                v.put("contextName", "Junior Musicianship Programme");
            }
            case DOMAIN_APPROVAL_DECLINED -> {
                v.put("approved", false);
                v.put("reviewNotes", "We couldn't read the teaching certificate you uploaded. Please add a clear scan of it.");
            }
            case TRAINING_RATE_UPDATE_REJECTED -> {
                v.put("approved", false);
                v.put("decisionLabel", "not accepted");
                v.put("reviewNotes", "This course is priced for beginners; we can revisit the rate next term.");
            }
            case ORGANISATION_INVITATION_ACCEPTED -> v.put("recipientName", "Brian Kamau");
            case NEW_ASSIGNMENT_SUBMISSION, CLASS_SCHEDULE_UPDATED, CLASS_MARKETPLACE_JOB_APPLICATION_WITHDRAWN,
                 CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_ORGANISATION -> v.put("instructorName", "Brian Kamau");
            default -> {
            }
        }
        if (type.name().startsWith("CLASS_MARKETPLACE")) {
            v.put("contextType", "class");
        }
        return v;
    }

    /** One value per variable any template reads, typed the way publishers send them. */
    public static Map<String, Object> sampleVariables() {
        LocalDateTime soon = LocalDateTime.now().plusDays(2);
        Map<String, Object> v = new HashMap<>();
        Map<String, String> text = Map.ofEntries(
                Map.entry("contextName", "Piano Foundations: Grade 1"),
                Map.entry("reviewNotes", "Great work on the practical evidence. Keep the rhythm steady."),
                Map.entry("courseName", "Piano Foundations: Grade 1"),
                Map.entry("assignmentTitle", "Week 3 Scales Recording"),
                Map.entry("organisationName", "Nairobi Music Academy"),
                Map.entry("organizationName", "Nairobi Music Academy"),
                Map.entry("instructorName", "Brian Kamau"),
                Map.entry("statusLabel", "Approved"),
                Map.entry("contextType", "course"),
                Map.entry("studentName", "Amina Otieno"),
                Map.entry("roleName", "Instructor"),
                Map.entry("orderDisplayId", "ELM-10482"),
                Map.entry("orderId", "ELM-10482"),
                Map.entry("gradeLevel", "excellent"),
                Map.entry("currencyCode", "KES"),
                Map.entry("actionLink", "https://elimika.sarafrika.com/dashboard"),
                Map.entry("guardianName", "Grace Otieno"),
                Map.entry("domainLabel", "course creator"),
                Map.entry("courseImageUrl", "https://elimika.sarafrika.com/assets/course-cover.jpg"),
                Map.entry("changeTypeLabel", "rescheduled"),
                Map.entry("assignmentId", "a1b2c3"),
                Map.entry("title", "Studio closed on Friday"),
                Map.entry("timezone", "Africa/Nairobi"),
                Map.entry("submissionText", "Attached is my recording of the C major and G major scales, hands together."),
                Map.entry("releaseStrategy", "AFTER_LESSON"),
                Map.entry("relationshipLabel", "parent"),
                Map.entry("platformFeeCurrency", "KES"),
                Map.entry("personalMessage", "We would love to have you teach our weekend piano classes."),
                Map.entry("notes", "Bring your own headphones."),
                Map.entry("invitationLink", "https://elimika.sarafrika.com/invitations/abc123"),
                Map.entry("instructor", "Brian Kamau"),
                Map.entry("dashboard", "https://elimika.sarafrika.com/dashboard"),
                Map.entry("consentLink", "https://elimika.sarafrika.com/guardian-consent/abc123"),
                Map.entry("changedBy", "Wanjiru Mwangi"),
                Map.entry("branchName", "Westlands Studio"),
                Map.entry("assessmentType", "ASSIGNMENT"),
                Map.entry("assessmentTitle", "Week 3 Scales Recording"),
                Map.entry("welcomeMessage", "Welcome aboard! Your first lesson starts this Saturday."),
                Map.entry("submissionId", "s-204"),
                Map.entry("studentEmail", "amina.otieno@example.com"),
                Map.entry("paymentStatus", "Paid"),
                Map.entry("loginUrl", "https://elimika.sarafrika.com/login"),
                Map.entry("inviterName", "Wanjiru Mwangi"),
                Map.entry("instructorComments", "Clean technique and good tone. Work on the left hand evenness."),
                Map.entry("decisionLabel", "approved"),
                Map.entry("courseId", "c-101"),
                Map.entry("courseDescription", "A friendly introduction to piano technique, reading and performance."),
                Map.entry("body", "The studio will be closed this Friday for maintenance. Classes resume on Saturday."),
                Map.entry("reason", "Overlaps another class on Monday at 10:00"));
        v.putAll(text);
        v.put("actionPath", "/dashboard");
        v.put("approved", true);
        v.put("isUrgent", false);
        v.put("isLate", false);
        v.put("isStartingSoon", true);
        v.put("hasFeedback", true);
        v.put("hasAttachments", true);
        v.put("daysUntilDue", 2);
        v.put("clashCount", 2);
        v.put("estimatedDurationWeeks", 6);
        for (String date : List.of("dueDate", "dueAt", "createdAt", "visibleAt", "expiresAt", "firstClashAt",
                "changedAt", "submittedAt", "courseStartDate")) {
            v.put(date, soon);
        }
        v.put("score", 17);
        v.put("maxScore", 20);
        v.put("percentage", 85);
        v.put("subtotal", new BigDecimal("13500.00"));
        v.put("platformFeeAmount", new BigDecimal("270.00"));
        v.put("total", new BigDecimal("13770.00"));
        v.put("items", List.of(
                Map.of("title", "Piano Foundations: Grade 1", "quantity", 1, "total", new BigDecimal("12000.00")),
                Map.of("title", "Grade 1 practice pack", "quantity", 1, "total", new BigDecimal("1500.00"))));
        v.put("clashReasons", List.of("Sat 10:00-12:00: overlaps Violin Basics at Kilimani Strings", "Sat 25 Oct 10:00-12:00: overlaps Violin Basics at Kilimani Strings"));
        v.put("attachmentFileNames", List.of("c-major-scale.m4a", "hands-together.mp4"));
        return v;
    }
}
