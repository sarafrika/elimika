package apps.sarafrika.elimika.shared.utils.recurrence;

import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Expands a session template into its concrete occurrence windows.
 * <p>
 * This is the single source of truth for recurrence expansion: marketplace job
 * resource holds, instructor application eligibility checks and class definition
 * scheduling must all enumerate the exact same windows so that recruitment holds
 * line up one-to-one with the scheduled instances created at assignment. The
 * cursor logic is a faithful port of the historic expansion in
 * {@code ClassDefinitionServiceImpl}, including the weekly week-anchor interval
 * math, monthly day-of-month clamping and the iteration safety cap.
 */
@Slf4j
public final class RecurrenceExpander {

    public static final int MAX_EXPANSION_ITERATIONS = 2000;

    private RecurrenceExpander() {
    }

    public static List<OccurrenceWindow> expand(LocalDateTime templateStart,
                                                LocalDateTime templateEnd,
                                                RecurrencePattern pattern) {
        if (templateStart == null || templateEnd == null) {
            throw new IllegalArgumentException("Session templates require both start_time and end_time");
        }
        if (!templateStart.isBefore(templateEnd)) {
            throw new IllegalArgumentException("Session template start_time must be before end_time");
        }

        if (pattern == null || pattern.frequency() == null) {
            return List.of(new OccurrenceWindow(templateStart, templateEnd));
        }

        return switch (pattern.frequency()) {
            case DAILY -> expandDaily(templateStart, templateEnd, pattern);
            case WEEKLY -> expandWeekly(templateStart, templateEnd, pattern);
            case MONTHLY -> expandMonthly(templateStart, templateEnd, pattern);
        };
    }

    private static List<OccurrenceWindow> expandDaily(LocalDateTime templateStart,
                                                      LocalDateTime templateEnd,
                                                      RecurrencePattern pattern) {
        int interval = resolveInterval(pattern);
        int targetOccurrences = resolveTargetOccurrences(pattern);
        LocalDate endDateLimit = pattern.endDate();

        List<OccurrenceWindow> windows = new ArrayList<>();
        LocalDateTime cursorStart = templateStart;
        LocalDateTime cursorEnd = templateEnd;
        int iterations = 0;

        while (shouldContinue(windows.size(), targetOccurrences, cursorStart.toLocalDate(), endDateLimit)
                && iterations < MAX_EXPANSION_ITERATIONS) {
            iterations++;
            windows.add(new OccurrenceWindow(cursorStart, cursorEnd));
            cursorStart = cursorStart.plusDays(interval);
            cursorEnd = cursorEnd.plusDays(interval);
        }
        return windows;
    }

    private static List<OccurrenceWindow> expandWeekly(LocalDateTime templateStart,
                                                       LocalDateTime templateEnd,
                                                       RecurrencePattern pattern) {
        int interval = resolveInterval(pattern);
        int targetOccurrences = resolveTargetOccurrences(pattern);
        LocalDate endDateLimit = pattern.endDate();

        Set<DayOfWeek> allowedDays = parseDaysOfWeek(pattern.daysOfWeek());
        if (allowedDays.isEmpty()) {
            allowedDays = Set.of(templateStart.getDayOfWeek());
        }

        List<OccurrenceWindow> windows = new ArrayList<>();
        LocalDate cursorDate = templateStart.toLocalDate();
        LocalDate weekAnchor = cursorDate.minusDays(cursorDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        int iterations = 0;

        while (shouldContinue(windows.size(), targetOccurrences, cursorDate, endDateLimit)
                && iterations < MAX_EXPANSION_ITERATIONS) {
            iterations++;
            LocalDate currentWeekStart = cursorDate.minusDays(cursorDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
            long weeksBetween = ChronoUnit.WEEKS.between(weekAnchor, currentWeekStart);

            if (weeksBetween % interval == 0 && allowedDays.contains(cursorDate.getDayOfWeek())) {
                windows.add(new OccurrenceWindow(
                        cursorDate.atTime(templateStart.toLocalTime()),
                        cursorDate.atTime(templateEnd.toLocalTime())));
            }
            cursorDate = cursorDate.plusDays(1);
        }
        return windows;
    }

    private static List<OccurrenceWindow> expandMonthly(LocalDateTime templateStart,
                                                        LocalDateTime templateEnd,
                                                        RecurrencePattern pattern) {
        int interval = resolveInterval(pattern);
        int targetOccurrences = resolveTargetOccurrences(pattern);
        LocalDate endDateLimit = pattern.endDate();

        List<OccurrenceWindow> windows = new ArrayList<>();
        LocalDate cursorDate = templateStart.toLocalDate();
        int iterations = 0;

        while (shouldContinue(windows.size(), targetOccurrences, cursorDate, endDateLimit)
                && iterations < MAX_EXPANSION_ITERATIONS) {
            iterations++;
            LocalDate targetDate = resolveMonthlyDate(cursorDate, pattern.dayOfMonth());
            windows.add(new OccurrenceWindow(
                    LocalDateTime.of(targetDate, templateStart.toLocalTime()),
                    LocalDateTime.of(targetDate, templateEnd.toLocalTime())));
            cursorDate = cursorDate.plusMonths(interval);
        }
        return windows;
    }

    private static boolean shouldContinue(int emittedCount,
                                          int targetOccurrences,
                                          LocalDate currentDate,
                                          LocalDate endDateLimit) {
        if (targetOccurrences > 0 && emittedCount < targetOccurrences) {
            return true;
        }
        if (targetOccurrences <= 0 && endDateLimit != null) {
            return !currentDate.isAfter(endDateLimit);
        }
        return targetOccurrences <= 0 && endDateLimit == null && emittedCount == 0;
    }

    private static LocalDate resolveMonthlyDate(LocalDate baseDate, Integer dayOfMonth) {
        if (dayOfMonth == null) {
            return baseDate;
        }
        int lastDay = baseDate.lengthOfMonth();
        int safeDay = Math.min(dayOfMonth, lastDay);
        return baseDate.withDayOfMonth(safeDay);
    }

    private static Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return Set.of();
        }
        String[] parts = daysOfWeek.split(",");
        Set<DayOfWeek> results = new LinkedHashSet<>();
        for (String part : parts) {
            try {
                results.add(DayOfWeek.valueOf(part.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                log.warn("Ignoring invalid day_of_week entry: {}", part);
            }
        }
        return results;
    }

    private static int resolveInterval(RecurrencePattern pattern) {
        Integer interval = pattern.intervalValue();
        return interval == null || interval < 1 ? 1 : interval;
    }

    private static int resolveTargetOccurrences(RecurrencePattern pattern) {
        return pattern.occurrenceCount() != null ? pattern.occurrenceCount() : 0;
    }
}
