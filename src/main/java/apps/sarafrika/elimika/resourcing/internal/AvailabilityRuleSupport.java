package apps.sarafrika.elimika.resourcing.internal;

import apps.sarafrika.elimika.resourcing.model.ResourceAvailabilityRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared date-applicability logic for resource availability rules.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AvailabilityRuleSupport {

    /**
     * Whether a recurring rule applies on the given date: within its effective date
     * bounds and, when days_of_week is set, on one of the listed days.
     */
    public static boolean ruleAppliesOn(ResourceAvailabilityRule rule, LocalDate date) {
        if (rule.getEffectiveStartDate() != null && date.isBefore(rule.getEffectiveStartDate())) {
            return false;
        }
        if (rule.getEffectiveEndDate() != null && date.isAfter(rule.getEffectiveEndDate())) {
            return false;
        }
        Set<DayOfWeek> days = parseDaysOfWeek(rule.getDaysOfWeek());
        return days.isEmpty() || days.contains(date.getDayOfWeek());
    }

    public static Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return Set.of();
        }
        Set<DayOfWeek> results = new HashSet<>();
        for (String part : daysOfWeek.split(",")) {
            try {
                results.add(DayOfWeek.valueOf(part.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                log.warn("Ignoring invalid day_of_week entry on resource availability rule: {}", part);
            }
        }
        return results;
    }
}
