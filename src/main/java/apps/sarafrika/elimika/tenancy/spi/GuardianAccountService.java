package apps.sarafrika.elimika.tenancy.spi;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Creates domain-less accounts for invited guardians; {@code parent} follows from the guardian link. */
public interface GuardianAccountService {

    /**
     * Creates the Keycloak account with the guardian's chosen password, its email already verified (the invitation
     * reached that inbox), and the local mirror. No email is sent. Empty when the email already has an account;
     * {@link IllegalArgumentException} when Keycloak's password policy rejects the password.
     */
    Optional<UUID> registerInvitedGuardian(InvitedGuardianAccount account, String password);

    /** Identity supplied by the invited guardian; the email comes from the invitation, not the caller. */
    record InvitedGuardianAccount(String email, String firstName, String lastName, String phoneNumber,
                                  LocalDate dob, String gender) {
    }
}
