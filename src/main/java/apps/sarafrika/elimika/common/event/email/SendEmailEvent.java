package apps.sarafrika.elimika.common.event.email;

import java.util.UUID;

public record SendEmailEvent(UUID senderUuid, UUID logUuid, UUID organisationUuid) {
}
