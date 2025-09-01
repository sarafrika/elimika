package apps.sarafrika.elimika.notifications.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for the notifications module.
 * Enables async processing for notification handling.
 */
@Configuration
@EnableAsync
public class NotificationConfig {
    
    // Configuration will be extended as needed for the notification system
    // For now, we just enable async processing which is required for
    // non-blocking notification delivery
}