package apps.sarafrika.elimika.common.event.email;

public record MailingListMembersEvent(String email, String name, String mailingListAddress ,String action) {
}
