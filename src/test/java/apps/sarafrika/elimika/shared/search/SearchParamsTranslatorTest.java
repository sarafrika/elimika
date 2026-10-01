package apps.sarafrika.elimika.shared.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchParamsTranslator")
class SearchParamsTranslatorTest {

    private static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(
            "courses",
            1,
            List.of("title", "description"),
            List.of("status", "organisation_uuid", "price", "published_at", "is_free", "category_uuids"),
            List.of("title", "created_at", "price"));

    private static Map<String, String> params(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("A bare key is an equality on the attribute")
    void bareKeyIsEquality() {
        SearchFilter filter = SearchParamsTranslator.toFilter(params("status", "published"), DEFINITION);

        assertThat(filter).isEqualTo(SearchFilter.eq("status", "published"));
    }

    @Test
    @DisplayName("Every supported operator maps to the matching filter")
    void operators() {
        UUID org = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertThat(SearchParamsTranslator.toFilter(params("status_eq", "draft"), DEFINITION))
                .isEqualTo(SearchFilter.eq("status", "draft"));
        assertThat(SearchParamsTranslator.toFilter(params("status_noteq", "draft"), DEFINITION))
                .isEqualTo(SearchFilter.not(SearchFilter.eq("status", "draft")));
        assertThat(SearchParamsTranslator.toFilter(params("category_uuids_in", a + "," + b), DEFINITION))
                .isEqualTo(SearchFilter.in("category_uuids", List.of(a, b)));
        assertThat(SearchParamsTranslator.toFilter(params("status_notin", "draft,archived"), DEFINITION))
                .isEqualTo(SearchFilter.not(SearchFilter.in("status", List.of("draft", "archived"))));
        assertThat(SearchParamsTranslator.toFilter(params("price_gt", "10"), DEFINITION))
                .isEqualTo(SearchFilter.gt("price", new BigDecimal("10")));
        assertThat(SearchParamsTranslator.toFilter(params("price_gte", "10.5"), DEFINITION))
                .isEqualTo(SearchFilter.gte("price", new BigDecimal("10.5")));
        assertThat(SearchParamsTranslator.toFilter(params("price_lt", "10"), DEFINITION))
                .isEqualTo(SearchFilter.lt("price", new BigDecimal("10")));
        assertThat(SearchParamsTranslator.toFilter(params("price_lte", "10"), DEFINITION))
                .isEqualTo(SearchFilter.lte("price", new BigDecimal("10")));
        assertThat(SearchParamsTranslator.toFilter(params("price_between", "5,20"), DEFINITION))
                .isEqualTo(SearchFilter.between("price", new BigDecimal("5"), new BigDecimal("20")));
        assertThat(SearchParamsTranslator.toFilter(params("organisation_uuid", org.toString()), DEFINITION))
                .isEqualTo(SearchFilter.eq("organisation_uuid", org));
        assertThat(SearchParamsTranslator.toFilter(params("is_free", "TRUE"), DEFINITION))
                .isEqualTo(SearchFilter.eq("is_free", true));
    }

    @Test
    @DisplayName("ISO dates become UTC epoch seconds, which is how documents store instants")
    void datesBecomeEpochSeconds() {
        assertThat(SearchParamsTranslator.toFilter(params("published_at_gte", "2025-01-01T00:00:00"), DEFINITION))
                .isEqualTo(SearchFilter.gte("published_at", 1735689600L));
        assertThat(SearchParamsTranslator.toFilter(params("published_at_lt", "2025-01-02"), DEFINITION))
                .isEqualTo(SearchFilter.lt("published_at", 1735776000L));
    }

    @Test
    @DisplayName("camelCase and snake_case keys resolve to the definition's attribute name")
    void camelAndSnakeKeys() {
        UUID org = UUID.randomUUID();

        assertThat(SearchParamsTranslator.toFilter(params("organisationUuid", org.toString()), DEFINITION))
                .isEqualTo(SearchFilter.eq("organisation_uuid", org));
        assertThat(SearchParamsTranslator.toFilter(params("publishedAt_gte", "2025-01-01"), DEFINITION))
                .isEqualTo(SearchFilter.gte("published_at", 1735689600L));
        assertThat(SearchParamsTranslator.toFilter(params("organisation_uuid", org.toString()), DEFINITION))
                .isEqualTo(SearchFilter.eq("organisation_uuid", org));
    }

    @Test
    @DisplayName("Several keys are ANDed together")
    void keysAreAnded() {
        SearchFilter filter = SearchParamsTranslator.toFilter(params("status", "published", "price_lte", "0"), DEFINITION);

        assertThat(filter).isEqualTo(SearchFilter.and(
                SearchFilter.eq("status", "published"),
                SearchFilter.lte("price", new BigDecimal("0"))));
    }

    @Test
    @DisplayName("Paging, sorting, text and facet parameters are not filters")
    void reservedKeysAreSkipped() {
        SearchFilter filter = SearchParamsTranslator.toFilter(
                params("page", "2", "size", "20", "sort", "title,asc", "q", "java", "facets", "status", "Page", "1"),
                DEFINITION);

        assertThat(filter).isNull();
    }

    @Test
    @DisplayName("A key outside the filterable allow-list is rejected, not ignored")
    void unknownKeyIsRejected() {
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("password_hash", "x"), DEFINITION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported search field: password_hash");
        // Searchable is not filterable.
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("title", "x"), DEFINITION))
                .isInstanceOf(IllegalArgumentException.class);
        // An operator suffix does not smuggle an unknown field through.
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("secret_in", "a,b"), DEFINITION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("The rejected key is echoed back only as a sanitised identifier")
    void rejectedKeyIsSanitised() {
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("x\" OR 1=1 <script>", "v"), DEFINITION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported search field: xOR11script");
    }

    @Test
    @DisplayName("A between filter needs exactly two bounds")
    void betweenNeedsTwoBounds() {
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("price_between", "5"), DEFINITION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Sort expressions resolve against the sortable allow-list")
    void sortTranslation() {
        assertThat(SearchParamsTranslator.toSort("title,desc", DEFINITION))
                .containsExactly(SearchSort.desc("title"));
        assertThat(SearchParamsTranslator.toSort("createdAt", DEFINITION))
                .containsExactly(SearchSort.asc("created_at"));
        assertThat(SearchParamsTranslator.toSort("price,asc,created_at,desc", DEFINITION))
                .containsExactly(SearchSort.asc("price"), SearchSort.desc("created_at"));
        assertThat(SearchParamsTranslator.toSort(null, DEFINITION)).isEmpty();
    }

    @Test
    @DisplayName("Sorting by an attribute that is not sortable is rejected")
    void unknownSortIsRejected() {
        assertThatThrownBy(() -> SearchParamsTranslator.toSort("status,asc", DEFINITION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort property: status");
    }

    @Test
    @DisplayName("_geo is never a plain filter or sort key, even when the index lists it")
    void geoIsNotAPlainKey() {
        SearchIndexDefinition geo = SearchIndexDefinition.of("venues", 1, List.of("name"),
                List.of("status", "_geo"), List.of("name", "_geo"));

        assertThat(geo.geoFilterable()).isTrue();
        assertThat(geo.geoSortable()).isTrue();
        assertThat(DEFINITION.geoFilterable()).isFalse();
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("_geo", "1,2"), geo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params("geo_eq", "1"), geo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchParamsTranslator.toSort("_geo,asc", geo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("A removed text operator gets the same message as on the database path, not 'Unsupported search field'")
    void removedTextOperatorUsesTheDocumentedMessage() {
        for (String key : List.of("title_like", "title_startswith", "description_ENDSWITH")) {
            assertThatThrownBy(() -> SearchParamsTranslator.toFilter(params(key, "kube"), DEFINITION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Text operators were removed; use the q parameter for text search (rejected: " + key + ")");
        }
    }
}
