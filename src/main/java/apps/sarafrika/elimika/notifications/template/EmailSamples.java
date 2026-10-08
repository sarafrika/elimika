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

    /** One value per variable any template reads, typed the way publishers send them. */
    public static Map<String, Object> sampleVariables() {
        LocalDateTime soon = LocalDateTime.now().plusDays(2);
        Map<String, Object> v = new HashMap<>();
        for (String text : List.of("contextName", "reviewNotes", "courseName", "assignmentTitle", "organisationName",
                "organizationName", "instructorName", "statusLabel", "contextType", "studentName", "roleName",
                "orderDisplayId", "orderId", "gradeLevel", "currencyCode", "actionPath", "actionLink", "guardianName",
                "domainLabel", "courseImageUrl", "changeTypeLabel", "assignmentId", "title", "timezone",
                "submissionText", "releaseStrategy", "relationshipLabel", "platformFeeCurrency", "personalMessage",
                "notes", "invitationLink", "instructor", "dashboard", "consentLink", "changedBy", "branchName",
                "assessmentType", "assessmentTitle", "welcomeMessage", "submissionId", "studentEmail", "paymentStatus",
                "loginUrl", "inviterName", "instructorComments", "decisionLabel", "courseId", "courseDescription",
                "body", "reason")) {
            v.put(text, "Sample " + text);
        }
        v.put("actionPath", "/dashboard");
        v.put("approved", true);
        v.put("isUrgent", true);
        v.put("isLate", false);
        v.put("isStartingSoon", true);
        v.put("hasFeedback", true);
        v.put("hasAttachments", true);
        v.put("daysUntilDue", 2);
        v.put("clashCount", 1);
        v.put("estimatedDurationWeeks", 6);
        for (String date : List.of("dueDate", "dueAt", "createdAt", "visibleAt", "expiresAt", "firstClashAt",
                "changedAt", "submittedAt", "courseStartDate")) {
            v.put(date, soon);
        }
        for (String amount : List.of("score", "maxScore", "percentage", "total", "subtotal", "platformFeeAmount")) {
            v.put(amount, new BigDecimal("85.50"));
        }
        v.put("items", List.of(Map.of("title", "Piano Basics", "quantity", 1, "total", new BigDecimal("1500.00"))));
        v.put("clashReasons", List.of("Overlaps another class on Monday 10:00"));
        v.put("attachmentFileNames", List.of("essay.pdf"));
        return v;
    }
}
