package apps.sarafrika.elimika.shared.event.email;

public record MailingListMembersEvent(String email, String name, String mailingListAddress ,String action) {
}
