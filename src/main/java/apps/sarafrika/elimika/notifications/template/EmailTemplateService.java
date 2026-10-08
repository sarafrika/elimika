package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Service for generating email content using Thymeleaf templates.
 * Uses templates from resources/templates/email/ following the existing design system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {
    
    private final TemplateEngine templateEngine;
    
    @Value("${app.email.from-name:Elimika}")
    private String applicationName;
    
    @Value("${app.email.frontend.url:https://elimika.sarafrika.com}")
    private String frontendUrl;

    private EmailFormatter formatter = new EmailFormatter(ZoneId.of("Africa/Nairobi"));

    /** Colour sets per tone: the header chip, hero tint and accent text all come from one. */
    private static final Map<String, Map<String, String>> TONES = Map.of(
            "blue", Map.of("tint", "#F3F7FF", "chip", "#EAF1FF", "text", "#0047B3", "solid", "#0061ED"),
            "green", Map.of("tint", "#F0FAF5", "chip", "#E8F6EF", "text", "#0B6B45", "solid", "#0E7A4F"),
            "amber", Map.of("tint", "#FFF8EB", "chip", "#FFF1DC", "text", "#8A3F06", "solid", "#B4540A"),
            "slate", Map.of("tint", "#F5F6F8", "chip", "#EEF0F3", "text", "#344054", "solid", "#475467"),
            "violet", Map.of("tint", "#F7F4FF", "chip", "#F1EDFD", "text", "#5B33B6", "solid", "#6941C6"));

    @Value("${app.email.display-zone:Africa/Nairobi}")
    void setDisplayZone(String zone) {
        this.formatter = new EmailFormatter(ZoneId.of(zone));
    }
    
    /**
     * Generate email subject line for a notification
     */
    public String generateSubject(NotificationEvent event) {
        Map<String, Object> vars = event.getTemplateVariables() == null ? Map.of() : event.getTemplateVariables();
        return switch (event.getNotificationType().getTemplateName()) {
            case "assignment-due-reminder" -> dueSubject(vars);
            case "assignment-graded" -> vars.get("score") != null && vars.get("maxScore") != null
                    ? String.format("You scored %s/%s on %s", vars.get("score"), vars.get("maxScore"),
                            value(vars, "assignmentTitle", "your assignment"))
                    : String.format("Graded: %s", value(vars, "assignmentTitle", "your assignment"));
            case "course-enrollment-welcome" ->
                    String.format("You're enrolled: %s", value(vars, "courseName", "your new course"));
            case "new-assignment-submission" ->
                    String.format("%s submitted %s", value(vars, "studentName", "A student"),
                            value(vars, "assignmentTitle", "an assignment"));
            case "class-schedule-updated" ->
                    String.format("%s: %s", formatter.capitalize(value(vars, "changeTypeLabel", "updated")),
                            value(vars, "assessmentTitle", "an assessment"));
            case "order-payment-receipt" ->
                    String.format("Receipt for order %s", value(vars, "orderDisplayId", value(vars, "orderId", "")).trim());
            case "account-created" -> {
                String first = formatter.firstName(event.getRecipientName());
                yield first.isBlank() ? "Welcome to " + applicationName : "Welcome to " + applicationName + ", " + first;
            }
            case "training-application-status" ->
                    String.format("Update on your application: %s", value(vars, "contextName", "training"));
            case "training-rate-update-decision" -> Boolean.TRUE.equals(vars.get("approved"))
                    ? String.format("Your updated rate for %s was approved", value(vars, "contextName", "your training"))
                    : String.format("Your rate change for %s was not approved", value(vars, "contextName", "your training"));
            case "class-marketplace-job-application-update" ->
                    String.format("Your application moved forward: %s", value(vars, "contextName", "a class"));
            case "class-marketplace-job-hired" ->
                    String.format("You've been hired to train %s", value(vars, "contextName", "a class"));
            case "class-marketplace-job-application-withdrawn" ->
                    String.format("%s withdrew from %s", value(vars, "instructorName", "An instructor"),
                            value(vars, "contextName", "your class job"));
            case "class-marketplace-job-hire-blocked-organisation" ->
                    String.format("Couldn't hire %s: schedule clash", value(vars, "instructorName", "the instructor"));
            case "class-marketplace-job-hire-blocked-instructor" ->
                    String.format("A schedule clash stopped your hire for %s", value(vars, "contextName", "a class"));
            case "organisation-invitation" ->
                    String.format("%s invited you to join them on %s", value(vars, "organisationName", "An organisation"),
                            applicationName);
            case "guardian-consent-request" ->
                    String.format("Your approval is needed for %s to join %s", value(vars, "studentName", "your child"),
                            value(vars, "organisationName", "an organisation"));
            case "guardian-link-invitation" ->
                    String.format("%s named you as their parent or guardian on %s", value(vars, "studentName", "A learner"),
                            applicationName);
            case "guardian-link-established" ->
                    String.format("You can now follow %s's learning on %s", value(vars, "studentName", "your child"),
                            applicationName);
            case "invitation-accepted" ->
                    String.format("%s joined %s", value(vars, "recipientName", "Someone"),
                            value(vars, "organisationName", "your organisation"));
            case "organisation-announcement" -> value(vars, "title", event.getNotificationType().getDisplayName());
            case "domain-approval-decision" -> Boolean.TRUE.equals(vars.get("approved"))
                    ? String.format("You're approved as %s on %s", article(value(vars, "domainLabel", "member")), applicationName)
                    : String.format("Your %s application needs another look", value(vars, "domainLabel", "account"));
            default -> String.format("[%s] %s", applicationName, event.getNotificationType().getDisplayName());
        };
    }

    private String dueSubject(Map<String, Object> vars) {
        String title = value(vars, "assignmentTitle", "your assignment");
        Object days = vars.get("daysUntilDue");
        int remaining = days instanceof Number number ? number.intValue() : -1;
        return switch (remaining) {
            case 0 -> "Due today: " + title;
            case 1 -> "Due tomorrow: " + title;
            case -1 -> "Due soon: " + title;
            default -> "Due in " + remaining + " days: " + title;
        };
    }

    private static String value(Map<String, Object> vars, String key, String fallback) {
        Object value = vars.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private static String article(String noun) {
        return ("aeiou".indexOf(Character.toLowerCase(noun.charAt(0))) >= 0 ? "an " : "a ") + noun;
    }

    /**
     * Generate HTML email content using Thymeleaf templates
     */
    public String generateEmailContent(NotificationEvent event) {
        Context context = createEmailContext(event);
        
        try {
            String templatePath = event.getNotificationType().getEmailTemplatePath();
            return templateEngine.process(templatePath, context);
        } catch (Exception e) {
            log.error("Failed to generate email content for {}: {}", 
                event.getNotificationType(), e.getMessage());
            throw new RuntimeException("Email template processing failed", e);
        }
    }
    
    /**
     * Create Thymeleaf context with all necessary variables
     */
    private Context createEmailContext(NotificationEvent event) {
        Context context = new Context();
        
        // Add system variables
        context.setVariable("applicationName", applicationName);
        context.setVariable("companyName", applicationName);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("currentYear", java.time.Year.now().getValue());
        context.setVariable("supportEmail", "support@sarafrika.com");
        context.setVariable("logoUrl", frontendUrl + "/assets/logo.png");
        context.setVariable("fmt", formatter);
        context.setVariable("tones", TONES);
        
        // Add recipient information
        context.setVariable("recipientName", event.getRecipientName());
        context.setVariable("recipientEmail", event.getRecipientEmail());
        
        // Add all template-specific variables
        Map<String, Object> templateVars = normalizeTemplateVariables(event.getTemplateVariables());
        templateVars.forEach(context::setVariable);
        
        return context;
    }

    private Map<String, Object> normalizeTemplateVariables(Map<String, Object> templateVars) {
        if (templateVars == null || templateVars.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new HashMap<>(templateVars);
        if (normalized.containsKey("createdAt")) {
            normalized.put("createdAt", normalizeCreatedAt(normalized.get("createdAt")));
        }
        return normalized;
    }

    private Object normalizeCreatedAt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime || value instanceof LocalDateTime) {
            return value;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof String raw) {
            String text = raw.trim();
            if (text.isEmpty()) {
                return raw;
            }
            Optional<OffsetDateTime> parsed = parseOffsetDateTime(text);
            return parsed.map(parsedValue -> (Object) parsedValue).orElse(raw);
        }
        return value;
    }

    private Optional<OffsetDateTime> parseOffsetDateTime(String text) {
        try {
            return Optional.of(OffsetDateTime.parse(text));
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return Optional.of(Instant.parse(text).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return Optional.of(LocalDateTime.parse(text).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT);
            return Optional.of(LocalDateTime.parse(text, formatter).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}
