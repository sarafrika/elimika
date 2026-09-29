package apps.sarafrika.elimika.shared.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

/**
 * Guards endpoints that bind a raw {@link Pageable} against sorting by columns the caller cannot
 * see. Ranking a page by a hidden column — a price, a pay rate, a payment reference — leaks its
 * order even when the column itself is never returned, so each endpoint names the properties it
 * may be sorted by and anything else is rejected as a 400.
 * <p>
 * Matching ignores case and underscores, as {@link GenericSpecificationBuilder} does, because
 * Spring Data resolves {@code created_date} to the field {@code createdDate}.
 */
public final class SortAllowList {

    private static final int MAX_ECHOED_PROPERTY_LENGTH = 64;

    private SortAllowList() {
    }

    /**
     * @param pageable the bound pageable; unpaged or unsorted requests always pass
     * @param allowed  the entity properties this endpoint may be sorted by
     * @throws IllegalArgumentException when a requested sort property is not in {@code allowed}
     */
    public static void validate(Pageable pageable, Set<String> allowed) {
        if (pageable == null || pageable.isUnpaged()) {
            return;
        }
        validate(pageable.getSort(), allowed);
    }

    /**
     * @throws IllegalArgumentException when a requested sort property is not in {@code allowed}
     */
    public static void validate(Sort sort, Set<String> allowed) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            String requested = normalise(order.getProperty());
            boolean permitted = allowed.stream().anyMatch(property -> normalise(property).equals(requested));
            if (!permitted) {
                throw new IllegalArgumentException("Unsupported sort property: " + sanitise(order.getProperty()));
            }
        }
    }

    private static String normalise(String property) {
        return property.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static String sanitise(String property) {
        String cleaned = property.replaceAll("[^A-Za-z0-9_.]", "");
        return cleaned.length() > MAX_ECHOED_PROPERTY_LENGTH
                ? cleaned.substring(0, MAX_ECHOED_PROPERTY_LENGTH)
                : cleaned;
    }
}
