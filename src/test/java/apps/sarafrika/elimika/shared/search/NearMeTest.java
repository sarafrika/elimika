package apps.sarafrika.elimika.shared.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Near-me parameters")
class NearMeTest {

    @Test
    @DisplayName("near is rounded to 2 decimal places before it is used")
    void roundsNear() {
        NearMe near = NearMe.parse("-1.292066, 36.821946", null).orElseThrow();

        assertThat(near.lat()).isEqualTo(-1.29);
        assertThat(near.lng()).isEqualTo(36.82);
        assertThat(near.filter()).isEqualTo(new SearchFilter.GeoRadius(-1.29, 36.82, 10_000));
        assertThat(near.sort().origin()).isEqualTo(new SearchGeoPoint(-1.29, 36.82));
    }

    @ParameterizedTest(name = "radius_km={0} -> {1} km")
    @CsvSource(nullValues = "null", value = {
            "null, 10",
            "'', 10",
            "0, 2",
            "0.4, 2",
            "1, 2",
            "2, 2",
            "25, 25",
            "100, 100",
            "101, 100",
            "5000, 100",
            "-3, 2"})
    @DisplayName("The radius is clamped to 2-100 km and defaults to 10")
    void clampsRadius(String radiusKm, int expected) {
        assertThat(NearMe.parse("-1.29,36.82", radiusKm).orElseThrow().radiusKm()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Without near there is no near-me search, and radius_km alone is a 400")
    void nearIsOptional() {
        assertThat(NearMe.parse(null, null)).isEmpty();
        assertThat(NearMe.parse("  ", null)).isEmpty();
        assertThat(NearMe.from(Map.of("q", "maths"))).isEmpty();
        assertThatThrownBy(() -> NearMe.parse(null, "5")).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"-1.2921", "-1.2921,36.8219,4", "abc,36.8219", "91.5,36.8219", "-1.2921,181"})
    @DisplayName("A malformed or out-of-range near is a 400 that never echoes the value")
    void rejectsBadNear(String near) {
        assertThatThrownBy(() -> NearMe.parse(near, null))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("1.2921").doesNotContain("8219"));
    }

    @Test
    @DisplayName("A non-numeric radius is a 400")
    void rejectsBadRadius() {
        assertThatThrownBy(() -> NearMe.parse("-1.29,36.82", "far")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NearMe.parse("-1.29,36.82", "NaN")).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0} m -> {1}")
    @CsvSource(nullValues = "null", value = {
            "null, null",
            "0, <2 km",
            "1999, <2 km",
            "2000, 2-5 km",
            "4999, 2-5 km",
            "5000, 5-10 km",
            "9999, 5-10 km",
            "10000, 10-25 km",
            "24999, 10-25 km",
            "25000, >25 km",
            "140000, >25 km"})
    @DisplayName("Distances become coarse bands, never metres")
    void bandsDistances(Integer meters, String band) {
        assertThat(NearMe.distanceBand(meters)).isEqualTo(band);
    }

    @Test
    @DisplayName("Without text distance sorts first; with text it is the last tie-breaker")
    void ordersByDistanceWithoutText() {
        NearMe near = NearMe.parse("-1.29,36.82", null).orElseThrow();
        List<SearchSort> requested = List.of(SearchSort.desc("created_at"));

        assertThat(near.sorts(false, requested)).extracting(SearchSort::field)
                .containsExactly(SearchIndexDefinition.GEO_ATTRIBUTE, "created_at");
        assertThat(near.sorts(true, requested)).extracting(SearchSort::field)
                .containsExactly("created_at", SearchIndexDefinition.GEO_ATTRIBUTE);
    }

    @Test
    @DisplayName("Hits become bands keyed by document, from the module's rounded points when the engine reports no distance")
    void bandsPerHit() {
        UUID reported = UUID.randomUUID();
        UUID located = UUID.randomUUID();
        UUID unlocatable = UUID.randomUUID();
        SearchPage page = new SearchPage(List.of(
                new SearchHit(reported, Map.of(), null, null, 30_000),
                new SearchHit(located, Map.of(), null, null, null),
                new SearchHit(unlocatable, Map.of(), null, null, null)), 3, 0, 20, Map.of());
        NearMe near = NearMe.parse("-1.30,36.83", null).orElseThrow();

        assertThat(near.distanceBands(page, Map.of(located, new SearchGeoPoint(-1.29, 36.82))))
                .containsOnly(Map.entry(reported, ">25 km"), Map.entry(located, "<2 km"));
    }

    @Test
    @DisplayName("Distances are great-circle metres between the rounded points")
    void measuresDistance() {
        // One hundredth of a degree of longitude at the equator is about 1.11 km.
        assertThat(NearMe.meters(0, 36.82, 0, 36.83)).isBetween(1_100.0, 1_120.0);
        NearMe near = NearMe.parse("-1.29,36.83", null).orElseThrow();
        assertThat(near.distanceBand(new SearchGeoPoint(-1.29, 36.82))).isEqualTo("<2 km");
        assertThat(near.distanceBand(new SearchGeoPoint(-1.29, 36.86))).isEqualTo("2-5 km");
        assertThat(near.distanceBand(new SearchGeoPoint(-1.29, 38.20))).isEqualTo(">25 km");
        assertThat(near.distanceBand((SearchGeoPoint) null)).isNull();
    }

    @Test
    @DisplayName("Indexed points are rounded to 2 decimal places, and a missing coordinate means no point")
    void roundsIndexedPoints() {
        assertThat(SearchGeoPoint.rounded(new BigDecimal("-1.292066"), new BigDecimal("36.821946")))
                .isEqualTo(new SearchGeoPoint(-1.29, 36.82));
        assertThat(SearchGeoPoint.rounded(null, new BigDecimal("36.82"))).isNull();
        assertThat(SearchGeoPoint.rounded(new BigDecimal("-1.29"), null)).isNull();
    }

    @Test
    @DisplayName("near and radius_km are never read as attribute filters")
    void nearIsReserved() {
        SearchIndexDefinition definition = SearchIndexDefinition.of("things", 1, List.of("title"), List.of("title"),
                List.of());
        assertThat(SearchParamsTranslator.toFilter(Map.of("near", "-1.29,36.82", "radius_km", "5"), definition))
                .isNull();
    }

    record Doc(@com.fasterxml.jackson.annotation.JsonProperty("uuid") UUID uuid,
               @com.fasterxml.jackson.annotation.JsonProperty("title") String title,
               @com.fasterxml.jackson.annotation.JsonProperty("_geo") SearchGeoPoint geo) {
    }

    @Test
    @DisplayName("Displayed attributes list every document attribute except _geo")
    void displayedAttributesDropGeo() {
        assertThat(SearchDocumentAttributes.displayedWithoutGeo(Doc.class)).containsExactly("uuid", "title");
    }
}
