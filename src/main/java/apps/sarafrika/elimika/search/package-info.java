/**
 * Search module - the search engine adapter and the machinery that keeps its indexes in step with
 * PostgreSQL.
 * <p>
 * PostgreSQL stays the source of truth; every index is a disposable projection. This module owns the
 * engine (Meilisearch, spoken over plain HTTP) and the sync machinery, and knows nothing about any
 * domain. What goes into an index is defined by the module that owns the data, through a
 * {@code shared.search.SearchDocumentSource} bean; modules search through
 * {@code shared.search.SearchGateway}. No other module imports anything from here, and nothing here
 * is imported by another module - it is a leaf.
 * <ul>
 *     <li>{@code internal.meilisearch} - the {@code SearchGateway}/{@code SearchIndexAdmin}
 *     implementation, filter rendering and the health indicator.</li>
 *     <li>{@code internal.sync} - the durable indexing listener, incomplete-publication resubmission,
 *     blue/green rebuilds, drift reconciliation and the startup check.</li>
 *     <li>{@code internal.state} - the {@code search_index_state} table.</li>
 * </ul>
 * Search is off by default ({@code search.enabled=false}): no events are published, no HTTP call is
 * made, and every search raises {@code SearchUnavailableException} so callers fall back to the
 * database.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Search",
        allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.search;
