package apps.sarafrika.elimika.tenancy.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Creates accounts for guardians a student invited, since parents cannot self-register.
 * The account carries no domain; the {@code parent} domain follows from the guardian link.
 */
public interface GuardianAccountService {

    /** Creates the Keycloak account and local mirror and emails the set-password link; empty when the email already has an account. */
    Optional<UUID> registerInvitedGuardian(InvitedGuardianAccount account);

    /** Identity supplied by the invited guardian; the email comes from the invitation, not the caller. */
    record InvitedGuardianAccount(String email, String firstName, String lastName, String phoneNumber) {
    }
}
