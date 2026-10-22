package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.wallet.entity.PayoutApprover;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.repository.PayoutApproverRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** In-app notices about a withdrawal's outcome, to the earner or to the organisation's payout approvers. */
@Component
@RequiredArgsConstructor
public class WithdrawalNotifier {

    private static final String PAID = "WALLET_WITHDRAWAL_PAID";
    private static final String FAILED = "WALLET_WITHDRAWAL_FAILED";

    private final ApplicationEventPublisher eventPublisher;
    private final PayoutApproverRepository approverRepository;

    public void paid(Withdrawal withdrawal) {
        publish(withdrawal, PAID, kes(withdrawal.getNetAmount()) + " sent to M-Pesa",
                "Withdrawal " + withdrawal.getReference() + " reached " + Msisdn.mask(withdrawal.getMsisdn())
                        + (withdrawal.getMpesaReceipt() == null ? "." : ". M-Pesa receipt " + withdrawal.getMpesaReceipt() + "."));
    }

    public void failed(Withdrawal withdrawal) {
        publish(withdrawal, FAILED, "Withdrawal returned to your wallet",
                "M-Pesa could not pay " + withdrawal.getReference() + ", so " + kes(withdrawal.getAmount())
                        + " is back in the wallet. " + nullToEmpty(withdrawal.getFailureReason()));
    }

    public void rejected(Withdrawal withdrawal) {
        publish(withdrawal, FAILED, "Withdrawal not approved",
                withdrawal.getReference() + " was not approved and " + kes(withdrawal.getAmount())
                        + " is back in the wallet. Reason: " + nullToEmpty(withdrawal.getRejectionReason()));
    }

    private void publish(Withdrawal withdrawal, String type, String title, String body) {
        boolean organisation = withdrawal.getOwnerType() == WalletOwnerType.ORGANISATION;
        String link = organisation ? "/dashboard/organisation/wallet?tab=withdrawals" : "/dashboard/wallet?tab=withdrawals";
        Set<UUID> recipients = new LinkedHashSet<>();
        if (organisation) {
            approverRepository.findByOrganisationUuidOrderByIdAsc(withdrawal.getOwnerUuid()).stream()
                    .map(PayoutApprover::getUserUuid).forEach(recipients::add);
            if (withdrawal.getRequestedByUserUuid() != null) {
                recipients.add(withdrawal.getRequestedByUserUuid());
            }
        } else {
            recipients.add(withdrawal.getOwnerUuid());
        }
        Map<String, Object> variables = Map.of("reference", withdrawal.getReference(),
                "amount", withdrawal.getAmount().toPlainString(), "status", withdrawal.getStatus().name());
        for (UUID recipient : recipients) {
            eventPublisher.publishEvent(new NotificationRequestedEvent(null, recipient, null, null, type, "HIGH", "INBOX",
                    title, body.trim(), link, variables, Set.of("in_app"),
                    type + ":" + withdrawal.getUuid() + ":" + recipient, null,
                    organisation ? withdrawal.getOwnerUuid() : null, organisation ? "organisation_user" : null));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String kes(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        return "KES " + format.format(amount);
    }
}
