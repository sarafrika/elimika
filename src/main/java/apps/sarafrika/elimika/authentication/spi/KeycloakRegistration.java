package apps.sarafrika.elimika.authentication.spi;

import java.time.LocalDate;

/** Identity given at registration; written to Keycloak, which owns personal data. */
public record KeycloakRegistration(
        String email,
        String firstName,
        String middleName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender
) {
}
