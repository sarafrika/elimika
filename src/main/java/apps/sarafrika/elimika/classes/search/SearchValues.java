package apps.sarafrika.elimika.classes.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** Conversions shared by the classes module's search sources. Stored date-times are UTC. */
final class SearchValues {

    private SearchValues() {
    }

    static Long epochSeconds(LocalDateTime utc) {
        return utc == null ? null : utc.toEpochSecond(ZoneOffset.UTC);
    }

    /** The last second of {@code date} in UTC: a registration window closes at the end of its last day. */
    static Long endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
    }

    static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    static LocalDateTime earliest(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    static <T> List<UUID> distinct(Collection<T> rows, Function<T, UUID> field) {
        return rows.stream().map(field).filter(Objects::nonNull).distinct().toList();
    }
}
