package apps.sarafrika.elimika.shared.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("QueryStringRedactor")
class QueryStringRedactorTest {

    @ParameterizedTest
    @NullAndEmptySource
    void leavesNullAndEmptyUntouched(String queryString) {
        assertThat(QueryStringRedactor.redact(queryString)).isEqualTo(queryString);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "q=jane                 | q=[redacted:4]",
            "search=math            | search=[redacted:4]",
            "near=Nairobi           | near=[redacted:7]",
            "lat=-1.29&lng=36.82    | lat=[redacted:5]&lng=[redacted:5]",
            "email=a@b.co           | email=[redacted:6]",
            "recipient_email=a@b.co | recipient_email=[redacted:6]",
            "email_eq=a@b.co        | email_eq=[redacted:6]",
            "email_like=jane        | email_like=[redacted:4]",
            "first_name_like=jan    | first_name_like=[redacted:3]",
            "title_startswith=Intro | title_startswith=[redacted:5]",
            "title_endswith=101     | title_endswith=[redacted:3]",
    })
    void redactsSensitiveValues(String input, String expected) {
        assertThat(QueryStringRedactor.redact(input)).isEqualTo(expected);
    }

    @Test
    void matchesKeysCaseInsensitively() {
        assertThat(QueryStringRedactor.redact("Q=x&SEARCH=yy&Recipient_EMAIL=zzz&Name_LIKE=a"))
                .isEqualTo("Q=[redacted:1]&SEARCH=[redacted:2]&Recipient_EMAIL=[redacted:3]&Name_LIKE=[redacted:1]");
    }

    @Test
    void measuresDecodedLengthOfUrlEncodedValues() {
        assertThat(QueryStringRedactor.redact("email=jane%40example.com&q=caf%C3%A9"))
                .isEqualTo("email=[redacted:16]&q=[redacted:4]");
    }

    @Test
    void decodesEncodedKeys() {
        assertThat(QueryStringRedactor.redact("%71=secret&user%5Femail=a%40b"))
                .isEqualTo("%71=[redacted:6]&user%5Femail=[redacted:3]");
    }

    @Test
    void treatsPlusAsSpace() {
        assertThat(QueryStringRedactor.redact("q=jane+doe")).isEqualTo("q=[redacted:8]");
    }

    @Test
    void redactsEveryRepeatedKey() {
        assertThat(QueryStringRedactor.redact("q=a&page=0&q=bcd&q=ef"))
                .isEqualTo("q=[redacted:1]&page=0&q=[redacted:3]&q=[redacted:2]");
    }

    @Test
    void keepsParametersWithoutValues() {
        assertThat(QueryStringRedactor.redact("q&search=&email=&flag&q=x"))
                .isEqualTo("q&search=&email=&flag&q=[redacted:1]");
    }

    @Test
    void toleratesMalformedPairsAndEncoding() {
        assertThat(QueryStringRedactor.redact("&&=orphan&q=100%&q=%zz&a=b=c&q=x=y&"))
                .isEqualTo("&&=orphan&q=[redacted:4]&q=[redacted:3]&a=b=c&q=[redacted:3]&");
    }

    @Test
    void leavesNonSensitiveParametersByteIdentical() {
        String query = "user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001&action=approve"
                + "&page=0&size=20&sort=created_date%2Cdesc&status_in=ACTIVE,PENDING&weird=%zz";
        assertThat(QueryStringRedactor.redact(query)).isEqualTo(query);
    }

    @Test
    void keepsUuidFiltersIntactAlongsideRedactedSearch() {
        assertThat(QueryStringRedactor.redact("q=jane&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001"))
                .isEqualTo("q=[redacted:4]&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001");
    }

    @Test
    void doesNotTreatSimilarKeysAsSensitive() {
        String query = "query=x&search_type=y&latitude_bucket=z&like=w&q_mode=v";
        assertThat(QueryStringRedactor.redact(query)).isEqualTo(query);
    }

    @Test
    @DisplayName("Redaction is a fixed point: an already redacted string comes back unchanged")
    void isIdempotent() {
        String once = QueryStringRedactor.redact("q=jane+doe&page=2&email_eq=a%40b.co");

        assertThat(once).isEqualTo("q=[redacted:8]&page=2&email_eq=[redacted:6]");
        assertThat(QueryStringRedactor.redact(once)).isEqualTo(once);
    }
}
