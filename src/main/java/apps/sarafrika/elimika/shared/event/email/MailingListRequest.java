package apps.sarafrika.elimika.shared.event.email;

public record MailingListRequest(String name, String description, String mailingListAddress, String action) {
}
