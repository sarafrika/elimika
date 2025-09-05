package apps.sarafrika.elimika.shared.event.email;

import java.util.UUID;

public record SendEmailEvent(UUID senderUuid, UUID logUuid, UUID organisationUuid) {
}
