package apps.sarafrika.elimika.common.event.user;

import apps.sarafrika.elimika.common.enums.UserDomain;

import java.util.UUID;

public record UserCreationEvent(String username, String firstName, String lastName, String email, Boolean active,
                                UserDomain userDomain, String realm, UUID sarafrikaCorrelationId) {
}


