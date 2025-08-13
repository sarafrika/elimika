package apps.sarafrika.elimika.common.event.organisation;

import java.util.UUID;

public record OrganisationCreationFailureEvent(UUID userUuid, String organisationName, String errorMessage) {
}
