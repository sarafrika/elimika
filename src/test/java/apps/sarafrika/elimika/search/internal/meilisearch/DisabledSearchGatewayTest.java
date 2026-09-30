package apps.sarafrika.elimika.search.internal.meilisearch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DisabledSearchGateway")
class DisabledSearchGatewayTest {

    @Test
    @DisplayName("Geo and matching-strategy requests are refused like any other while search is off")
    void refusesGeoRequests() {
        SearchRequest request = new SearchRequest("venues", "hall", SearchFilter.geoRadius(-1.29, 36.82, 2000),
                SearchScope.unrestricted("test"), List.of(SearchSort.geoPoint(-1.29, 36.82)), 0, 10, List.of(), null,
                SearchRequest.MatchingStrategy.ALL);
        DisabledSearchGateway gateway = new DisabledSearchGateway();

        assertThatThrownBy(() -> gateway.search(request)).isInstanceOf(SearchUnavailableException.class);
        assertThatThrownBy(() -> gateway.multiSearchPerIndex(List.of(request)))
                .isInstanceOf(SearchUnavailableException.class);
        assertThatThrownBy(() -> gateway.multiSearch(List.of(request), 5))
                .isInstanceOf(SearchUnavailableException.class);
    }
}
