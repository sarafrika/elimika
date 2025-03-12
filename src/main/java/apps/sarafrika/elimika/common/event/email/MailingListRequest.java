package apps.sarafrika.elimika.common.event.email;

public record MailingListRequest(String name, String description, String mailingListAddress, String action) {
}
