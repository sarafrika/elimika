package apps.sarafrika.elimika.timetabling.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleSummaryTest {

    @ParameterizedTest
    @CsvSource(delimiter = '#', value = {
            "WEEKLY|MONDAY,WEDNESDAY|2031-05-05 09:00|11:00 # Mon & Wed · 9:00–11:00",
            "WEEKLY|FRIDAY,monday,WEDNESDAY|2031-05-05 18:30|20:00 # Mon, Wed & Fri · 18:30–20:00",
            "WEEKLY||2031-05-07 09:00|10:00 # Wed · 9:00–10:00",
            "DAILY||2031-05-05 07:00|08:00 # Daily · 7:00–8:00",
            "MONTHLY||2031-05-15 10:00|12:00 # Monthly · 10:00–12:00",
            "||2031-05-10 14:30|16:30 # Sat 10 May · 14:30–16:30"
    })
    void describesEachKindOfTemplate(String packed, String expected) {
        assertThat(ScheduleSummary.fromPackedTemplates(packed)).isEqualTo(expected);
    }

    @Test
    void joinsDistinctTemplatesInOrder() {
        assertThat(ScheduleSummary.fromPackedTemplates(
                "WEEKLY|MONDAY|2031-05-05 09:00|11:00;WEEKLY|THURSDAY|2031-05-08 14:00|15:00;WEEKLY|MONDAY|2031-05-12 09:00|11:00"))
                .isEqualTo("Mon · 9:00–11:00; Thu · 14:00–15:00");
    }

    @Test
    void skipsWhatItCannotRead() {
        assertThat(ScheduleSummary.fromPackedTemplates(null)).isNull();
        assertThat(ScheduleSummary.fromPackedTemplates("")).isNull();
        assertThat(ScheduleSummary.fromPackedTemplates("WEEKLY|MONDAY|not a time|11:00")).isNull();
    }
}
