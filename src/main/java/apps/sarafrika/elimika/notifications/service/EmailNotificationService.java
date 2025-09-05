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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
    
    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    
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
            helper.setText(htmlContent, true);
            
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
            
        } catch (MessagingException e) {
            log.error("Failed to send email notification {}: {}", event.getNotificationId(), e.getMessage());
            deliveryLog.markAsFailed(e.getMessage());
            deliveryLogRepository.save(deliveryLog);
            return NotificationResult.failed(event.getNotificationId(), "email", e.getMessage());
        }
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
     * Attach logos as inline content for email templates
     */
    private void attachLogos(MimeMessageHelper helper) {
        try {
            // Attach Elimika full color logo
            Resource elimikaLogo = new ClassPathResource("static/logos/elimika/elimika-logo-full-color.svg");
            if (elimikaLogo.exists()) {
                helper.addInline("elimikaLogo", elimikaLogo, "image/svg+xml");
                log.debug("Attached Elimika logo to email");
            } else {
                log.warn("Elimika logo not found at: static/logos/elimika/elimika-logo-full-color.svg");
            }
            
            // Attach Sarafrika full color logo
            Resource sarafrikaLogo = new ClassPathResource("static/logos/sarafrika/sarafrika-logo-full-color.svg");
            if (sarafrikaLogo.exists()) {
                helper.addInline("sarafrikaLogo", sarafrikaLogo, "image/svg+xml");
                log.debug("Attached Sarafrika logo to email");
            } else {
                log.warn("Sarafrika logo not found at: static/logos/sarafrika/sarafrika-logo-full-color.svg");
            }
            
        } catch (MessagingException e) {
            log.error("Failed to attach logos to email: {}", e.getMessage());
            // Don't throw exception - email can still be sent without logos
        }
    }
    
    /**
     * Check if email service is available
     */
    public boolean isAvailable() {
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