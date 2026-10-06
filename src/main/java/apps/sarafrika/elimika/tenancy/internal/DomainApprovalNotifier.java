package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Tells platform admins a domain awaits approval, and the user how their request was decided. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainApprovalNotifier {

    public static final String REQUESTED = "DOMAIN_APPROVAL_REQUESTED";
    public static final String GRANTED = "DOMAIN_APPROVAL_GRANTED";
    public static final String DECLINED = "DOMAIN_APPROVAL_DECLINED";

    static final String ADMIN_QUEUE_URL = "/dashboard/admin/registrations";
    static final String SIGN_IN_URL = "/login";

    private final UserDomainMappingRepository mappingRepository;
    private final UserDomainRepository domainRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void requested(UUID userUuid, String domain) {
        try {
            String who = userRepository.findByUuid(userUuid).map(DomainApprovalNotifier::fullName).orElse("A new user");
            UUID adminDomain = domainRepository.findByDomainName("admin").map(d -> d.getUuid()).orElse(null);
            if (adminDomain == null) {
                return;
            }
            for (UserDomainMapping admin : mappingRepository.findByUserDomainUuid(adminDomain)) {
                if (!admin.isApproved()) {
                    continue;
                }
                eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                        admin.getUserUuid(), REQUESTED, "INBOX",
                        "Account awaiting approval",
                        who + " registered as " + label(domain) + " and is waiting for approval.",
                        ADMIN_QUEUE_URL,
                        Map.of("user_uuid", userUuid, "domain", domain),
                        "domain-approval-requested:" + userUuid + ":" + domain + ":" + admin.getUserUuid()));
            }
        } catch (Exception e) {
            log.warn("Failed to notify admins that user {} awaits {} approval: {}", userUuid, domain, e.getMessage());
        }
    }

    public void decided(UUID userUuid, String domain, DomainApprovalStatus status, String reason) {
        if (status != DomainApprovalStatus.APPROVED && status != DomainApprovalStatus.REJECTED
                && status != DomainApprovalStatus.SUSPENDED) {
            return;
        }
        boolean approved = status == DomainApprovalStatus.APPROVED;
        String type = approved ? GRANTED : DECLINED;
        try {
            String body = approved
                    ? "Your " + label(domain) + " account is approved. You can now open your dashboard."
                    : "Your " + label(domain) + " account was not approved." + (reason == null ? "" : " " + reason);
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    userUuid, type, "POPUP",
                    approved ? "Account approved" : "Account not approved",
                    body, SIGN_IN_URL,
                    Map.of("domain", domain, "status", status.getValue()),
                    "domain-approval-decision:" + userUuid + ":" + domain + ":" + status.getValue()));
            userRepository.findByUuid(userUuid).ifPresent(user -> email(user, type, domain, approved, reason));
        } catch (Exception e) {
            log.warn("Failed to notify user {} of the {} decision: {}", userUuid, domain, e.getMessage());
        }
    }

    private void email(User user, String type, String domain, boolean approved, String reason) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", fullName(user));
        variables.put("domainLabel", label(domain));
        variables.put("approved", approved);
        variables.put("reviewNotes", reason == null ? "" : reason);
        variables.put("actionPath", SIGN_IN_URL);
        eventPublisher.publishEvent(NotificationRequestedEvent.email(user.getUuid(), user.getEmail(), fullName(user),
                type, variables));
    }

    private static String fullName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }

    private static String label(String domain) {
        return domain == null ? "member" : domain.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
