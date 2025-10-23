package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.commerce.purchase.service.CommercePaywallService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePaywallServiceImpl implements CommercePaywallService {

    private final CommerceAccessService commerceAccessService;
    private final ClassDefinitionService classDefinitionService;

    @Override
    public void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid) {
        log.debug("Bypassing commerce paywall verification for student {} and class {}",
                studentUuid, classDefinitionUuid);
    }
}
