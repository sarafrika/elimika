package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.RegistrationRequestDTO;

import java.util.UUID;

/** Self-registration: Keycloak owns the account, Elimika records the requested domain as pending. */
public interface RegistrationService {

    /** Registers a new person; silently skips an email that already has an account. */
    void register(RegistrationRequestDTO request, String clientIp);

    /** Resends the set-password email while it is still outstanding; silently skips otherwise. */
    void resendActionsEmail(String email);

    /** An existing signed-in account asks for another domain; it is held pending like a registration. */
    DomainApplicationDTO applyForDomain(UUID userUuid, String domain);
}
