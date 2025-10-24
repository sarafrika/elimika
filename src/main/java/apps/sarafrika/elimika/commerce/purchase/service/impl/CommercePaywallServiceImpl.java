package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.commerce.purchase.spi.paywall.CommercePaywallService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePaywallServiceImpl implements CommercePaywallService {

    private final CommerceAccessService commerceAccessService;

    @Override
    public void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid) {
        log.debug("Bypassing commerce paywall verification for student {} and class {}",
                studentUuid, classDefinitionUuid);
    }
}
