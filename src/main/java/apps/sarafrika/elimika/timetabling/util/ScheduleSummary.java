package apps.sarafrika.elimika.timetabling.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Renders session templates packed as "type|days|yyyy-MM-dd HH:mm|HH:mm", joined by ';', as a short
// line such as "Mon & Wed · 9:00–11:00".
public final class ScheduleSummary {

    private static final DateTimeFormatter PACKED_START = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter PACKED_END = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter ONE_OFF_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private ScheduleSummary() {
    }

    public static String fromPackedTemplates(String packed) {
        if (packed == null || packed.isBlank()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (String template : packed.split(";")) {
            String line = describe(template);
            if (line != null && !lines.contains(line)) {
                lines.add(line);
            }
        }
        return lines.isEmpty() ? null : String.join("; ", lines);
    }

    private static String describe(String template) {
        String[] fields = template.split("\\|", -1);
        if (fields.length != 4) {
            return null;
        }
        try {
            LocalDateTime start = LocalDateTime.parse(fields[2], PACKED_START);
            LocalTime end = LocalTime.parse(fields[3], PACKED_END);
            return days(fields[0], fields[1], start) + " · " + CLOCK.format(start) + "–" + CLOCK.format(end);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String days(String recurrenceType, String daysOfWeek, LocalDateTime start) {
        return switch (recurrenceType.trim().toUpperCase(Locale.ROOT)) {
            case "DAILY" -> "Daily";
            case "MONTHLY" -> "Monthly";
            case "WEEKLY" -> weekdays(daysOfWeek, start.getDayOfWeek());
            default -> ONE_OFF_DATE.format(start);
        };
    }

    /** A weekly template without named days repeats on the weekday it starts on, as the expander does. */
    private static String weekdays(String daysOfWeek, DayOfWeek startDay) {
        List<DayOfWeek> days = Arrays.stream(daysOfWeek.split(","))
                .map(ScheduleSummary::dayOfWeek)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<String> names = (days.isEmpty() ? List.of(startDay) : days).stream()
                .map(day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .toList();
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " & " + names.getLast();
    }

    private static DayOfWeek dayOfWeek(String name) {
        try {
            return DayOfWeek.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
