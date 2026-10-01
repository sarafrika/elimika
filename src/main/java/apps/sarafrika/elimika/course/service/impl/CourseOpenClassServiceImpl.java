package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseOpenClasses;
import apps.sarafrika.elimika.course.dto.OpenClassAvailability;
import apps.sarafrika.elimika.course.dto.OpenClassSummary;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseOpenClassService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.OpenClassListing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists the classes of a public course that a visitor can still join.
 * <p>
 * The course is checked with the same rule the anonymous course read uses
 * ({@link CourseServiceImpl#isPublicCourse}), for every caller: this page is the public face of a
 * course, so a signed-in author previewing their draft gets the same 404 a stranger does. The classes
 * themselves come from the classes module through {@link ClassDefinitionLookupService}, already reduced
 * to fields that are safe to publish. Seat counts are turned into an availability band here and go
 * no further.
 */
@Service
@Transactional(readOnly = true)
public class CourseOpenClassServiceImpl implements CourseOpenClassService {

    private static final Comparator<OpenClassSummary> CHEAPEST_THEN_SOONEST = Comparator
            .comparing((OpenClassSummary summary) -> summary.availability() == OpenClassAvailability.FULL)
            .thenComparing(OpenClassSummary::fee, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OpenClassSummary::startsOn, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OpenClassSummary::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    /** English country names, lower-cased, for dropping a trailing country from a location label. */
    private static final Set<String> COUNTRY_NAMES = Arrays.stream(Locale.getISOCountries())
            .map(code -> Locale.of("", code).getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private final CourseRepository courseRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final String currencyCode;

    public CourseOpenClassServiceImpl(CourseRepository courseRepository,
                                      ClassDefinitionLookupService classDefinitionLookupService,
                                      @Value("${commerce.internal.default-currency:KES}") String currencyCode) {
        this.courseRepository = courseRepository;
        this.classDefinitionLookupService = classDefinitionLookupService;
        this.currencyCode = StringUtils.hasText(currencyCode) ? currencyCode.trim().toUpperCase(Locale.ROOT) : null;
    }

    @Override
    public CourseOpenClasses getOpenClasses(UUID courseUuid) {
        courseRepository.findByUuid(courseUuid)
                .filter(CourseServiceImpl::isPublicCourse)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with UUID: " + courseUuid));

        List<OpenClassSummary> classes = new ArrayList<>();
        for (OpenClassListing listing : classDefinitionLookupService.findOpenClassesForCourse(courseUuid)) {
            classes.add(toSummary(listing));
        }
        classes.sort(CHEAPEST_THEN_SOONEST);

        // A full class stays listed (it still tells the visitor the course runs) but cannot be joined,
        // so it neither counts nor sets the "from" price.
        List<OpenClassSummary> joinable = classes.stream()
                .filter(summary -> summary.availability() != OpenClassAvailability.FULL)
                .toList();
        BigDecimal priceFrom = joinable.stream()
                .map(OpenClassSummary::fee)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new CourseOpenClasses(priceFrom, classes.isEmpty() ? null : currencyCode, joinable.size(), classes);
    }

    private OpenClassSummary toSummary(OpenClassListing listing) {
        String[] place = splitLocation(listing.locationName());
        return new OpenClassSummary(
                listing.uuid(),
                listing.title(),
                listing.locationType(),
                listing.sessionFormat(),
                place[0],
                place[1],
                listing.salePrice(),
                currencyCode,
                OpenClassAvailability.of(listing.maxParticipants(), listing.seatsLeft()),
                listing.startsOn(),
                listing.endsOn(),
                listing.registrationClosesOn(),
                listing.branchName());
    }

    /**
     * Splits a location label into {@code [place_name, area]}: the first comma-separated part, and the
     * remaining parts joined back together with a trailing country dropped. Either is null when empty.
     */
    static String[] splitLocation(String locationName) {
        if (!StringUtils.hasText(locationName)) {
            return new String[]{null, null};
        }
        List<String> parts = Arrays.stream(locationName.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
        if (parts.isEmpty()) {
            return new String[]{null, null};
        }
        String placeName = parts.removeFirst();
        if (!parts.isEmpty() && COUNTRY_NAMES.contains(parts.getLast().toLowerCase(Locale.ROOT))) {
            parts.removeLast();
        }
        return new String[]{placeName, parts.isEmpty() ? null : String.join(", ", parts)};
    }
}
