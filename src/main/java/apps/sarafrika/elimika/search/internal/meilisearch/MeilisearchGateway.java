package apps.sarafrika.elimika.search.internal.meilisearch;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.shared.search.FederatedSearchResult;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link SearchGateway} and {@link SearchIndexAdmin} over Meilisearch's HTTP API.
 * <p>
 * Plain {@link RestClient} rather than the vendor SDK: the handful of endpoints used here are stable
 * and small, and keeping the engine behind this one class is what makes it replaceable.
 * <p>
 * Writes in Meilisearch are asynchronous tasks. Every write here waits for its task to finish -
 * polling {@code GET /tasks/{uid}} with backoff up to {@code task-wait-timeout} - and throws when the
 * task failed or did not finish in time. That turns a silent indexing failure into an exception the
 * durable listener lets escape, which keeps its event publication incomplete until a retry succeeds.
 * <p>
 * Connection failures and engine errors surface as {@link SearchUnavailableException}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class MeilisearchGateway implements SearchGateway, SearchIndexAdmin {

    private static final TypeReference<Map<String, Object>> DOCUMENT = new TypeReference<>() {
    };
    private static final Duration FIRST_POLL = Duration.ofMillis(25);
    private static final Duration MAX_POLL = Duration.ofSeconds(1);

    private final RestClient client;
    /** Same engine, longer read timeout: index creation, settings, swaps and deletes. */
    private final RestClient adminClient;
    private final ObjectMapper objectMapper;
    private final Duration taskWaitTimeout;

    public MeilisearchGateway(SearchProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        SearchProperties.Meilisearch settings = properties.getMeilisearch();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(settings.getConnectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.client = client(builder, settings, httpClient, settings.getReadTimeout());
        Duration adminTimeout = settings.getAdminReadTimeout() == null
                || settings.getAdminReadTimeout().compareTo(settings.getReadTimeout()) < 0
                ? settings.getReadTimeout() : settings.getAdminReadTimeout();
        this.adminClient = client(builder, settings, httpClient, adminTimeout);
        this.objectMapper = objectMapper;
        this.taskWaitTimeout = settings.getTaskWaitTimeout();
    }

    private static RestClient client(RestClient.Builder builder, SearchProperties.Meilisearch settings,
                                     HttpClient httpClient, Duration readTimeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        RestClient.Builder configured = builder.clone()
                .baseUrl(settings.getHost())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(settings.getApiKey())) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey());
        }
        return configured.build();
    }

    // ===== SearchGateway =====

    @Override
    public SearchPage search(SearchRequest request) {
        Map<String, Object> body = pagedQueryBody(request);
        JsonNode response = call("search " + request.index() + " [" + request.scope().label() + "]",
                () -> client.post()
                        .uri("/indexes/{uid}/search", request.index())
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class));
        return toPage(request, response);
    }

    @Override
    public List<SearchPage> multiSearchPerIndex(List<SearchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> queries = new ArrayList<>();
        for (SearchRequest request : requests) {
            Map<String, Object> query = pagedQueryBody(request);
            query.put("indexUid", request.index());
            queries.add(query);
        }
        JsonNode response = call("multi-search over " + requests.size() + " index(es)",
                () -> client.post().uri("/multi-search").body(Map.of("queries", queries)).retrieve().body(JsonNode.class));
        JsonNode results = response.path("results");
        if (!results.isArray() || results.size() != requests.size()) {
            throw new SearchUnavailableException("Search engine answered a multi-search of " + requests.size()
                    + " queries with " + results.size() + " result(s)");
        }
        List<SearchPage> pages = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            pages.add(toPage(requests.get(i), results.get(i)));
        }
        return pages;
    }

    private Map<String, Object> pagedQueryBody(SearchRequest request) {
        Map<String, Object> body = queryBody(request);
        body.put("page", request.page() + 1);
        body.put("hitsPerPage", request.size());
        body.put("showRankingScore", true);
        if (!request.sort().isEmpty()) {
            body.put("sort", MeilisearchFilterRenderer.renderSort(request.sort()));
        }
        if (!request.facets().isEmpty()) {
            body.put("facets", request.facets().stream().map(MeilisearchFilterRenderer::attribute).toList());
        }
        return body;
    }

    private SearchPage toPage(SearchRequest request, JsonNode response) {
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode hit : response.path("hits")) {
            hits.add(new SearchHit(uuidOf(hit), document(hit), formatted(hit), rankingScore(hit), geoDistance(hit)));
        }
        return new SearchPage(
                hits,
                response.path("totalHits").asLong(hits.size()),
                request.page(),
                request.size(),
                facetDistribution(response.path("facetDistribution")));
    }

    @Override
    public FederatedSearchResult multiSearch(List<SearchRequest> requests, int limit) {
        return federated(requests, 0, limit, false);
    }

    @Override
    public FederatedSearchResult federatedSearch(List<SearchRequest> requests, int offset, int limit) {
        return federated(requests, offset, limit, true);
    }

    private FederatedSearchResult federated(List<SearchRequest> requests, int offset, int limit, boolean sorted) {
        if (requests == null || requests.isEmpty()) {
            return new FederatedSearchResult(List.of(), 0);
        }
        List<Map<String, Object>> queries = new ArrayList<>();
        for (SearchRequest request : requests) {
            Map<String, Object> query = queryBody(request);
            query.put("indexUid", request.index());
            if (sorted && !request.sort().isEmpty()) {
                query.put("sort", MeilisearchFilterRenderer.renderSort(request.sort()));
            }
            queries.add(query);
        }
        Map<String, Object> body = Map.of(
                "federation", Map.of("limit", Math.max(1, limit), "offset", Math.max(0, offset)),
                "queries", queries);
        JsonNode response = call("federated search",
                () -> client.post().uri("/multi-search").body(body).retrieve().body(JsonNode.class));

        List<FederatedSearchResult.Hit> hits = new ArrayList<>();
        for (JsonNode hit : response.path("hits")) {
            JsonNode federation = hit.path("_federation");
            hits.add(new FederatedSearchResult.Hit(
                    federation.path("indexUid").asText(null),
                    uuidOf(hit),
                    document(hit),
                    formatted(hit),
                    federation.has("weightedRankingScore") ? federation.path("weightedRankingScore").asDouble() : null));
        }
        return new FederatedSearchResult(hits, response.path("estimatedTotalHits").asLong(hits.size()));
    }

    @Override
    public void upsert(String index, List<?> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        JsonNode task = call("upsert into " + index, () -> client.post()
                .uri(uri -> uri.path("/indexes/{uid}/documents").queryParam("primaryKey", "uuid").build(index))
                .body(documents)
                .retrieve()
                .body(JsonNode.class));
        awaitTask(task, "upsert " + documents.size() + " document(s) into " + index);
    }

    @Override
    public void delete(String index, Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return;
        }
        List<String> ids = uuids.stream().map(UUID::toString).toList();
        JsonNode task = call("delete from " + index, () -> client.post()
                .uri("/indexes/{uid}/documents/delete-batch", index)
                .body(ids)
                .retrieve()
                .body(JsonNode.class));
        awaitTask(task, "delete " + ids.size() + " document(s) from " + index);
    }

    // ===== SearchIndexAdmin =====

    @Override
    public void ensureIndex(String uid, SearchIndexDefinition definition) {
        JsonNode created = call("create index " + uid, () -> adminClient.post()
                .uri("/indexes")
                .body(Map.of("uid", uid, "primaryKey", definition.primaryKey()))
                .retrieve()
                .body(JsonNode.class));
        TaskOutcome outcome = awaitTaskOutcome(created, "create index " + uid);
        if (!outcome.succeeded() && !"index_already_exists".equals(outcome.errorCode())) {
            throw outcome.failure();
        }
        JsonNode updated = call("update settings of " + uid, () -> adminClient.patch()
                .uri("/indexes/{uid}/settings", uid)
                .body(settings(definition))
                .retrieve()
                .body(JsonNode.class));
        awaitTask(updated, "update settings of " + uid);
    }

    @Override
    public void swapIndexes(String first, String second) {
        JsonNode task = call("swap " + first + " and " + second, () -> adminClient.post()
                .uri("/swap-indexes")
                .body(List.of(Map.of("indexes", List.of(first, second))))
                .retrieve()
                .body(JsonNode.class));
        awaitTask(task, "swap " + first + " and " + second);
    }

    @Override
    public void deleteIndex(String uid) {
        JsonNode task;
        try {
            task = adminClient.delete().uri("/indexes/{uid}", uid).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }
            throw unavailable("delete index " + uid, ex);
        } catch (RestClientException ex) {
            throw unavailable("delete index " + uid, ex);
        }
        TaskOutcome outcome = awaitTaskOutcome(task, "delete index " + uid);
        if (!outcome.succeeded() && !"index_not_found".equals(outcome.errorCode())) {
            throw outcome.failure();
        }
    }

    @Override
    public Optional<SearchIndexStats> stats(String uid) {
        try {
            JsonNode stats = client.get().uri("/indexes/{uid}/stats", uid).retrieve().body(JsonNode.class);
            return Optional.of(new SearchIndexStats(
                    stats.path("numberOfDocuments").asLong(),
                    stats.path("isIndexing").asBoolean()));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw unavailable("read stats of " + uid, ex);
        } catch (RestClientException ex) {
            throw unavailable("read stats of " + uid, ex);
        }
    }

    /** Whether the engine answers {@code GET /health} with {@code available}. */
    public boolean isAvailable() {
        try {
            JsonNode health = client.get().uri("/health").retrieve().body(JsonNode.class);
            return health != null && "available".equals(health.path("status").asText());
        } catch (RestClientException ex) {
            return false;
        }
    }

    // ===== Request building =====

    private Map<String, Object> queryBody(SearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", request.hasText() ? request.text() : "");
        String filter = MeilisearchFilterRenderer.render(request.scope().filter(), request.filter());
        if (filter != null) {
            body.put("filter", filter);
        }
        if (request.searchOn() != null && !request.searchOn().isEmpty()) {
            body.put("attributesToSearchOn",
                    request.searchOn().stream().map(MeilisearchFilterRenderer::attribute).toList());
        }
        if (request.hasText()) {
            body.put("attributesToHighlight", List.of("*"));
        }
        if (request.matchingStrategy() != null) {
            body.put("matchingStrategy", request.matchingStrategy().name().toLowerCase(Locale.ROOT));
        }
        log.debug("Search on {} scoped to {}", request.index(), request.scope().label());
        return body;
    }

    private static Map<String, Object> settings(SearchIndexDefinition definition) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("searchableAttributes",
                definition.searchableAttributes().isEmpty() ? List.of("*") : definition.searchableAttributes());
        settings.put("displayedAttributes",
                definition.displayedAttributes().isEmpty() ? List.of("*") : definition.displayedAttributes());
        settings.put("filterableAttributes", definition.filterableAttributes());
        settings.put("sortableAttributes", definition.sortableAttributes());
        if (!definition.rankingRules().isEmpty()) {
            settings.put("rankingRules", definition.rankingRules());
        }
        settings.put("synonyms", definition.synonyms());
        settings.put("stopWords", definition.stopWords());
        settings.put("typoTolerance", Map.of("disableOnAttributes", definition.typoDisabledAttributes()));
        settings.put("pagination", Map.of("maxTotalHits", definition.maxTotalHits()));
        return settings;
    }

    // ===== Response mapping =====

    private Map<String, Object> document(JsonNode hit) {
        Map<String, Object> document = objectMapper.convertValue(hit, DOCUMENT);
        document.keySet().removeIf(key -> key.startsWith("_") && !SearchIndexDefinition.GEO_ATTRIBUTE.equals(key));
        return document;
    }

    private static Double rankingScore(JsonNode hit) {
        JsonNode score = hit.get("_rankingScore");
        return score == null || !score.isNumber() ? null : score.asDouble();
    }

    private static Integer geoDistance(JsonNode hit) {
        JsonNode distance = hit.get("_geoDistance");
        return distance == null || !distance.isNumber() ? null : (int) Math.round(distance.asDouble());
    }

    private Map<String, Object> formatted(JsonNode hit) {
        JsonNode formatted = hit.get("_formatted");
        return formatted == null || formatted.isNull() ? null : objectMapper.convertValue(formatted, DOCUMENT);
    }

    private static UUID uuidOf(JsonNode hit) {
        String value = hit.path("uuid").asText(null);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, Map<String, Long>> facetDistribution(JsonNode node) {
        Map<String, Map<String, Long>> distribution = new LinkedHashMap<>();
        node.properties().forEach(facet -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            facet.getValue().properties().forEach(value -> counts.put(value.getKey(), value.getValue().asLong()));
            distribution.put(facet.getKey(), counts);
        });
        return distribution;
    }

    // ===== Tasks =====

    private void awaitTask(JsonNode task, String description) {
        TaskOutcome outcome = awaitTaskOutcome(task, description);
        if (!outcome.succeeded()) {
            throw outcome.failure();
        }
    }

    private TaskOutcome awaitTaskOutcome(JsonNode task, String description) {
        if (task == null || !task.has("taskUid")) {
            throw new SearchUnavailableException("Search engine returned no task for " + description);
        }
        long taskUid = task.path("taskUid").asLong();
        long deadline = System.nanoTime() + taskWaitTimeout.toNanos();
        Duration pause = FIRST_POLL;
        while (true) {
            JsonNode status = call("poll task " + taskUid,
                    () -> client.get().uri("/tasks/{uid}", taskUid).retrieve().body(JsonNode.class));
            String state = status.path("status").asText();
            switch (state) {
                case "succeeded" -> {
                    return TaskOutcome.success();
                }
                case "failed", "canceled" -> {
                    JsonNode error = status.path("error");
                    return TaskOutcome.failed(error.path("code").asText(state), new SearchUnavailableException(
                            "Search task " + taskUid + " to " + description + " " + state + ": "
                                    + error.path("message").asText("no detail")));
                }
                default -> {
                    // enqueued or processing
                }
            }
            if (System.nanoTime() > deadline) {
                throw new SearchUnavailableException("Search task " + taskUid + " to " + description
                        + " did not finish within " + taskWaitTimeout);
            }
            sleep(pause);
            pause = pause.multipliedBy(2).compareTo(MAX_POLL) > 0 ? MAX_POLL : pause.multipliedBy(2);
        }
    }

    private static void sleep(Duration pause) {
        try {
            Thread.sleep(pause.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SearchUnavailableException("Interrupted while waiting for a search task", ex);
        }
    }

    private record TaskOutcome(boolean succeeded, String errorCode, SearchUnavailableException failure) {
        static TaskOutcome success() {
            return new TaskOutcome(true, null, null);
        }

        static TaskOutcome failed(String errorCode, SearchUnavailableException failure) {
            return new TaskOutcome(false, errorCode, failure);
        }
    }

    // ===== Errors =====

    private static <T> T call(String description, Supplier<T> request) {
        try {
            T result = request.get();
            if (result == null) {
                throw new SearchUnavailableException("Search engine returned an empty response to " + description);
            }
            return result;
        } catch (RestClientException ex) {
            throw unavailable(description, ex);
        }
    }

    private static SearchUnavailableException unavailable(String description, RestClientException ex) {
        if (ex instanceof ResourceAccessException) {
            return new SearchUnavailableException("Search engine unreachable during " + description, ex);
        }
        if (ex instanceof RestClientResponseException response) {
            HttpStatusCode status = response.getStatusCode();
            return new SearchUnavailableException("Search engine answered " + status.value() + " to " + description
                    + ": " + response.getResponseBodyAsString(), ex);
        }
        return new SearchUnavailableException("Search engine call failed during " + description, ex);
    }
}
