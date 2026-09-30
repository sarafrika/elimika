package apps.sarafrika.elimika.shared.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Search contract records")
class SearchContractTest {

    private static final SearchScope SCOPE = SearchScope.unrestricted("test");

    @Test
    @DisplayName("The matching strategy is optional and defaults to the engine's own")
    void matchingStrategyIsOptional() {
        SearchRequest plain = SearchRequest.of("courses", "java", null, SCOPE, 0, 10);
        SearchRequest legacy = new SearchRequest("courses", "java", null, SCOPE, List.of(), 0, 10, List.of(), null);

        assertThat(plain.matchingStrategy()).isNull();
        assertThat(legacy.matchingStrategy()).isNull();
        assertThat(plain.withMatchingStrategy(SearchRequest.MatchingStrategy.FREQUENCY))
                .extracting(SearchRequest::matchingStrategy, SearchRequest::text)
                .containsExactly(SearchRequest.MatchingStrategy.FREQUENCY, "java");
    }

    @Test
    @DisplayName("A hit built without scores carries no ranking score or distance")
    void hitWithoutScores() {
        SearchHit hit = new SearchHit(UUID.randomUUID(), Map.of(), null);

        assertThat(hit.rankingScore()).isNull();
        assertThat(hit.geoDistanceMeters()).isNull();
    }

    @Test
    @DisplayName("geoPoint sorts ascending on _geo from the given origin")
    void geoPointSort() {
        SearchSort sort = SearchSort.geoPoint(-1.29, 36.82);

        assertThat(sort.field()).isEqualTo(SearchIndexDefinition.GEO_ATTRIBUTE);
        assertThat(sort.direction()).isEqualTo(SearchSort.Direction.ASC);
        assertThat(sort.origin()).isEqualTo(new SearchGeoPoint(-1.29, 36.82));
        assertThat(sort.isGeo()).isTrue();
        assertThat(SearchSort.asc("title").isGeo()).isFalse();
    }
}
