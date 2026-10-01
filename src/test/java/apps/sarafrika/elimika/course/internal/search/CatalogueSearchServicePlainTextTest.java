package apps.sarafrika.elimika.course.internal.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogueSearchServicePlainTextTest {

    @Test
    void stripsRichTextToASingleLineSummary() {
        String html = "<p></p><h3>🌸 Flower Gardening Course</h3><p>Learn how to <strong>grow</strong>&nbsp;flowers.</p>";

        assertThat(CatalogueSearchService.plainText(html))
                .isEqualTo("🌸 Flower Gardening Course Learn how to grow flowers.");
    }

    @Test
    void decodesEntitiesOnce() {
        assertThat(CatalogueSearchService.plainText("<strong>Beauty &amp; Beauty Therapy</strong>"))
                .isEqualTo("Beauty & Beauty Therapy");
    }

    @Test
    void returnsNullForEmptyMarkup() {
        assertThat(CatalogueSearchService.plainText("<p></p><br>")).isNull();
        assertThat(CatalogueSearchService.plainText(null)).isNull();
    }
}
