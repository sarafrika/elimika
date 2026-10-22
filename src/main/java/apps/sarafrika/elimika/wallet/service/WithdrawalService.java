package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.shared.otp.IssuedCode;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Withdrawals to M-Pesa: request, confirm by SMS code (money leaves the balance), optional second
 * approval and admin review, then paid or reversed from M-Pesa's result.
 */
public interface WithdrawalService {

    RequestedWithdrawal request(WalletOwner owner, BigDecimal amount, UUID payoutMethodUuid, String idempotencyKey);

    IssuedCode resendCode(WalletOwner owner, UUID withdrawalUuid);

    Withdrawal confirm(WalletOwner owner, UUID withdrawalUuid, String code);

    Withdrawal cancel(WalletOwner owner, UUID withdrawalUuid);

    /** An organisation's second payout approver agrees; must not be the one who asked. */
    Withdrawal approveAsSecond(WalletOwner owner, UUID withdrawalUuid);

    Page<Withdrawal> list(WalletOwner owner, Collection<WithdrawalStatus> statuses, Pageable pageable);

    Withdrawal get(WalletOwner owner, UUID withdrawalUuid);

    Page<Withdrawal> queue(Collection<WithdrawalStatus> statuses, Pageable pageable);

    Withdrawal adminApprove(UUID withdrawalUuid);

    Withdrawal adminReject(UUID withdrawalUuid, String reason);

    /** Hands a cleared withdrawal to M-Pesa; safe to repeat. */
    void send(UUID withdrawalUuid);

    /** Asks M-Pesa about a withdrawal in flight and settles it when the result is in. */
    void refresh(UUID withdrawalUuid);

    /** Ends a request whose code was never entered. */
    void expire(UUID withdrawalUuid);

    record RequestedWithdrawal(Withdrawal withdrawal, IssuedCode verification) {
    }
}
