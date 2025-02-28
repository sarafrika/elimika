package apps.sarafrika.elimika.common.event.mailgun;

import java.util.UUID;

public record SuccessfulEmailSend(UUID sentLoguuid, String mailgunId) {
}
