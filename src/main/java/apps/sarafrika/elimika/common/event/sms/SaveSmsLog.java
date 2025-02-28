package apps.sarafrika.elimika.common.event.sms;

import apps.sarafrika.elimika.common.enums.SenderEnum;

import java.util.UUID;

public record SaveSmsLog(UUID organisationUuid, String msisdn, String message, SenderEnum sender, UUID senderUuid) {
}
