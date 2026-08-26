package apps.sarafrika.elimika.notifications.service;

import apps.sarafrika.elimika.notifications.api.DeliveryStatus;
import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationResult;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.model.NotificationDeliveryLog;
import apps.sarafrika.elimika.notifications.model.NotificationDeliveryLogRepository;
import apps.sarafrika.elimika.notifications.template.EmailTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private EmailTemplateService templateService;

    @Mock
    private NotificationDeliveryLogRepository deliveryLogRepository;

    @Test
    void sendEmailFailsSoftlyWhenMailSenderIsUnavailable() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        NotificationEvent event = new TestNotificationEvent(notificationId, recipientId);
        EmailNotificationService service = new EmailNotificationService(
                mailSenderProvider,
                templateService,
                deliveryLogRepository);

        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(deliveryLogRepository.save(any(NotificationDeliveryLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResult result = service.sendEmail(event).get();

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.errorMessage()).isEqualTo("Email service is not configured");
        verify(templateService, never()).generateSubject(any());
        verify(templateService, never()).generateEmailContent(any());
    }

    private record TestNotificationEvent(
            UUID notificationId,
            UUID recipientId
    ) implements NotificationEvent {

        @Override
        public UUID getNotificationId() {
            return notificationId;
        }

        @Override
        public UUID getRecipientId() {
            return recipientId;
        }

        @Override
        public String getRecipientEmail() {
            return "learner@example.com";
        }

        @Override
        public String getRecipientName() {
            return "Learner";
        }

        @Override
        public NotificationType getNotificationType() {
            return NotificationType.CLASS_SCHEDULE_UPDATED;
        }

        @Override
        public NotificationPriority getPriority() {
            return NotificationPriority.NORMAL;
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return LocalDateTime.now();
        }

        @Override
        public Map<String, Object> getTemplateVariables() {
            return Map.of();
        }
    }
}
