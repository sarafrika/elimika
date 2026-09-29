package apps.sarafrika.elimika.search.integration;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.repository.PlatformCurrencyRepository;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises the search platform against a real Meilisearch and a real PostgreSQL: index settings,
 * upsert, scoped search, delete, the durable indexing listener (fed both explicitly and by a JPA
 * entity change), and a blue/green rebuild.
 * <p>
 * No production module defines an index yet, so the test brings two sources of its own: a list-backed
 * catalogue, and one over the {@code currencies} table whose trigger fires on {@link PlatformCurrency}
 * writes - which proves the entity listener to event publication to engine path end to end.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Testcontainers
@DisplayName("Search platform (end-to-end)")
class SearchPlatformIntegrationTest {

    private static final String IMAGE = "getmeili/meilisearch:v1.54.1";
    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final String CATALOGUE = "test_catalogue";
    private static final String CURRENCIES = "test_currencies";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withEnv("MEILI_MASTER_KEY", MASTER_KEY)
            .withEnv("MEILI_NO_ANALYTICS", "true")
            .withExposedPorts(7700)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        registry.add("search.enabled", () -> "true");
        registry.add("search.meilisearch.host", SearchPlatformIntegrationTest::meilisearchUrl);
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
        // Small batches so the rebuild walks several checkpoints.
        registry.add("search.rebuild-batch-size", () -> "2");
    }

    private static String meilisearchUrl() {
        return "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700);
    }

    @MockBean private JwtDecoder jwtDecoder;

    @Autowired private SearchGateway gateway;
    @Autowired private SearchIndexAdmin admin;
    @Autowired private SearchIndexRequests indexRequests;
    @Autowired private SearchIndexRebuilder rebuilder;
    @Autowired private SearchIndexStateStore stateStore;
    @Autowired private CatalogueSource catalogue;
    @Autowired private PlatformCurrencyRepository currencyRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final UUID orgA = UUID.randomUUID();
    private final UUID orgB = UUID.randomUUID();

    @BeforeEach
    void reset() {
        catalogue.clear();
        admin.deleteIndex(CATALOGUE);
        admin.ensureIndex(CATALOGUE, catalogue.definition());
    }

    // ===== Settings =====

    @Test
    @DisplayName("ensureIndex creates the index with the definition's settings")
    void ensureIndexAppliesSettings() {
        JsonNode settings = engine().get().uri("/indexes/{uid}/settings", CATALOGUE).retrieve().body(JsonNode.class);

        assertThat(texts(settings.path("searchableAttributes"))).containsExactly("title", "description");
        assertThat(texts(settings.path("filterableAttributes"))).contains("organisation_uuid", "status", "price");
        assertThat(texts(settings.path("sortableAttributes"))).contains("price", "title");
        assertThat(texts(settings.path("synonyms").path("js"))).containsExactly("javascript");
        assertThat(texts(settings.path("typoTolerance").path("disableOnAttributes"))).containsExactly("status");
        assertThat(settings.path("pagination").path("maxTotalHits").asInt()).isEqualTo(500);

        JsonNode index = engine().get().uri("/indexes/{uid}", CATALOGUE).retrieve().body(JsonNode.class);
        assertThat(index.path("primaryKey").asText()).isEqualTo("uuid");
    }

    @Test
    @DisplayName("The startup runner created the entity-backed index and recorded its state")
    void startupRunnerPreparedIndexes() {
        assertThat(admin.stats(CURRENCIES)).isPresent();
        assertThat(stateStore.find(CURRENCIES)).isPresent();
    }

    // ===== Upsert, scoped search, delete =====

    @Test
    @DisplayName("Search is bounded by its scope; the user filter narrows it and cannot widen it")
    void scopedSearch() {
        CatalogueItem javaA = item("Java for beginners", orgA, "published", 30);
        CatalogueItem advancedA = item("Advanced Java", orgA, "published", 80);
        CatalogueItem draftA = item("Java drafts", orgA, "draft", 10);
        CatalogueItem javaB = item("Java at org B", orgB, "published", 20);
        gateway.upsert(CATALOGUE, List.of(javaA, advancedA, draftA, javaB));

        SearchScope orgAScope = SearchScope.of(SearchFilter.eq("organisation_uuid", orgA), "org:" + orgA);

        SearchPage all = gateway.search(new SearchRequest(CATALOGUE, "java", null, orgAScope,
                List.of(SearchSort.asc("price")), 0, 10, List.of("status"), null));
        assertThat(uuids(all.hits())).containsExactly(draftA.uuid(), javaA.uuid(), advancedA.uuid());
        assertThat(all.totalHits()).isEqualTo(3);
        assertThat(all.facetDistribution().get("status")).containsEntry("published", 2L).containsEntry("draft", 1L);
        assertThat(all.hits().getFirst().formatted()).isNotNull();

        SearchPage narrowed = gateway.search(new SearchRequest(CATALOGUE, "java",
                SearchFilter.and(SearchFilter.eq("status", "published"), SearchFilter.lte("price", 50)),
                orgAScope, List.of(), 0, 10, List.of(), null));
        assertThat(uuids(narrowed.hits())).containsExactly(javaA.uuid());

        // A value that tries to close its quotes and OR in the other organisation matches nothing.
        SearchPage hostile = gateway.search(SearchRequest.of(CATALOGUE, "java",
                SearchFilter.eq("status", "published\" OR organisation_uuid = \"" + orgB), orgAScope, 0, 10));
        assertThat(hostile.hits()).isEmpty();

        // Paging is 0-based.
        SearchPage second = gateway.search(new SearchRequest(CATALOGUE, null, null, orgAScope,
                List.of(SearchSort.asc("price")), 1, 2, List.of(), null));
        assertThat(uuids(second.hits())).containsExactly(advancedA.uuid());
        assertThat(second.totalPages()).isEqualTo(2);

        SearchPage unrestricted = gateway.search(SearchRequest.of(CATALOGUE, "java", null,
                SearchScope.unrestricted("platform-admin"), 0, 10));
        assertThat(unrestricted.totalHits()).isEqualTo(4);

        gateway.delete(CATALOGUE, List.of(javaA.uuid(), javaB.uuid()));
        SearchPage afterDelete = gateway.search(SearchRequest.of(CATALOGUE, "java", null,
                SearchScope.unrestricted("platform-admin"), 0, 10));
        assertThat(uuids(afterDelete.hits())).containsExactlyInAnyOrder(advancedA.uuid(), draftA.uuid());
    }

    // ===== Durable listener =====

    @Test
    @DisplayName("An indexing request upserts what the source returns and deletes what it no longer does")
    void listenerSyncsFromSource() {
        CatalogueItem kept = catalogue.add(item("Kept course", orgA, "published", 10));
        CatalogueItem removed = catalogue.add(item("Removed course", orgA, "published", 10));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                indexRequests.enqueue(CATALOGUE, Set.of(kept.uuid(), removed.uuid())));
        awaitTrue(() -> countIn(CATALOGUE, "course") == 2);

        catalogue.hide(removed.uuid());
        indexRequests.enqueue(CATALOGUE, removed.uuid());
        awaitTrue(() -> countIn(CATALOGUE, "course") == 1);
        assertThat(uuids(search(CATALOGUE, "course").hits())).containsExactly(kept.uuid());
    }

    @Test
    @DisplayName("A request from a transaction that rolls back is never published")
    void rolledBackRequestIsDropped() {
        CatalogueItem ghost = catalogue.add(item("Rolled back course", orgA, "published", 10));
        CatalogueItem marker = catalogue.add(item("Marker course", orgA, "published", 10));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            indexRequests.enqueue(CATALOGUE, ghost.uuid());
            status.setRollbackOnly();
        });
        // Requests are applied in order on one thread, so once the marker is in, the ghost had its chance.
        indexRequests.enqueue(CATALOGUE, marker.uuid());
        awaitTrue(() -> countIn(CATALOGUE, "marker") == 1);
        assertThat(countIn(CATALOGUE, "rolled")).isZero();
    }

    @Test
    @DisplayName("A JPA entity change reaches the index through its trigger, including one flushed at commit")
    void entityChangeReachesTheIndex() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        // Modify a managed entity without saving: Hibernate only notices at the commit flush.
        transaction.executeWithoutResult(status -> {
            PlatformCurrency kes = currencyRepository.findByCodeIgnoreCase("KES").orElseThrow();
            kes.setCurrencyName("Kenyan Shilling Zebrafish");
        });
        awaitTrue(() -> countIn(CURRENCIES, "zebrafish") == 1);

        transaction.executeWithoutResult(status -> {
            PlatformCurrency kes = currencyRepository.findByCodeIgnoreCase("KES").orElseThrow();
            kes.setActive(false);
        });
        awaitTrue(() -> countIn(CURRENCIES, "zebrafish") == 0);

        transaction.executeWithoutResult(status -> {
            PlatformCurrency kes = currencyRepository.findByCodeIgnoreCase("KES").orElseThrow();
            kes.setActive(true);
            kes.setCurrencyName("Kenyan Shilling");
        });
        awaitTrue(() -> countIn(CURRENCIES, "shilling") >= 1);
    }

    // ===== Rebuild =====

    @Test
    @DisplayName("A rebuild copies the source into a build index, swaps it in and drops the old contents")
    void blueGreenRebuild() {
        List<CatalogueItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            items.add(catalogue.add(item("Rebuilt course " + i, orgA, "published", i)));
        }
        CatalogueItem hidden = catalogue.add(item("Hidden rebuilt course", orgA, "published", 99));
        catalogue.hide(hidden.uuid());
        // Something the source no longer knows about, left in the live index.
        CatalogueItem stale = item("Stale rebuilt course", orgB, "published", 1);
        gateway.upsert(CATALOGUE, List.of(stale, hidden));

        rebuilder.rebuild(CATALOGUE);

        SearchPage page = search(CATALOGUE, "rebuilt");
        assertThat(uuids(page.hits())).containsExactlyInAnyOrderElementsOf(items.stream().map(CatalogueItem::uuid).toList());
        assertThat(admin.stats(CATALOGUE).orElseThrow().numberOfDocuments()).isEqualTo(5);

        SearchIndexState state = stateStore.find(CATALOGUE).orElseThrow();
        assertThat(state.getStatus()).isEqualTo(SearchIndexStatus.READY);
        assertThat(state.getSchemaVersion()).isEqualTo(1);
        assertThat(state.getBuildIndexName()).isNull();
        assertThat(state.getDocumentCount()).isEqualTo(5L);
        assertThat(state.getLastBuiltAt()).isNotNull();

        JsonNode indexes = engine().get().uri("/indexes?limit=100").retrieve().body(JsonNode.class);
        List<String> names = new ArrayList<>();
        indexes.path("results").forEach(index -> names.add(index.path("uid").asText()));
        assertThat(names).contains(CATALOGUE).noneMatch(name -> name.contains("__build_"));

        // The settings survived the swap.
        JsonNode settings = engine().get().uri("/indexes/{uid}/settings", CATALOGUE).retrieve().body(JsonNode.class);
        assertThat(texts(settings.path("filterableAttributes"))).contains("organisation_uuid");
    }

    // ===== Helpers =====

    private SearchPage search(String index, String text) {
        return gateway.search(SearchRequest.of(index, text, null, SearchScope.unrestricted("test"), 0, 100));
    }

    private long countIn(String index, String text) {
        return search(index, text).totalHits();
    }

    private static void awaitTrue(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
        }
        throw new AssertionError("Condition not met within 20s");
    }

    private static RestClient engine() {
        return RestClient.builder()
                .baseUrl(meilisearchUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + MASTER_KEY)
                .build();
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private static List<UUID> uuids(List<SearchHit> hits) {
        return hits.stream().map(SearchHit::uuid).toList();
    }

    private static CatalogueItem item(String title, UUID organisation, String status, int price) {
        return new CatalogueItem(UUID.randomUUID(), title, "About " + title, organisation, status, price);
    }

    // ===== Test sources =====

    record CatalogueItem(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("organisation_uuid") UUID organisationUuid,
            @JsonProperty("status") String status,
            @JsonProperty("price") int price
    ) implements SearchDocument {
    }

    /** A list-backed source; row ids are insertion order, hidden rows are not indexable. */
    static class CatalogueSource implements SearchDocumentSource<CatalogueItem> {

        private final ConcurrentSkipListMap<Long, CatalogueItem> rows = new ConcurrentSkipListMap<>();
        private final Set<UUID> hidden = java.util.concurrent.ConcurrentHashMap.newKeySet();
        private long nextId = 1;

        synchronized CatalogueItem add(CatalogueItem item) {
            rows.put(nextId++, item);
            return item;
        }

        void hide(UUID uuid) {
            hidden.add(uuid);
        }

        synchronized void clear() {
            rows.clear();
            hidden.clear();
        }

        @Override
        public SearchIndexDefinition definition() {
            return SearchIndexDefinition.of(CATALOGUE, 1,
                            List.of("title", "description"),
                            List.of("organisation_uuid", "status", "price"),
                            List.of("price", "title"))
                    .withSynonyms(Map.of("js", List.of("javascript")))
                    .withTypoDisabledAttributes(List.of("status"))
                    .withMaxTotalHits(500);
        }

        @Override
        public List<CatalogueItem> loadByUuids(Collection<UUID> uuids) {
            return rows.values().stream()
                    .filter(item -> uuids.contains(item.uuid()) && !hidden.contains(item.uuid()))
                    .toList();
        }

        @Override
        public SearchBatch<CatalogueItem> loadAfter(long lastId, int batchSize) {
            List<Map.Entry<Long, CatalogueItem>> page = rows.tailMap(lastId, false).entrySet().stream()
                    .limit(batchSize)
                    .toList();
            if (page.isEmpty()) {
                return SearchBatch.end(lastId);
            }
            List<CatalogueItem> documents = page.stream()
                    .map(Map.Entry::getValue)
                    .filter(item -> !hidden.contains(item.uuid()))
                    .toList();
            return new SearchBatch<>(documents, page.getLast().getKey());
        }

        @Override
        public List<SearchIndexTrigger<?>> triggers() {
            return List.of();
        }

        @Override
        public long countIndexable() {
            return rows.values().stream().filter(item -> !hidden.contains(item.uuid())).count();
        }
    }

    record CurrencyDocument(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("code") String code,
            @JsonProperty("currency_name") String currencyName
    ) implements SearchDocument {
    }

    /** A source over the real {@code currencies} table, triggered by {@link PlatformCurrency} writes. */
    static class CurrencySource implements SearchDocumentSource<CurrencyDocument> {

        private final NamedParameterJdbcTemplate jdbc;

        CurrencySource(NamedParameterJdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public SearchIndexDefinition definition() {
            return SearchIndexDefinition.of(CURRENCIES, 1, List.of("currency_name", "code"), List.of("code"), List.of("code"));
        }

        @Override
        public List<CurrencyDocument> loadByUuids(Collection<UUID> uuids) {
            return jdbc.query(
                    "SELECT uuid, code, currency_name FROM currencies WHERE active AND uuid IN (:uuids)",
                    new MapSqlParameterSource("uuids", uuids),
                    (rs, row) -> new CurrencyDocument(rs.getObject("uuid", UUID.class), rs.getString("code"),
                            rs.getString("currency_name")));
        }

        @Override
        public SearchBatch<CurrencyDocument> loadAfter(long lastId, int batchSize) {
            List<long[]> ids = new ArrayList<>();
            List<CurrencyDocument> documents = new ArrayList<>();
            jdbc.query("SELECT id, uuid, code, currency_name, active FROM currencies WHERE id > :lastId ORDER BY id LIMIT :limit",
                    new MapSqlParameterSource(Map.of("lastId", lastId, "limit", batchSize)),
                    rs -> {
                        ids.add(new long[]{rs.getLong("id")});
                        if (rs.getBoolean("active")) {
                            documents.add(new CurrencyDocument(rs.getObject("uuid", UUID.class), rs.getString("code"),
                                    rs.getString("currency_name")));
                        }
                    });
            return ids.isEmpty() ? SearchBatch.end(lastId) : new SearchBatch<>(documents, ids.getLast()[0]);
        }

        @Override
        public List<SearchIndexTrigger<?>> triggers() {
            return List.of(SearchIndexTrigger.direct(PlatformCurrency.class, BaseEntity::getUuid));
        }
    }

    @TestConfiguration
    static class SourcesConfiguration {

        @Bean
        CatalogueSource catalogueSource() {
            return new CatalogueSource();
        }

        @Bean
        CurrencySource currencySource(NamedParameterJdbcTemplate jdbc) {
            return new CurrencySource(jdbc);
        }
    }
}
