package apps.sarafrika.elimika.common.event.user;

public record AddUserToOrganisationEvent(String userId, String organisationId, String realm) {
}
