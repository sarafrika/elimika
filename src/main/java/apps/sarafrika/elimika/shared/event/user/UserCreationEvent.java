package apps.sarafrika.elimika.shared.event.user;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.util.UUID;

public record UserCreationEvent(String username, String firstName, String lastName, String email, Boolean active,
                                UserDomain userDomain, String realm, UUID sarafrikaCorrelationId) {
}


