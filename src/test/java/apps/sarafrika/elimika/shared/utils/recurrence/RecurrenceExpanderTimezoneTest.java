package apps.sarafrika.elimika.shared.utils.recurrence;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expansion must walk local dates: the staging regression put every "WEDNESDAY"
 * occurrence of a 00:10 EAT class on a Thursday.
 */
class RecurrenceExpanderTimezoneTest {

    private static final String NAIROBI = "Africa/Nairobi";
    private static final String NEW_YORK = "America/New_York";
    private static final String LONDON = "Europe/London";

    // 2026-09-09T00:10+03:00, the staging template that produced Thursday instances
    private static final LocalDateTime STAGING_START = LocalDateTime.of(2026, 9, 8, 21, 10);
    private static final LocalDateTime STAGING_END = LocalDateTime.of(2026, 9, 8, 22, 10);

    @Test
    void stagingWeeklyRuleLandsOnLocalWednesdays() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "WEDNESDAY", null, LocalDate.of(2026, 12, 9), null);

        List<OccurrenceWindow> windows = RecurrenceExpander.expand(
                STAGING_START, STAGING_END, pattern, NAIROBI);

        assertThat(windows).hasSize(14);
        assertThat(windows.getFirst()).isEqualTo(new OccurrenceWindow(STAGING_START, STAGING_END));
        assertThat(windows.getLast().start()).isEqualTo(LocalDateTime.of(2026, 12, 8, 21, 10));
        assertThat(windows).allSatisfy(window -> {
            assertThat(localStart(window, NAIROBI).getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
            assertThat(localStart(window, NAIROBI).toLocalTime()).isEqualTo(LocalTime.of(0, 10));
        });
    }

    @Test
    void nullZoneRepeatsHistoricUtcExpansion() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "WEDNESDAY", null, LocalDate.of(2026, 12, 9), null);

        List<OccurrenceWindow> legacy = RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern);

        assertThat(legacy).isEqualTo(RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern, "UTC"));
        assertThat(legacy.getFirst().start()).isEqualTo(LocalDateTime.of(2026, 9, 9, 21, 10));
    }

    @Test
    void utcZoneMatchesLegacyOverloadAcrossFrequencies() {
        List<RecurrencePattern> patterns = List.of(
                new RecurrencePattern(RecurrenceFrequency.DAILY, 2, null, null, LocalDate.of(2026, 9, 20), null),
                new RecurrencePattern(RecurrenceFrequency.WEEKLY, 2, "TUESDAY,FRIDAY", null, null, 6),
                new RecurrencePattern(RecurrenceFrequency.MONTHLY, 1, null, 31, null, 4));

        for (RecurrencePattern pattern : patterns) {
            assertThat(RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern, "UTC"))
                    .isEqualTo(RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern));
        }
    }

    @Test
    void blankAndUnusableZonesFallBackToUtc() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "WEDNESDAY", null, null, 3);
        List<OccurrenceWindow> legacy = RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern);

        assertThat(RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern, "   ")).isEqualTo(legacy);
        assertThat(RecurrenceExpander.expand(STAGING_START, STAGING_END, pattern, "Not/AZone")).isEqualTo(legacy);
    }

    @Test
    void lateEveningLocalTimeWhoseUtcInstantIsTheNextDayKeepsLocalWeekday() {
        // 2026-06-10T22:30-04:00, a Wednesday evening in New York
        LocalDateTime start = LocalDateTime.of(2026, 6, 11, 2, 30);
        LocalDateTime end = LocalDateTime.of(2026, 6, 11, 3, 30);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "WEDNESDAY", null, null, 3);

        assertThat(RecurrenceExpander.expand(start, end, pattern, NEW_YORK)).containsExactly(
                window(LocalDateTime.of(2026, 6, 11, 2, 30)),
                window(LocalDateTime.of(2026, 6, 18, 2, 30)),
                window(LocalDateTime.of(2026, 6, 25, 2, 30)));
        assertThat(localStart(RecurrenceExpander.expand(start, end, pattern, NEW_YORK).getFirst(), NEW_YORK))
                .isEqualTo(LocalDateTime.of(2026, 6, 10, 22, 30));
    }

    @Test
    void dailyEndDateBoundsTheSeriesByLocalDates() {
        // 2026-09-09T00:30+03:00 through the requester's 11 September
        LocalDateTime start = LocalDateTime.of(2026, 9, 8, 21, 30);
        LocalDateTime end = LocalDateTime.of(2026, 9, 8, 22, 30);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 1, null, null, LocalDate.of(2026, 9, 11), null);

        assertThat(RecurrenceExpander.expand(start, end, pattern, NAIROBI)).containsExactly(
                window(LocalDateTime.of(2026, 9, 8, 21, 30), 60),
                window(LocalDateTime.of(2026, 9, 9, 21, 30), 60),
                window(LocalDateTime.of(2026, 9, 10, 21, 30), 60));
        assertThat(RecurrenceExpander.expand(start, end, pattern)).hasSize(4);
    }

    @Test
    void monthlyClampsAgainstTheLocalMonthNotTheUtcMonth() {
        // 2026-01-01T01:00+03:00, whose UTC instant still sits in December
        LocalDateTime start = LocalDateTime.of(2025, 12, 31, 22, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 0);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.MONTHLY, 1, null, 31, null, 3);

        assertThat(RecurrenceExpander.expand(start, end, pattern, NAIROBI)).containsExactly(
                window(LocalDateTime.of(2026, 1, 30, 22, 0), 60),
                window(LocalDateTime.of(2026, 2, 27, 22, 0), 60),
                window(LocalDateTime.of(2026, 3, 30, 22, 0), 60));
        assertThat(RecurrenceExpander.expand(start, end, pattern).getFirst().start())
                .isEqualTo(LocalDateTime.of(2025, 12, 31, 22, 0));
    }

    @Test
    void weeklySeriesKeepsLocalWallClockAcrossDaylightSavingStart() {
        // 2026-03-04T20:30-05:00, three Wednesdays spanning the 8 March switch to EDT
        LocalDateTime start = LocalDateTime.of(2026, 3, 5, 1, 30);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 2, 30);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "WEDNESDAY", null, null, 3);

        List<OccurrenceWindow> windows = RecurrenceExpander.expand(start, end, pattern, NEW_YORK);

        assertThat(windows).containsExactly(
                window(LocalDateTime.of(2026, 3, 5, 1, 30)),
                window(LocalDateTime.of(2026, 3, 12, 0, 30)),
                window(LocalDateTime.of(2026, 3, 19, 0, 30)));
        assertThat(windows).allSatisfy(window -> {
            assertThat(localStart(window, NEW_YORK).getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
            assertThat(localStart(window, NEW_YORK).toLocalTime()).isEqualTo(LocalTime.of(20, 30));
        });
    }

    @Test
    void dailySeriesKeepsLocalStartTimeAcrossBritishSummerTimeStart() {
        // 2026-03-28T00:30Z, three mornings spanning the 29 March switch to BST
        LocalDateTime start = LocalDateTime.of(2026, 3, 28, 0, 30);
        LocalDateTime end = LocalDateTime.of(2026, 3, 28, 1, 30);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 1, null, null, null, 3);

        List<OccurrenceWindow> windows = RecurrenceExpander.expand(start, end, pattern, LONDON);

        assertThat(windows.stream().map(OccurrenceWindow::start)).containsExactly(
                LocalDateTime.of(2026, 3, 28, 0, 30),
                LocalDateTime.of(2026, 3, 29, 0, 30),
                LocalDateTime.of(2026, 3, 29, 23, 30));
        assertThat(windows).allSatisfy(window ->
                assertThat(localStart(window, LONDON).toLocalTime()).isEqualTo(LocalTime.of(0, 30)));
    }

    @Test
    void patternlessTemplateIsUnaffectedByTheZone() {
        assertThat(RecurrenceExpander.expand(STAGING_START, STAGING_END, null, NAIROBI))
                .containsExactly(new OccurrenceWindow(STAGING_START, STAGING_END));
    }

    private static LocalDateTime localStart(OccurrenceWindow window, String zone) {
        return window.start().atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of(zone)).toLocalDateTime();
    }

    private static OccurrenceWindow window(LocalDateTime start) {
        return window(start, 60);
    }

    private static OccurrenceWindow window(LocalDateTime start, int durationMinutes) {
        return new OccurrenceWindow(start, start.plusMinutes(durationMinutes));
    }
}
