package apps.sarafrika.elimika.common.event.organisation;

import java.util.UUID;

public record SuccessfulOrganisationCreationEvent(UUID blastWaveId, String keycloakId, UUID userUuid, String organisationName) {
}
