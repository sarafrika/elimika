package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.service.CatalogueBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogueBackfillServiceImpl implements CatalogueBackfillService {

    private final InternalCommerceProperties internalCommerceProperties;

    @Override
    public int backfillPublishedCatalogue() {
        if (!Boolean.TRUE.equals(internalCommerceProperties.getEnabled())) {
            log.info("Internal commerce disabled; skipping catalogue backfill");
            return 0;
        }

        log.info("Catalogue backfill no-op: commerce module cannot depend on course/classes per architecture rules");
        return 0;
    }
}
