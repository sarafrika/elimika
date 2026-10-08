package apps.sarafrika.elimika.notifications.service;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationResult;
import apps.sarafrika.elimika.notifications.api.DeliveryStatus;
import apps.sarafrika.elimika.notifications.model.NotificationDeliveryLog;
import apps.sarafrika.elimika.notifications.model.NotificationDeliveryLogRepository;
import apps.sarafrika.elimika.notifications.template.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending email notifications using the existing mail infrastructure.
 * Integrates with the EmailTemplateService for dynamic content generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {
    
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailTemplateService templateService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;
    
    @Value("${app.email.from:no-reply@sarafrika.com}")
    private String fromEmail;
    
    @Value("${app.email.from-name:Elimika}")
    private String fromName;
    
    /**
     * Send an email notification asynchronously
     */
    public CompletableFuture<NotificationResult> sendEmail(NotificationEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendEmailSync(event);
            } catch (Exception e) {
                log.error("Failed to send email notification {}: {}", event.getNotificationId(), e.getMessage(), e);
                return handleEmailFailure(event, e.getMessage());
            }
        });
    }
    
    /**
     * Send email notification synchronously
     */
    private NotificationResult sendEmailSync(NotificationEvent event) {
        log.debug("Sending email notification {} to {}", event.getNotificationId(), event.getRecipientEmail());
        
        // Create delivery log entry
        NotificationDeliveryLog deliveryLog = NotificationDeliveryLog.builder()
            .notificationId(event.getNotificationId())
            .userUuid(event.getRecipientId())
            .recipientEmail(event.getRecipientEmail())
            .notificationType(event.getNotificationType())
            .priority(event.getPriority())
            .deliveryChannel("email")
            .deliveryStatus(DeliveryStatus.PENDING)
            .templateUsed(event.getNotificationType().getTemplateName())
            .organizationUuid(event.getOrganizationId())
            .build();
        
        deliveryLog = deliveryLogRepository.save(deliveryLog);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(mailHost)) {
            return failDelivery(deliveryLog, event, "Email service is not configured");
        }
        
        try {
            // Generate email content from template
            String subject = templateService.generateSubject(event);
            String htmlContent = templateService.generateEmailContent(event);
            
            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            try {
                helper.setFrom(fromEmail, fromName);
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(event.getRecipientEmail());
            helper.setSubject(subject);
            helper.setText(EmailPlainText.from(htmlContent), htmlContent);
            
            // Add reply-to if organization context exists
            if (event.getOrganizationId() != null) {
                helper.setReplyTo(fromEmail); // Could be customized per organization
            }
            
            // Attach logos as inline content
            attachLogos(helper);
            
            mailSender.send(message);
            
            // Update delivery log as successful
            deliveryLog.markAsDelivered();
            deliveryLogRepository.save(deliveryLog);
            
            log.info("Email notification {} sent successfully to {}", 
                event.getNotificationId(), event.getRecipientEmail());
            
            return NotificationResult.success(event.getNotificationId(), "email");
            
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email notification {}: {}", event.getNotificationId(), e.getMessage());
            return failDelivery(deliveryLog, event, e.getMessage());
        }
    }

    private NotificationResult failDelivery(NotificationDeliveryLog deliveryLog,
                                            NotificationEvent event,
                                            String errorMessage) {
        deliveryLog.markAsFailed(errorMessage);
        deliveryLogRepository.save(deliveryLog);
        return NotificationResult.failed(event.getNotificationId(), "email", errorMessage);
    }
    
    /**
     * Handle email sending failure
     */
    private NotificationResult handleEmailFailure(NotificationEvent event, String errorMessage) {
        // Try to find existing delivery log or create new one
        NotificationDeliveryLog deliveryLog = deliveryLogRepository
            .findByNotificationId(event.getNotificationId())
            .orElse(NotificationDeliveryLog.builder()
                .notificationId(event.getNotificationId())
                .userUuid(event.getRecipientId())
                .recipientEmail(event.getRecipientEmail())
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .deliveryChannel("email")
                .deliveryStatus(DeliveryStatus.FAILED)
                .organizationUuid(event.getOrganizationId())
                .build());
        
        deliveryLog.markAsFailed(errorMessage);
        deliveryLogRepository.save(deliveryLog);
        
        return NotificationResult.failed(event.getNotificationId(), "email", errorMessage);
    }
    
    /**
     * Attach logos as inline PNGs; Gmail and Outlook do not render SVG images.
     */
    private void attachLogos(MimeMessageHelper helper) {
        attachLogo(helper, "elimikaLogo", "static/logos/elimika/elimika-logo-email.png");
        attachLogo(helper, "sarafrikaLogo", "static/logos/sarafrika/sarafrika-logo-email.png");
    }

    private void attachLogo(MimeMessageHelper helper, String contentId, String path) {
        Resource logo = new ClassPathResource(path);
        if (!logo.exists()) {
            log.warn("Email logo not found at: {}", path);
            return;
        }
        try {
            helper.addInline(contentId, logo, "image/png");
        } catch (MessagingException e) {
            // The email still reads fine without the logo.
            log.error("Failed to attach {} to email: {}", path, e.getMessage());
        }
    }

    /**
     * Check if email service is available
     */
    public boolean isAvailable() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(mailHost)) {
            return false;
        }
        try {
            // Simple connectivity check
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("Email service unavailable: {}", e.getMessage());
            return false;
        }
    }
}
