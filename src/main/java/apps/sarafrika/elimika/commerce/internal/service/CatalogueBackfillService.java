package apps.sarafrika.elimika.commerce.internal.service;

/**
 * Forces catalogue regeneration for existing courses/classes.
 */
public interface CatalogueBackfillService {

    /**
     * Rebuilds catalogue entries for all published courses and their class definitions.
     *
     * @return number of class definitions processed
     */
    int backfillPublishedCatalogue();
}
