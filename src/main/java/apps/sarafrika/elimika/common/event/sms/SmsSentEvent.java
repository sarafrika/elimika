package apps.sarafrika.elimika.common.event.sms;

import java.util.UUID;

public record SmsSentEvent(boolean successful, UUID logUuid, String statusId) {
}
