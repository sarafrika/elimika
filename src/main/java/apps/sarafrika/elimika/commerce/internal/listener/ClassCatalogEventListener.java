package apps.sarafrika.elimika.commerce.internal.listener;

import apps.sarafrika.elimika.commerce.internal.service.CatalogProvisioningService;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchronises class definitions into the internal commerce catalog when new classes are created or updated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassCatalogEventListener {

    private final CatalogProvisioningService catalogProvisioningService;

    @EventListener
    public void onClassDefined(ClassDefinedEventDTO event) {
        log.debug("Received ClassDefinedEvent for class definition {}", event.definitionUuid());
        catalogProvisioningService.ensureClassIsPurchasable(event.definitionUuid());
    }

    @EventListener
    public void onClassUpdated(ClassDefinitionUpdatedEventDTO event) {
        log.debug("Received ClassDefinitionUpdatedEvent for class definition {}", event.definitionUuid());
        catalogProvisioningService.ensureClassIsPurchasable(event.definitionUuid());
    }
}
