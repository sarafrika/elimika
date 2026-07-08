package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.*;
import apps.sarafrika.elimika.notifications.model.NotificationPreferencesRepository;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.notifications.service.EmailNotificationService;
import apps.sarafrika.elimika.notifications.service.UserNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    @Mock
    private NotificationPreferencesService preferencesService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void sendNotification_WithInAppAndEmail_SendsBothSuccessfully() throws Exception {
        // Arrange
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String email = "test@example.com";

        NotificationEvent event = mock(NotificationEvent.class);
        when(event.getNotificationId()).thenReturn(notificationId);
        when(event.getRecipientId()).thenReturn(recipientId);
        when(event.getRecipientEmail()).thenReturn(email);
        when(event.getDeliveryChannels()).thenReturn(Set.of("in_app", "email"));
        when(event.getNotificationType()).thenReturn(NotificationType.CLASS_SCHEDULE_UPDATED);

        DeliveryOptions options = DeliveryOptions.defaults();

        when(preferencesService.isNotificationEnabled(recipientId, NotificationType.CLASS_SCHEDULE_UPDATED, "in_app"))
                .thenReturn(true);
        when(preferencesService.isNotificationEnabled(recipientId, NotificationType.CLASS_SCHEDULE_UPDATED, "email"))
                .thenReturn(true);

        when(emailNotificationService.sendEmail(event))
                .thenReturn(CompletableFuture.completedFuture(NotificationResult.success(notificationId, "email")));

        // Act
        CompletableFuture<NotificationResult> futureResult = notificationService.sendNotification(event, options);
        NotificationResult result = futureResult.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.channel()).isEqualTo("email");

        verify(userNotificationService).createFromEvent(event);
        verify(emailNotificationService).sendEmail(event);
    }
}
