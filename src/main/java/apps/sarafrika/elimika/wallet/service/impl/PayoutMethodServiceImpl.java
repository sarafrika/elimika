package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.otp.IssuedCode;
import apps.sarafrika.elimika.shared.otp.OneTimeCodePurpose;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.wallet.entity.PayoutApprover;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.repository.PayoutApproverRepository;
import apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository;
import apps.sarafrika.elimika.wallet.repository.WalletAuditEventRepository;
import apps.sarafrika.elimika.wallet.service.PayoutMethodService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutMethodServiceImpl implements PayoutMethodService {

    private static final int MAX_LIVE_METHODS = 5;
    private static final String SECURITY_ALERT = "SECURITY_ALERT";

    private final PayoutMethodRepository methodRepository;
    private final PayoutApproverRepository approverRepository;
    private final WalletAuditEventRepository auditRepository;
    private final OneTimeCodeService oneTimeCodeService;
    private final DomainSecurityService domainSecurityService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${wallet.payout-methods.hold:PT24H}")
    private Duration hold;

    @Value("${app.email.frontend.url:}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public List<PayoutMethod> list(WalletOwner owner) {
        return methodRepository.findLive(owner.type(), owner.uuid(), PayoutMethodStatus.REMOVED);
    }

    @Override
    @Transactional
    public AddedPayoutMethod add(WalletOwner owner, String rawMsisdn, String label, boolean confirmsRegisteredName) {
        String msisdn = Msisdn.normalise(rawMsisdn).orElseThrow(() ->
                new IllegalArgumentException("Enter a valid Safaricom number, for example 0712 345 678."));
        if (!confirmsRegisteredName) {
            throw new IllegalArgumentException("Confirm the number is registered in your name.");
        }
        List<PayoutMethod> live = list(owner);
        if (live.stream().anyMatch(method -> method.getMsisdn().equals(msisdn))) {
            throw new DuplicateResourceException("This number is already one of your payout methods.");
        }
        if (live.size() >= MAX_LIVE_METHODS) {
            throw new IllegalArgumentException("You can keep up to " + MAX_LIVE_METHODS + " payout numbers. Remove one first.");
        }
        UUID actor = domainSecurityService.getCurrentUserUuid();
        PayoutMethod method = new PayoutMethod();
        method.setOwnerType(owner.type());
        method.setOwnerUuid(owner.uuid());
        method.setMethodType(PayoutMethodType.MPESA);
        method.setMsisdn(msisdn);
        method.setLabel(StringUtils.hasText(label) ? label.trim() : null);
        method.setStatus(PayoutMethodStatus.PENDING_VERIFICATION);
        method.setAddedByUserUuid(actor);
        methodRepository.saveAndFlush(method);
        audit(owner, WalletAuditEventType.PAYOUT_METHOD_ADDED, actor, method, Map.of(), values(method));
        return new AddedPayoutMethod(method, sendCode(method, actor));
    }

    @Override
    @Transactional
    public IssuedCode resendCode(WalletOwner owner, UUID methodUuid) {
        PayoutMethod method = find(owner, methodUuid);
        if (method.getStatus() != PayoutMethodStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException("This number is already verified.");
        }
        return sendCode(method, domainSecurityService.getCurrentUserUuid());
    }

    @Override
    @Transactional
    public PayoutMethod verify(WalletOwner owner, UUID methodUuid, String code) {
        PayoutMethod method = find(owner, methodUuid);
        if (method.getStatus() != PayoutMethodStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException("This number is already verified.");
        }
        oneTimeCodeService.verify(OneTimeCodePurpose.PAYOUT_METHOD_VERIFICATION, method.getUuid(), code);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> before = values(method);
        method.setStatus(PayoutMethodStatus.VERIFIED);
        method.setVerifiedAt(now);
        method.setUnlockAt(now.plus(hold));
        boolean first = list(owner).stream()
                .noneMatch(other -> other.isDefaultMethod() && !other.getUuid().equals(method.getUuid()));
        method.setDefaultMethod(first);
        methodRepository.save(method);
        UUID actor = domainSecurityService.getCurrentUserUuid();
        audit(owner, WalletAuditEventType.PAYOUT_METHOD_VERIFIED, actor, method, before, values(method));
        alert(owner, "A payout number was added to your wallet",
                Msisdn.mask(method.getMsisdn()) + " can receive withdrawals from " + formatUtc(method.getUnlockAt())
                        + ". If this wasn't you, remove it and change your password.");
        return method;
    }

    @Override
    @Transactional
    public PayoutMethod makeDefault(WalletOwner owner, UUID methodUuid) {
        PayoutMethod method = find(owner, methodUuid);
        if (method.getStatus() != PayoutMethodStatus.VERIFIED) {
            throw new IllegalArgumentException("Verify this number before making it the default.");
        }
        if (method.isDefaultMethod()) {
            return method;
        }
        PayoutMethod previous = list(owner).stream().filter(PayoutMethod::isDefaultMethod).findFirst().orElse(null);
        if (previous != null) {
            previous.setDefaultMethod(false);
            methodRepository.saveAndFlush(previous);
        }
        method.setDefaultMethod(true);
        methodRepository.save(method);
        Map<String, Object> before = previous == null ? Map.of() : Map.of("default_msisdn", Msisdn.mask(previous.getMsisdn()));
        audit(owner, WalletAuditEventType.PAYOUT_METHOD_DEFAULT_CHANGED, domainSecurityService.getCurrentUserUuid(),
                method, before, Map.of("default_msisdn", Msisdn.mask(method.getMsisdn())));
        return method;
    }

    @Override
    @Transactional
    public void remove(WalletOwner owner, UUID methodUuid) {
        PayoutMethod method = find(owner, methodUuid);
        Map<String, Object> before = values(method);
        boolean wasDefault = method.isDefaultMethod();
        boolean wasVerified = method.getStatus() == PayoutMethodStatus.VERIFIED;
        method.setStatus(PayoutMethodStatus.REMOVED);
        method.setDefaultMethod(false);
        method.setRemovedAt(OffsetDateTime.now(ZoneOffset.UTC));
        methodRepository.saveAndFlush(method);
        if (wasDefault) {
            list(owner).stream()
                    .filter(other -> other.getStatus() == PayoutMethodStatus.VERIFIED)
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultMethod(true);
                        methodRepository.save(next);
                    });
        }
        audit(owner, WalletAuditEventType.PAYOUT_METHOD_REMOVED, domainSecurityService.getCurrentUserUuid(),
                method, before, values(method));
        if (wasVerified) {
            alert(owner, "A payout number was removed from your wallet",
                    Msisdn.mask(method.getMsisdn()) + " will no longer receive withdrawals. If this wasn't you, "
                            + "change your password and check your payout details.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutApprover> approvers(UUID organisationUuid) {
        return approverRepository.findByOrganisationUuidOrderByIdAsc(organisationUuid);
    }

    @Override
    @Transactional
    public PayoutApprover addApprover(UUID organisationUuid, UUID userUuid) {
        if (!domainSecurityService.isOrganisationAdmin(userUuid, organisationUuid)) {
            throw new IllegalArgumentException("Only an admin of this organisation can be a payout approver.");
        }
        if (approverRepository.existsByOrganisationUuidAndUserUuid(organisationUuid, userUuid)) {
            throw new DuplicateResourceException("This admin is already a payout approver.");
        }
        UUID actor = domainSecurityService.getCurrentUserUuid();
        PayoutApprover approver = new PayoutApprover();
        approver.setOrganisationUuid(organisationUuid);
        approver.setUserUuid(userUuid);
        approver.setAddedByUserUuid(actor);
        approverRepository.save(approver);
        auditRepository.save(WalletAuditEvent.of(WalletOwner.organisation(organisationUuid),
                WalletAuditEventType.PAYOUT_APPROVER_ADDED, actor, null, "Payout approver named",
                Map.of(), Map.of("approver_user_uuid", userUuid.toString())));
        return approver;
    }

    @Override
    @Transactional
    public void removeApprover(UUID organisationUuid, UUID userUuid) {
        PayoutApprover approver = approverRepository.findByOrganisationUuidAndUserUuid(organisationUuid, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("This admin is not a payout approver."));
        approverRepository.delete(approver);
        auditRepository.save(WalletAuditEvent.of(WalletOwner.organisation(organisationUuid),
                WalletAuditEventType.PAYOUT_APPROVER_REMOVED, domainSecurityService.getCurrentUserUuid(), null,
                "Payout approver removed", Map.of("approver_user_uuid", userUuid.toString()), Map.of()));
    }

    private PayoutMethod find(WalletOwner owner, UUID methodUuid) {
        return methodRepository.findByUuidAndOwnerTypeAndOwnerUuid(methodUuid, owner.type(), owner.uuid())
                .filter(method -> method.getStatus() != PayoutMethodStatus.REMOVED)
                .orElseThrow(() -> new ResourceNotFoundException("Payout number not found."));
    }

    private IssuedCode sendCode(PayoutMethod method, UUID actor) {
        return oneTimeCodeService.issue(OneTimeCodePurpose.PAYOUT_METHOD_VERIFICATION, method.getUuid(), actor,
                method.getMsisdn(), code -> "Your Elimika code to add this number for withdrawals is " + code
                        + ". It expires in 10 minutes. Never share it with anyone.");
    }

    private void audit(WalletOwner owner, WalletAuditEventType type, UUID actor, PayoutMethod method,
                       Map<String, Object> before, Map<String, Object> after) {
        auditRepository.save(WalletAuditEvent.of(owner, type, actor, method.getUuid().toString(),
                "M-Pesa " + Msisdn.mask(method.getMsisdn()), before, after));
    }

    /** Only masked numbers reach the audit trail. */
    private static Map<String, Object> values(PayoutMethod method) {
        Map<String, Object> values = new HashMap<>();
        values.put("msisdn", Msisdn.mask(method.getMsisdn()));
        values.put("status", method.getStatus().name());
        values.put("is_default", method.isDefaultMethod());
        if (method.getUnlockAt() != null) {
            values.put("unlock_at", method.getUnlockAt().toString());
        }
        return values;
    }

    /** Emails the person who made the change and tells the organisation's other approvers in the app. */
    private void alert(WalletOwner owner, String title, String body) {
        String link = (frontendUrl == null ? "" : frontendUrl) + (owner.type() == WalletOwnerType.ORGANISATION
                ? "/dashboard/organisation/wallet?tab=payout" : "/dashboard/wallet?tab=payout");
        Map<String, Object> variables = Map.of("alertTitle", title, "alertMessage", body, "actionLink", link);
        UUID actor = domainSecurityService.getCurrentUserUuid();
        domainSecurityService.getCurrentCallerContact().ifPresent(contact -> eventPublisher.publishEvent(
                new NotificationRequestedEvent(null, actor, contact.email(), contact.name(), SECURITY_ALERT, "HIGH",
                        "INBOX", title, body, link, variables, Set.of("in_app", "email"), null, null,
                        owner.type() == WalletOwnerType.ORGANISATION ? owner.uuid() : null, null)));
        if (owner.type() == WalletOwnerType.ORGANISATION) {
            approvers(owner.uuid()).stream()
                    .map(PayoutApprover::getUserUuid)
                    .filter(approver -> !approver.equals(actor))
                    .forEach(approver -> eventPublisher.publishEvent(new NotificationRequestedEvent(null, approver,
                            null, null, SECURITY_ALERT, "HIGH", "INBOX", title, body, link, variables,
                            Set.of("in_app"), null, null, owner.uuid(), null)));
        }
    }

    private static String formatUtc(OffsetDateTime time) {
        return time.atZoneSameInstant(java.time.ZoneId.of("Africa/Nairobi"))
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", java.util.Locale.ENGLISH))
                + " (EAT)";
    }
}
