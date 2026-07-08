package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        templateEngine = mock(TemplateEngine.class);
        emailTemplateService = new EmailTemplateService(templateEngine);
    }

    @Test
    void generateSubject_ForClassScheduleUpdated() {
        // Arrange
        NotificationEvent event = mock(NotificationEvent.class);
        when(event.getNotificationType()).thenReturn(NotificationType.CLASS_SCHEDULE_UPDATED);
        
        Map<String, Object> vars = Map.of(
            "changeTypeLabel", "rescheduled",
            "assessmentTitle", "Midterm Exam"
        );
        when(event.getTemplateVariables()).thenReturn(vars);

        // Act
        String subject = emailTemplateService.generateSubject(event);

        // Assert
        assertThat(subject).isEqualTo("Class schedule rescheduled: Midterm Exam");
    }

    @Test
    void generateEmailContent_ForClassScheduleUpdated() {
        // Arrange
        NotificationEvent event = mock(NotificationEvent.class);
        when(event.getNotificationType()).thenReturn(NotificationType.CLASS_SCHEDULE_UPDATED);
        when(event.getRecipientName()).thenReturn("John Doe");
        when(event.getRecipientEmail()).thenReturn("john@example.com");

        Map<String, Object> vars = new HashMap<>();
        vars.put("changeTypeLabel", "rescheduled");
        vars.put("assessmentTitle", "Midterm Exam");
        when(event.getTemplateVariables()).thenReturn(vars);

        when(templateEngine.process(eq("email/class-schedule-updated.html"), any(Context.class)))
                .thenReturn("HTML Content");

        // Act
        String content = emailTemplateService.generateEmailContent(event);

        // Assert
        assertThat(content).isEqualTo("HTML Content");
    }
}
