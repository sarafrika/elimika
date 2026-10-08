package apps.sarafrika.elimika.notifications.template;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders every email template through the app's Thymeleaf setup, so a broken template fails the build. */
@SpringBootTest(classes = {ThymeleafAutoConfiguration.class, EmailTemplateService.class})
class EmailTemplatesRenderTest {

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Test
    void everyEmailTemplateRendersWithRealisticVariables() {
        List<String> failures = new ArrayList<>();
        int rendered = 0;
        for (NotificationType type : EmailSamples.typesWithTemplates()) {
            try {
                NotificationEvent event = EmailSamples.event(type, "learner@example.com", EmailSamples.sampleVariables(type));
                assertThat(emailTemplateService.generateSubject(event)).isNotBlank();
                assertThat(emailTemplateService.generateEmailContent(event)).contains("</html>");
                rendered++;
            } catch (Throwable e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                failures.add(type + " (" + type.getEmailTemplatePath() + "): " + root.getMessage());
            }
        }
        assertThat(failures).as("email templates that fail to render").isEmpty();
        assertThat(rendered).isGreaterThan(10);
    }
}
