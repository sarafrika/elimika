package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

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
        Map<String, Object> templateVars = event.getTemplateVariables();
        templateVars.forEach(context::setVariable);
        
        return context;
    }
}
