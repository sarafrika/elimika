package apps.sarafrika.elimika.wallet.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.wallet.repository.PayoutApproverRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Authorization for wallet balances and ledger operations.
 * <p>
 * A wallet is keyed by user, and holds money. Two callers legitimately reach someone else's
 * wallet: a platform admin doing support, and an administrator of an organisation that user
 * actually belongs to. Everyone else may only reach their own.
 * <p>
 * The distinction that matters here is {@code administersOrganisationOf(target)} rather than
 * {@code isOrganizationAdmin()}. The latter reads the caller's domains and never looks at the
 * target, so it was satisfied by an administrator of <em>any</em> organisation operating on
 * <em>any</em> user — which on a credit endpoint means being able to fund an arbitrary wallet on
 * the platform.
 */
@Service("walletSecurityService")
@RequiredArgsConstructor
public class WalletSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final PayoutApproverRepository payoutApproverRepository;

    @Value("${wallet.withdrawals.enabled:false}")
    private boolean withdrawalsEnabled;

    /**
     * Read access to a wallet and its ledger: the holder, a platform admin, or an administrator of
     * an organisation the holder belongs to.
     */
    public boolean canAccessWallet(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        if (isSelf(userUuid)) {
            return true;
        }
        return domainSecurityService.isPlatformAdmin()
                || domainSecurityService.administersOrganisationOf(userUuid);
    }

    /**
     * Crediting a wallet — recording a deposit or a sale — is not a self-service operation, so
     * unlike {@link #canAccessWallet(UUID)} this deliberately omits the holder.
     */
    public boolean canCreditWallet(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        return domainSecurityService.isPlatformAdmin()
                || domainSecurityService.administersOrganisationOf(userUuid);
    }

    /**
     * Moving money out of a wallet needs the same standing as reading it: the holder always, and
     * otherwise someone with administrative reach over the holder.
     */
    public boolean canTransferFrom(UUID userUuid) {
        return canAccessWallet(userUuid);
    }

    /**
     * Withdrawing is holder-only, and only for people who earn: an approved instructor or course
     * creator. Off until {@code wallet.withdrawals.enabled} is set.
     */
    public boolean canWithdraw(UUID userUuid) {
        return withdrawalsEnabled && userUuid != null && isSelf(userUuid)
                && domainSecurityService.hasAnyDomain(UserDomain.instructor, UserDomain.course_creator);
    }

    /** An organisation's money is withdrawn by its named payout approvers; platform admins never do it for them. */
    public boolean canWithdrawFromOrganisation(UUID organisationUuid) {
        return withdrawalsEnabled && canManageOrganisationPayouts(organisationUuid);
    }

    /** A person's own payout numbers, managed by them alone, and only by people who earn. */
    public boolean canManagePayouts(UUID userUuid) {
        return userUuid != null && isSelf(userUuid)
                && domainSecurityService.hasAnyDomain(UserDomain.instructor, UserDomain.course_creator);
    }

    /** An organisation's payout numbers: an admin of it who is also one of its named payout approvers. */
    public boolean canManageOrganisationPayouts(UUID organisationUuid) {
        UUID caller = domainSecurityService.getCurrentUserUuid();
        return caller != null && domainSecurityService.managesOrganisation(organisationUuid)
                && payoutApproverRepository.existsByOrganisationUuidAndUserUuid(organisationUuid, caller);
    }

    /** Naming payout approvers is for any admin of the organisation. */
    public boolean canManagePayoutApprovers(UUID organisationUuid) {
        return domainSecurityService.managesOrganisation(organisationUuid);
    }

    private boolean isSelf(UUID userUuid) {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        return currentUserUuid != null && currentUserUuid.equals(userUuid);
    }
}
