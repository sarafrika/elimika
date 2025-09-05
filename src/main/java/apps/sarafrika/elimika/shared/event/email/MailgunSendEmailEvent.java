package apps.sarafrika.elimika.shared.event.email;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record MailgunSendEmailEvent(String from, String senderName, String to, String replyTo, String replyToName, String subject, String body, List<String> attachments, String domain, ZonedDateTime scheduledDateTime, UUID sentLoguuid) {
}
