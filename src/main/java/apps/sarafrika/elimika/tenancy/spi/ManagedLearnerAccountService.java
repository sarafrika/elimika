package apps.sarafrika.elimika.tenancy.spi;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Creates accounts for learners whom a guardian manages: a local user with a user number but no Keycloak account
 * and no email, so it can never sign in. The guardian link is what gives anyone access to it.
 */
public interface ManagedLearnerAccountService {

    /** Creates the account inside the caller's transaction and returns its uuid. Sends nothing. */
    UUID createManagedLearnerAccount(ManagedLearnerAccount account);

    record ManagedLearnerAccount(String firstName, String middleName, String lastName, LocalDate dob) {
    }
}
