package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.commerce.spi.paywall.CommercePaywallService;
import apps.sarafrika.elimika.shared.exceptions.PaymentRequiredException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePaywallServiceImpl implements CommercePaywallService {

    private final CommerceAccessService commerceAccessService;

    @Override
    public void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid) {
        Assert.notNull(studentUuid, "studentUuid is required for paywall verification");
        Assert.notNull(classDefinitionUuid, "classDefinitionUuid is required for paywall verification");

        boolean hasAccess = commerceAccessService.hasClassAccess(studentUuid, classDefinitionUuid);
        if (!hasAccess) {
            log.warn("Paywall check failed for student {} and class {}", studentUuid, classDefinitionUuid);
            throw new PaymentRequiredException("Payment required before enrollment is permitted");
        }
    }
}
