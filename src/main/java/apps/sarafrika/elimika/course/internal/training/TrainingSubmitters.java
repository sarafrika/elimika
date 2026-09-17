package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Finds the user behind a created_by audit value: the JWT subject (Keycloak id), or an email on older rows. */
@Component
@RequiredArgsConstructor
public class TrainingSubmitters {

    private final UserLookupService userLookupService;

    public Optional<UUID> userOf(String auditUser) {
        if (auditUser == null || auditUser.isBlank()) {
            return Optional.empty();
        }
        return userLookupService.findUserUuidByKeycloakId(auditUser)
                .or(() -> userLookupService.findUserUuidByEmail(auditUser));
    }
}
