package apps.sarafrika.elimika.commerce.purchase.service;

import java.util.UUID;

public interface CommerceAccessService {

    boolean hasCourseAccess(UUID studentUuid, UUID courseUuid);

    boolean hasClassAccess(UUID studentUuid, UUID classDefinitionUuid);
}
