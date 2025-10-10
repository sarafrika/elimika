package apps.sarafrika.elimika.commerce.purchase.service;

import java.util.UUID;

public interface CommercePaywallService {

    void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid);
}
