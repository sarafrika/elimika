package apps.sarafrika.elimika.search.internal.meilisearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchSort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MeilisearchFilterRenderer")
class MeilisearchFilterRendererTest {

    @Test
    @DisplayName("Every node type renders to Meilisearch syntax with every value quoted")
    void rendersEveryNode() {
        UUID uuid = UUID.fromString("7b1e4a1c-0000-4000-8000-000000000001");

        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("status", "published")))
                .isEqualTo("status = \"published\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("organisation_uuid", uuid)))
                .isEqualTo("organisation_uuid = \"7b1e4a1c-0000-4000-8000-000000000001\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("is_free", true)))
                .isEqualTo("is_free = \"true\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.in("status", "a", "b")))
                .isEqualTo("status IN [\"a\", \"b\"]");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.gte("price", new BigDecimal("1E+2"))))
                .isEqualTo("price >= \"100\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.between("price", 5, 20)))
                .isEqualTo("(price >= \"5\" AND price <= \"20\")");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.lt("published_at", 1735689600L)))
                .isEqualTo("published_at < \"1735689600\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.isNull("deleted_at")))
                .isEqualTo("deleted_at IS NULL");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.notEq("status", "draft")))
                .isEqualTo("NOT (status = \"draft\")");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.or(
                SearchFilter.eq("a", "1"),
                SearchFilter.and(SearchFilter.eq("b", "2"), SearchFilter.eq("c", "3")))))
                .isEqualTo("(a = \"1\") OR ((b = \"2\") AND (c = \"3\"))");
    }

    @Test
    @DisplayName("The scope is always ANDed with the caller's filter")
    void scopeIsAndedWithRequestFilter() {
        SearchFilter scope = SearchFilter.eq("visibility", "public");
        SearchFilter request = SearchFilter.or(SearchFilter.eq("status", "a"), SearchFilter.eq("status", "b"));

        assertThat(MeilisearchFilterRenderer.render(scope, request))
                .isEqualTo("(visibility = \"public\") AND ((status = \"a\") OR (status = \"b\"))");
        assertThat(MeilisearchFilterRenderer.render(scope, null)).isEqualTo("visibility = \"public\"");
        assertThat(MeilisearchFilterRenderer.render(null, null)).isNull();
    }

    @Test
    @DisplayName("Quotes and backslashes in values are escaped")
    void escapesQuotesAndBackslashes() {
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("title", "say \"hi\"")))
                .isEqualTo("title = \"say \\\"hi\\\"\"");
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("path", "C:\\temp\\")))
                .isEqualTo("path = \"C:\\\\temp\\\\\"");
    }

    @Test
    @DisplayName("A value cannot break out of its quotes to widen the scope")
    void injectionStaysInsideTheValue() {
        SearchFilter scope = SearchFilter.eq("organisation_uuid", "org-1");
        SearchFilter hostile = SearchFilter.eq("status", "x\" OR organisation_uuid = \"org-2");

        String rendered = MeilisearchFilterRenderer.render(scope, hostile);

        assertThat(rendered).isEqualTo(
                "(organisation_uuid = \"org-1\") AND (status = \"x\\\" OR organisation_uuid = \\\"org-2\")");
        // Stripping escaped quotes leaves exactly the four structural quotes of the two values.
        assertThat(rendered.replace("\\\"", "").chars().filter(c -> c == '"').count()).isEqualTo(4);
    }

    @Test
    @DisplayName("A trailing backslash cannot escape the closing quote")
    void trailingBackslashCannotEscapeTheQuote() {
        String rendered = MeilisearchFilterRenderer.render(SearchFilter.and(
                SearchFilter.eq("a", "x\\"),
                SearchFilter.eq("b", " OR b = \"y")));

        assertThat(rendered).isEqualTo("(a = \"x\\\\\") AND (b = \" OR b = \\\"y\")");
    }

    @Test
    @DisplayName("Attribute names outside [A-Za-z0-9_.] are rejected")
    void rejectsHostileAttributeNames() {
        assertThatThrownBy(() -> MeilisearchFilterRenderer.render(SearchFilter.eq("status = \"x\" OR a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MeilisearchFilterRenderer.render(SearchFilter.isNull("a b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MeilisearchFilterRenderer.renderSort(List.of(SearchSort.asc("title:desc"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(MeilisearchFilterRenderer.render(SearchFilter.eq("category.uuid", "c")))
                .isEqualTo("category.uuid = \"c\"");
    }

    @Test
    @DisplayName("Sorts render as attribute:direction")
    void rendersSort() {
        assertThat(MeilisearchFilterRenderer.renderSort(List.of(SearchSort.asc("title"), SearchSort.desc("created_at"))))
                .containsExactly("title:asc", "created_at:desc");
    }

    @Test
    @DisplayName("Unsupported value types never reach the renderer")
    void rejectsUnsupportedValues() {
        assertThatThrownBy(() -> SearchFilter.eq("a", new Object())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchFilter.eq("a", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchFilter.eq("a", Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
