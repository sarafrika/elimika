package apps.sarafrika.elimika.common.event.organisation;

import java.util.UUID;

public record OrganisationCreationEvent(String name, String slug,String description ,String realm, String domain, UUID sarafrikaCorrelationId, UUID userUuid) {
}
