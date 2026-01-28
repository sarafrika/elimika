package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Development-only implementation of {@link CommerceAccessService} that grants universal access.
 * <p>
 * This service is only active when the "dev" profile is enabled. It allows students to enroll
 * in any course or class without requiring a preceding purchase, facilitating rapid development
 * and testing of enrollment workflows.
 */
@Service
@Profile("dev")
@Slf4j
public class DevCommerceAccessServiceImpl implements CommerceAccessService {

    @Override
    public boolean hasCourseAccess(UUID studentUuid, UUID courseUuid) {
        log.debug("Dev profile active: granting universal access to course {} for student {}", courseUuid, studentUuid);
        return true;
    }

    @Override
    public boolean hasClassAccess(UUID studentUuid, UUID classDefinitionUuid) {
        log.debug("Dev profile active: granting universal access to class {} for student {}", classDefinitionUuid, studentUuid);
        return true;
    }
}
