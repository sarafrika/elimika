package apps.sarafrika.elimika.commerce.internal.listener;

import apps.sarafrika.elimika.commerce.internal.service.CatalogueBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Backfills catalogue entries for already published courses/classes on startup.
 * This is idempotent and only runs when internal commerce is enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogueBackfillInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final CatalogueBackfillService catalogueBackfillService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        int processed = catalogueBackfillService.backfillPublishedCatalogue();
        log.info("Catalogue backfill finished; processed {} class definitions", processed);
    }
}
