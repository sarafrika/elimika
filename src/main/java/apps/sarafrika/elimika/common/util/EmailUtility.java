package apps.sarafrika.elimika.common.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Email utility service for sending HTML emails with templates.
 * Handles organization invitations, notifications, and other email communications.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-07-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailUtility {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from:no-reply@sarafrika.com}")
    private String fromEmail;

    @Value("${app.email.from-name:Elimika}")
    private String fromName;

    @Value("${app.frontend.url:https://elimika.sarafrika.com}")
    private String frontendUrl;

    @Value("${app.company.name:Sarafrika}")
    private String companyName;

    // ================================
    // ORGANIZATION INVITATION EMAILS
    // ================================

    /**
     * Sends an organization invitation email to a user.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param organizationName the name of the inviting organization
     * @param organizationDomain the organization's domain/website
     * @param domainName the role/domain being offered (student, instructor, admin, organisation_user)
     * @param branchName optional training branch name (can be null)
     * @param inviterName the name of the person sending the invitation
     * @param invitationToken the unique token for accepting the invitation
     * @throws MessagingException if email sending fails
     */
    public void sendOrganizationInvitation(
            String recipientEmail,
            String recipientName,
            String organizationName,
            String organizationDomain,
            String domainName,
            String branchName,
            String inviterName,
            String invitationToken,
            String notes) throws MessagingException {

        log.debug("Sending organization invitation to {} for organization {}", recipientEmail, organizationName);

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("organizationName", organizationName);
        context.setVariable("organizationDomain", organizationDomain);
        context.setVariable("roleName", formatRoleName(domainName));
        context.setVariable("branchName", branchName);
        context.setVariable("inviterName", inviterName);
        context.setVariable("companyName", companyName);
        context.setVariable("acceptUrl", buildAcceptInvitationUrl(invitationToken));
        context.setVariable("declineUrl", buildDeclineInvitationUrl(invitationToken));
        context.setVariable("logoUrl", buildLogoUrl());
        context.setVariable("notes", notes);

        String subject = String.format("Invitation to join %s on %s", organizationName, companyName);
        String htmlContent = templateEngine.process("emails/organization-invitation", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);

        log.info("Organization invitation sent to {} for organization {}", recipientEmail, organizationName);
    }

    /**
     * Sends a branch-specific invitation email to a user.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param organizationName the name of the organization
     * @param branchName the name of the training branch
     * @param branchAddress the address of the training branch
     * @param domainName the role/domain being offered
     * @param inviterName the name of the person sending the invitation
     * @param invitationToken the unique token for accepting the invitation
     * @throws MessagingException if email sending fails
     */
    public void sendBranchInvitation(
            String recipientEmail,
            String recipientName,
            String organizationName,
            String branchName,
            String branchAddress,
            String domainName,
            String inviterName,
            String invitationToken,
            String notes) throws MessagingException {

        log.debug("Sending branch invitation to {} for branch {} in organization {}",
                recipientEmail, branchName, organizationName);

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("organizationName", organizationName);
        context.setVariable("branchName", branchName);
        context.setVariable("branchAddress", branchAddress);
        context.setVariable("roleName", formatRoleName(domainName));
        context.setVariable("inviterName", inviterName);
        context.setVariable("companyName", companyName);
        context.setVariable("acceptUrl", buildAcceptInvitationUrl(invitationToken));
        context.setVariable("declineUrl", buildDeclineInvitationUrl(invitationToken));
        context.setVariable("logoUrl", buildLogoUrl());
        context.setVariable("notes", notes);

        String subject = String.format("Invitation to join %s - %s on %s", organizationName, branchName, companyName);
        String htmlContent = templateEngine.process("emails/branch-invitation", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);

        log.info("Branch invitation sent to {} for branch {} in organization {}",
                recipientEmail, branchName, organizationName);
    }

    // ================================
    // INVITATION STATUS EMAILS
    // ================================

    /**
     * Sends confirmation email when invitation is accepted.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param organizationName the organization name
     * @param branchName optional branch name
     * @param roleName the assigned role
     * @throws MessagingException if email sending fails
     */
    public void sendInvitationAcceptedConfirmation(
            String recipientEmail,
            String recipientName,
            String organizationName,
            String branchName,
            String roleName) throws MessagingException {

        log.debug("Sending invitation acceptance confirmation to {}", recipientEmail);

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("organizationName", organizationName);
        context.setVariable("branchName", branchName);
        context.setVariable("roleName", formatRoleName(roleName));
        context.setVariable("companyName", companyName);
        context.setVariable("loginUrl", buildLoginUrl());
        context.setVariable("logoUrl", buildLogoUrl());

        String subject = String.format("Welcome to %s - Invitation Accepted", organizationName);
        String htmlContent = templateEngine.process("emails/invitation-accepted", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);

        log.info("Invitation acceptance confirmation sent to {}", recipientEmail);
    }

    /**
     * Sends notification to organization admin when invitation is declined.
     *
     * @param adminEmail the admin's email address
     * @param adminName the admin's name
     * @param declinedUserName the name of user who declined
     * @param declinedUserEmail the email of user who declined
     * @param organizationName the organization name
     * @param branchName optional branch name
     * @throws MessagingException if email sending fails
     */
    public void sendInvitationDeclinedNotification(
            String adminEmail,
            String adminName,
            String declinedUserName,
            String declinedUserEmail,
            String organizationName,
            String branchName) throws MessagingException {

        log.debug("Sending invitation declined notification to admin {}", adminEmail);

        Context context = new Context();
        context.setVariable("adminName", adminName);
        context.setVariable("declinedUserName", declinedUserName);
        context.setVariable("declinedUserEmail", declinedUserEmail);
        context.setVariable("organizationName", organizationName);
        context.setVariable("branchName", branchName);
        context.setVariable("companyName", companyName);
        context.setVariable("logoUrl", buildLogoUrl());

        String subject = String.format("Invitation Declined - %s", organizationName);
        String htmlContent = templateEngine.process("emails/invitation-declined", context);

        sendHtmlEmail(adminEmail, subject, htmlContent);

        log.info("Invitation declined notification sent to admin {}", adminEmail);
    }

    /**
     * Sends an invitation expiry reminder email to a user.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param organizationName the name of the inviting organization
     * @param organizationDomain the organization's domain/website (can be null)
     * @param domainName the role/domain being offered
     * @param branchName optional training branch name (can be null)
     * @param inviterName the name of the person who sent the invitation
     * @param invitationToken the unique token for accepting the invitation
     * @param hoursRemaining hours remaining before expiry
     * @throws MessagingException if email sending fails
     */
    public void sendInvitationExpiryReminder(
            String recipientEmail,
            String recipientName,
            String organizationName,
            String organizationDomain,
            String domainName,
            String branchName,
            String inviterName,
            String invitationToken,
            long hoursRemaining) throws MessagingException {

        log.debug("Sending invitation expiry reminder to {} for organization {}", recipientEmail, organizationName);

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("organizationName", organizationName);
        context.setVariable("organizationDomain", organizationDomain);
        context.setVariable("roleName", formatRoleName(domainName));
        context.setVariable("branchName", branchName);
        context.setVariable("inviterName", inviterName);
        context.setVariable("companyName", companyName);
        context.setVariable("acceptUrl", buildAcceptInvitationUrl(invitationToken));
        context.setVariable("declineUrl", buildDeclineInvitationUrl(invitationToken));
        context.setVariable("logoUrl", buildLogoUrl());
        context.setVariable("hoursRemaining", hoursRemaining);

        String subject = String.format("Reminder: Invitation to %s expires in %d hours", organizationName, hoursRemaining);
        String htmlContent = templateEngine.process("emails/invitation-expiry-reminder", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);

        log.info("Invitation expiry reminder sent to {} for organization {}", recipientEmail, organizationName);
    }

    // ================================
    // GENERIC EMAIL METHODS
    // ================================

    /**
     * Sends a generic HTML email with custom template and variables.
     *
     * @param recipientEmail the recipient's email address
     * @param subject the email subject
     * @param templateName the Thymeleaf template name (without .html extension)
     * @param variables the template variables
     * @throws MessagingException if email sending fails
     */
    public void sendTemplatedEmail(
            String recipientEmail,
            String subject,
            String templateName,
            Map<String, Object> variables) throws MessagingException {

        log.debug("Sending templated email to {} using template {}", recipientEmail, templateName);

        Context context = new Context();
        variables.forEach(context::setVariable);

        // Add common variables
        context.setVariable("companyName", companyName);
        context.setVariable("logoUrl", buildLogoUrl());

        String htmlContent = templateEngine.process(templateName, context);
        sendHtmlEmail(recipientEmail, subject, htmlContent);

        log.info("Templated email sent to {} using template {}", recipientEmail, templateName);
    }

    /**
     * Sends an HTML email.
     *
     * @param to the recipient's email address
     * @param subject the email subject
     * @param htmlContent the HTML content
     * @throws MessagingException if email sending fails
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        try {
            helper.setFrom(fromEmail, fromName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Add Elimika logo as embedded resource
        try {
            ClassPathResource logoResource = new ClassPathResource("static/images/elimika-logo.svg");
            if (logoResource.exists()) {
                helper.addInline("elimika-logo", logoResource, "image/svg+xml");
            }
        } catch (Exception e) {
            log.warn("Could not embed logo in email: {}", e.getMessage());
        }

        mailSender.send(message);
        log.debug("HTML email sent to: {}", to);
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Formats role/domain name for display in emails.
     */
    private String formatRoleName(String domainName) {
        if (domainName == null) return "Member";

        return switch (domainName.toLowerCase()) {
            case "student" -> "Student";
            case "instructor" -> "Instructor";
            case "admin" -> "Administrator";
            case "organisation_user" -> "Organization Member";
            default -> "Member";
        };
    }

    /**
     * Builds the URL for accepting invitations.
     */
    private String buildAcceptInvitationUrl(String token) {
        return String.format("%s/invitations/accept?token=%s", frontendUrl, token);
    }

    /**
     * Builds the URL for declining invitations.
     */
    private String buildDeclineInvitationUrl(String token) {
        return String.format("%s/invitations/decline?token=%s", frontendUrl, token);
    }

    /**
     * Builds the URL for user login.
     */
    private String buildLoginUrl() {
        return String.format("%s/login", frontendUrl);
    }

    /**
     * Builds the URL for the company logo.
     */
    private String buildLogoUrl() {
        return String.format("%s/static/images/elimika-logo.svg", frontendUrl);
    }

    /**
     * Validates email address format.
     */
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    /**
     * Generates a unique invitation token.
     */
    public String generateInvitationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void sendOrganisationRegistrationSuccess(String recipientEmail, String userName, String organisationName) throws MessagingException {
        log.debug("Sending organisation registration success email to {}", recipientEmail);

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("organisationName", organisationName);

        String subject = "Organization Registration Successful";
        String htmlContent = templateEngine.process("email/organisation-registration-success", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);
        log.info("Organisation registration success email sent to {}", recipientEmail);
    }

    public void sendOrganisationRegistrationFailure(String recipientEmail, String userName, String organisationName, String errorMessage) throws MessagingException {
        log.debug("Sending organisation registration failure email to {}", recipientEmail);

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("organisationName", organisationName);
        context.setVariable("errorMessage", errorMessage);

        String subject = "Organization Registration Failed";
        String htmlContent = templateEngine.process("email/organisation-registration-failure", context);

        sendHtmlEmail(recipientEmail, subject, htmlContent);
        log.info("Organisation registration failure email sent to {}", recipientEmail);
    }
}