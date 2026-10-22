package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.shared.otp.IssuedCode;
import apps.sarafrika.elimika.wallet.entity.PayoutApprover;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import java.util.List;
import java.util.UUID;

/** Payout numbers of a wallet, verified by a code sent to them, and an organisation's payout approvers. */
public interface PayoutMethodService {

    List<PayoutMethod> list(WalletOwner owner);

    /** Adds the number as pending and sends its code there. */
    AddedPayoutMethod add(WalletOwner owner, String rawMsisdn, String label, boolean confirmsRegisteredName);

    IssuedCode resendCode(WalletOwner owner, UUID methodUuid);

    PayoutMethod verify(WalletOwner owner, UUID methodUuid, String code);

    PayoutMethod makeDefault(WalletOwner owner, UUID methodUuid);

    void remove(WalletOwner owner, UUID methodUuid);

    List<PayoutApprover> approvers(UUID organisationUuid);

    PayoutApprover addApprover(UUID organisationUuid, UUID userUuid);

    void removeApprover(UUID organisationUuid, UUID userUuid);

    record AddedPayoutMethod(PayoutMethod method, IssuedCode verification) {
    }
}
