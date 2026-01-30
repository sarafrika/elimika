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
    
    /**
     * Generate email subject line for a notification
     */
    public String generateSubject(NotificationEvent event) {
        return switch (event.getNotificationType().getTemplateName()) {
            case "assignment-due-reminder" -> 
                String.format("Assignment Due: %s", 
                    event.getTemplateVariables().getOrDefault("assignmentTitle", "Assignment"));
            case "assignment-graded" -> 
                String.format("Assignment Graded: %s", 
                    event.getTemplateVariables().getOrDefault("assignmentTitle", "Assignment"));
            case "course-enrollment-welcome" -> 
                String.format("Welcome to %s!", 
                    event.getTemplateVariables().getOrDefault("courseName", "your new course"));
            case "new-assignment-submission" ->
                String.format("New Assignment Submission: %s",
                    event.getTemplateVariables().getOrDefault("assignmentTitle", "Assignment"));
            case "class-schedule-updated" ->
                String.format("Class schedule %s: %s",
                        event.getTemplateVariables().getOrDefault("changeTypeLabel", "updated"),
                        event.getTemplateVariables().getOrDefault("assessmentTitle", "Assessment"));
            case "order-payment-receipt" ->
                String.format("Receipt for Order %s",
                        event.getTemplateVariables().getOrDefault("orderDisplayId",
                                event.getTemplateVariables().getOrDefault("orderId", "payment")));
            default -> 
                String.format("[%s] %s", applicationName, event.getNotificationType().getDisplayName());
        };
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
            return parsed.<Object>map(value -> value).orElse(raw);
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
