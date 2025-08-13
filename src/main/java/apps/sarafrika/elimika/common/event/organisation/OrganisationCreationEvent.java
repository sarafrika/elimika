package apps.sarafrika.elimika.common.event.organisation;

import java.util.UUID;

public record OrganisationCreationEvent(String name, String slug,String description ,String realm, UUID blastWaveId) {
}
