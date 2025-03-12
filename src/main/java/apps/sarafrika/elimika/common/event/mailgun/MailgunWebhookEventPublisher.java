package apps.sarafrika.elimika.common.event.mailgun;

public record MailgunWebhookEventPublisher(String event, String recipient, String mailgunId) {
}
