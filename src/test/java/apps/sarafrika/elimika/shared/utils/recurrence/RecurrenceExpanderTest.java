package apps.sarafrika.elimika.shared.utils.recurrence;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceExpanderTest {

    // 2026-01-05 is a Monday
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 5, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 5, 12, 0);

    @Test
    void nullPatternYieldsSingleOccurrence() {
        List<OccurrenceWindow> windows = RecurrenceExpander.expand(START, END, null);

        assertThat(windows).containsExactly(new OccurrenceWindow(START, END));
    }

    @Test
    void nullFrequencyYieldsSingleOccurrence() {
        RecurrencePattern pattern = new RecurrencePattern(null, 2, null, null, null, 5);

        assertThat(RecurrenceExpander.expand(START, END, pattern))
                .containsExactly(new OccurrenceWindow(START, END));
    }

    @Test
    void dailyWithoutBoundsYieldsSingleOccurrence() {
        RecurrencePattern pattern = new RecurrencePattern(RecurrenceFrequency.DAILY, 1, null, null, null, null);

        assertThat(RecurrenceExpander.expand(START, END, pattern))
                .containsExactly(new OccurrenceWindow(START, END));
    }

    @Test
    void dailyOccurrenceCount() {
        RecurrencePattern pattern = new RecurrencePattern(RecurrenceFrequency.DAILY, 1, null, null, null, 3);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 6),
                window(2026, 1, 7));
    }

    @Test
    void dailyIntervalWithEndDateInclusive() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 2, null, null, LocalDate.of(2026, 1, 11), null);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 7),
                window(2026, 1, 9),
                window(2026, 1, 11));
    }

    @Test
    void dailyEndDateExcludesWindowsBeyondLimit() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 2, null, null, LocalDate.of(2026, 1, 10), null);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 7),
                window(2026, 1, 9));
    }

    @Test
    void occurrenceCountWinsOverEndDate() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 1, null, null, LocalDate.of(2026, 1, 6), 5);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).hasSize(5);
    }

    @Test
    void weeklyMultipleDays() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "MONDAY,WEDNESDAY", null, null, 4);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 7),
                window(2026, 1, 12),
                window(2026, 1, 14));
    }

    @Test
    void weeklyDefaultsToStartDayWhenNoDaysGiven() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, null, null, null, 3);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 12),
                window(2026, 1, 19));
    }

    @Test
    void biweeklyIntervalSkipsAlternateWeeks() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 2, "MONDAY", null, null, 3);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 19),
                window(2026, 2, 2));
    }

    @Test
    void weeklyStartDayNotInAllowedDaysBeginsAtNextAllowedDay() {
        LocalDateTime tuesdayStart = LocalDateTime.of(2026, 1, 6, 10, 0);
        LocalDateTime tuesdayEnd = LocalDateTime.of(2026, 1, 6, 12, 0);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "FRIDAY", null, null, 2);

        assertThat(RecurrenceExpander.expand(tuesdayStart, tuesdayEnd, pattern)).containsExactly(
                window(2026, 1, 9),
                window(2026, 1, 16));
    }

    @Test
    void weeklyIgnoresInvalidDayTokens() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.WEEKLY, 1, "MONDAY,NOTADAY", null, null, 2);

        assertThat(RecurrenceExpander.expand(START, END, pattern)).containsExactly(
                window(2026, 1, 5),
                window(2026, 1, 12));
    }

    @Test
    void monthlyClampsDayOfMonthToMonthLength() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 15, 12, 0);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.MONTHLY, 1, null, 31, null, 3);

        assertThat(RecurrenceExpander.expand(start, end, pattern)).containsExactly(
                window(2026, 1, 31),
                window(2026, 2, 28),
                window(2026, 3, 31));
    }

    @Test
    void monthlyWithoutDayOfMonthKeepsCursorDate() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 15, 12, 0);
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.MONTHLY, 2, null, null, null, 3);

        assertThat(RecurrenceExpander.expand(start, end, pattern)).containsExactly(
                window(2026, 1, 15),
                window(2026, 3, 15),
                window(2026, 5, 15));
    }

    @Test
    void expansionIsCappedAtMaxIterations() {
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrenceFrequency.DAILY, 1, null, null, null, 5000);

        assertThat(RecurrenceExpander.expand(START, END, pattern))
                .hasSize(RecurrenceExpander.MAX_EXPANSION_ITERATIONS);
    }

    @Test
    void rejectsMissingOrInvertedWindow() {
        assertThatThrownBy(() -> RecurrenceExpander.expand(null, END, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecurrenceExpander.expand(END, START, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OccurrenceWindow window(int year, int month, int day) {
        return new OccurrenceWindow(
                LocalDateTime.of(year, month, day, 10, 0),
                LocalDateTime.of(year, month, day, 12, 0));
    }
}
