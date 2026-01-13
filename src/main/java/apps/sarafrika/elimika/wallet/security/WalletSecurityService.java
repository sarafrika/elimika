package apps.sarafrika.elimika.wallet.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("walletSecurityService")
@RequiredArgsConstructor
public class WalletSecurityService {

    private final DomainSecurityService domainSecurityService;

    public boolean canAccessWallet(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        if (domainSecurityService.isOrganizationAdmin()) {
            return true;
        }
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        return currentUserUuid != null && currentUserUuid.equals(userUuid);
    }

    public boolean canTransferFrom(UUID userUuid) {
        return canAccessWallet(userUuid);
    }
}
