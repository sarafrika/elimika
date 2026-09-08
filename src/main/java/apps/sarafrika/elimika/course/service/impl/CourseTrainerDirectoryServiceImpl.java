package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainerDirectoryDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainerSummaryDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.projection.CourseTrainerRateView;
import apps.sarafrika.elimika.course.repository.projection.CourseTrainerView;
import apps.sarafrika.elimika.course.service.CourseTrainerDirectoryService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assembles a course's approved delivery list.
 * <p>
 * Two rules shape this class.
 * <p>
 * <strong>Rates are projected, not filtered.</strong> Whether the caller may see what trainers
 * charge is decided before anything is read, and decides <em>which query runs</em>. The unprivileged
 * path executes a select list naming three columns, none of them a rate, so no figure ever reaches
 * the JVM — as opposed to loading applications and nulling the rate card on the way out, which
 * leaves every number one careless mapping away from the wire.
 * <p>
 * <strong>Sorting is an allow-list, not a pass-through.</strong> Ordering is a read primitive: a
 * caller who may order by {@code privateOnlineHourlyRate} can binary-search a trainer's price out of
 * an endpoint that never prints it. Only the three fields this directory itself publishes may be
 * sorted on, and anything else is rejected outright rather than quietly dropped, so a client using
 * a field that does not exist finds out.
 * <p>
 * Which of the two paths runs is decided by ownership or platform administration <em>capped by the
 * dashboard the request came from</em>, through {@link CourseFootingCap}. Like the statistics
 * endpoint, this reads no {@code CourseContentAccess} of its own, so it was a third place the same
 * defect surfaced: a platform administrator on their own learner dashboard ran the privileged query
 * and was told what every approved trainer charges, along with how many applications are still
 * pending. Because the cap is applied before the query is chosen, a capped caller's rates are not
 * filtered out on the way to the wire — they are never read.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseTrainerDirectoryServiceImpl implements CourseTrainerDirectoryService {

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with UUID %s not found";

    /** Sortable fields, keyed by their JSON name with separators removed, as Spring Data matches them. */
    private static final String SORT_DISPLAY_NAME = "displayname";
    private static final String SORT_APPROVED_AT = "approvedat";
    private static final String SORT_ACTIVE_CLASS_COUNT = "activeclasscount";
    private static final String SORTABLE_FIELDS = "display_name, approved_at, active_class_count";

    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository applicationRepository;
    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final CourseFootingCap courseFootingCap;
    private final OrganisationLookupService organisationLookupService;
    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @Override
    public CourseTrainerDirectoryDTO getTrainerDirectory(UUID courseUuid, Pageable pageable) {
        if (!courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid));
        }

        // Validated before a row is read, so a rejected sort tells the caller nothing about the data.
        Comparator<CourseTrainerSummaryDTO> ordering = orderingFor(pageable);

        boolean mayReadCommercialTerms = courseFootingCap.permitsCommercials(
                courseSecurityService.isCourseOwner(courseUuid),
                domainSecurityService.isPlatformAdmin());

        List<CourseTrainerSummaryDTO> trainers = mayReadCommercialTerms
                ? withRates(courseUuid)
                : withoutRates(courseUuid);

        if (ordering != null) {
            trainers = trainers.stream().sorted(ordering).toList();
        }

        return new CourseTrainerDirectoryDTO(
                slice(trainers, pageable),
                mayReadCommercialTerms
                        ? applicationRepository.countByCourseUuidAndStatus(
                                courseUuid, CourseTrainingApplicationStatus.PENDING)
                        : null);
    }

    /**
     * The directory as everyone but the course creator sees it. The query behind
     * {@link CourseTrainerView} names no rate column, so there is nothing here to omit.
     */
    private List<CourseTrainerSummaryDTO> withoutRates(UUID courseUuid) {
        List<CourseTrainerView> views = applicationRepository.findTrainerDirectory(
                courseUuid, CourseTrainingApplicationStatus.APPROVED);
        Identities identities = resolveIdentities(courseUuid, views);
        return views.stream().map(view -> toSummary(view, identities, null)).toList();
    }

    /** The directory the course creator and platform admins see, with the terms each trainer offered. */
    private List<CourseTrainerSummaryDTO> withRates(UUID courseUuid) {
        List<CourseTrainerRateView> views = applicationRepository.findTrainerDirectoryWithRates(
                courseUuid, CourseTrainingApplicationStatus.APPROVED);
        List<CourseTrainerView> identityViews = views.stream().map(CourseTrainerRateView::identity).toList();
        Identities identities = resolveIdentities(courseUuid, identityViews);

        List<CourseTrainerSummaryDTO> trainers = new ArrayList<>(views.size());
        for (int index = 0; index < views.size(); index++) {
            trainers.add(toSummary(identityViews.get(index), identities, toRateCard(views.get(index))));
        }
        return List.copyOf(trainers);
    }

    /**
     * Names, places and class counts for a whole page of trainers, in four queries rather than four
     * per row: two lookups per applicant kind, each asked for the entire set at once.
     */
    private Identities resolveIdentities(UUID courseUuid, List<CourseTrainerView> views) {
        Set<UUID> instructorUuids = uuidsOfType(views, CourseTrainingApplicantType.INSTRUCTOR);
        Set<UUID> organisationUuids = uuidsOfType(views, CourseTrainingApplicantType.ORGANISATION);

        return new Identities(
                instructorLookupService.findInstructorDirectoryEntries(instructorUuids),
                organisationLookupService.findOrganisationNames(organisationUuids),
                organisationLookupService.findOrganisationTowns(organisationUuids),
                classDefinitionLookupService.countActiveCourseClassesByInstructor(courseUuid, instructorUuids),
                classDefinitionLookupService.countActiveCourseClassesByOrganisation(courseUuid, organisationUuids));
    }

    private static Set<UUID> uuidsOfType(List<CourseTrainerView> views, CourseTrainingApplicantType type) {
        return views.stream()
                .filter(view -> type == view.applicantType())
                .map(CourseTrainerView::applicantUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static CourseTrainerSummaryDTO toSummary(CourseTrainerView view,
                                                     Identities identities,
                                                     CourseTrainingRateCardDTO rateCard) {
        UUID applicantUuid = view.applicantUuid();
        boolean isInstructor = CourseTrainingApplicantType.INSTRUCTOR == view.applicantType();

        InstructorDirectoryEntry instructor = isInstructor ? identities.instructors().get(applicantUuid) : null;
        String displayName = isInstructor
                ? nameOrFallback(instructor == null ? null : instructor.displayName(), "Instructor")
                : nameOrFallback(identities.organisationNames().get(applicantUuid), "Organisation");
        String location = isInstructor
                ? instructorLocation(instructor)
                : blankToNull(identities.organisationTowns().get(applicantUuid));
        long activeClassCount = isInstructor
                ? identities.instructorClassCounts().getOrDefault(applicantUuid, 0L)
                : identities.organisationClassCounts().getOrDefault(applicantUuid, 0L);

        return new CourseTrainerSummaryDTO(
                view.applicantType(),
                applicantUuid,
                displayName,
                location,
                view.approvedAt(),
                activeClassCount,
                rateCard);
    }

    /**
     * The place an instructor works, qualified by their verification state when the platform has
     * vetted them — "Kisumu · verified instructor". Never coordinates: an instructor who has named
     * no town has no location here, even though the profile could format their latitude and
     * longitude into one.
     */
    private static String instructorLocation(InstructorDirectoryEntry instructor) {
        if (instructor == null) {
            return null;
        }
        String town = blankToNull(instructor.locationName());
        if (!instructor.adminVerified()) {
            return town;
        }
        return town == null ? "Verified instructor" : town + " · verified instructor";
    }

    private static String nameOrFallback(String name, String fallback) {
        String trimmed = blankToNull(name);
        return trimmed == null ? fallback : trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CourseTrainingRateCardDTO toRateCard(CourseTrainerRateView view) {
        return new CourseTrainingRateCardDTO(
                view.rateCurrency(),
                view.privateOnlineHourlyRate(),
                view.privateInpersonHourlyRate(),
                view.groupOnlineHourlyRate(),
                view.groupInpersonHourlyRate(),
                view.privateOnlineSessionRate(),
                view.privateInpersonSessionRate(),
                view.groupOnlineSessionRate(),
                view.groupInpersonSessionRate(),
                view.privateOnlineDailyRate(),
                view.privateInpersonDailyRate(),
                view.groupOnlineDailyRate(),
                view.groupInpersonDailyRate());
    }

    /**
     * The comparator the requested sort asks for, or null when nothing was requested.
     * <p>
     * Built from the three fields the directory publishes and no others. Sorting happens here rather
     * than in SQL because two of the three — the display name and the class count — are assembled
     * from other modules and have no column on {@code course_training_applications} to order by; the
     * approved set for one course is small enough that ordering it in memory costs nothing.
     *
     * @throws IllegalArgumentException when the sort names anything else, including a rate column
     */
    private static Comparator<CourseTrainerSummaryDTO> orderingFor(Pageable pageable) {
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            return null;
        }

        Comparator<CourseTrainerSummaryDTO> ordering = null;
        for (Sort.Order order : pageable.getSort()) {
            Comparator<CourseTrainerSummaryDTO> next = comparatorFor(order.getProperty());
            if (order.isDescending()) {
                next = next.reversed();
            }
            ordering = ordering == null ? next : ordering.thenComparing(next);
        }
        return ordering;
    }

    private static Comparator<CourseTrainerSummaryDTO> comparatorFor(String property) {
        return switch (normalise(property)) {
            case SORT_DISPLAY_NAME -> Comparator.comparing(
                    CourseTrainerSummaryDTO::displayName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case SORT_APPROVED_AT -> Comparator.comparing(
                    CourseTrainerSummaryDTO::approvedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case SORT_ACTIVE_CLASS_COUNT -> Comparator.comparingLong(CourseTrainerSummaryDTO::activeClassCount);
            default -> throw new IllegalArgumentException(
                    "Unsupported sort property: " + sanitise(property) + ". Sortable properties: " + SORTABLE_FIELDS);
        };
    }

    /** Spring Data treats {@code approved_at} and {@code approvedAt} as the same property; so do we. */
    private static String normalise(String property) {
        return property == null ? "" : property.toLowerCase(Locale.ROOT).replace("_", "");
    }

    /** Echoes back only what a sort property may legitimately contain, so the message cannot carry a payload. */
    private static String sanitise(String property) {
        if (property == null) {
            return "";
        }
        String trimmed = property.length() > 64 ? property.substring(0, 64) : property;
        return trimmed.replaceAll("[^A-Za-z0-9_.-]", "");
    }

    /**
     * The requested window of the directory. Ordering is applied first, so the window is taken from
     * the list the caller asked to see rather than from the order the database happened to return.
     */
    private static List<CourseTrainerSummaryDTO> slice(List<CourseTrainerSummaryDTO> trainers, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return trainers;
        }
        long offset = pageable.getOffset();
        if (offset >= trainers.size()) {
            return List.of();
        }
        int from = (int) offset;
        int to = Math.min(from + pageable.getPageSize(), trainers.size());
        return new ArrayList<>(trainers.subList(from, to));
    }

    /**
     * The cross-module facts one page of trainers needs, each fetched for the whole page at once.
     *
     * @param instructors            directory identity keyed by instructor UUID
     * @param organisationNames      organisation display names keyed by organisation UUID
     * @param organisationTowns      organisation towns keyed by organisation UUID
     * @param instructorClassCounts  active classes on this course keyed by instructor UUID
     * @param organisationClassCounts active classes on this course keyed by organisation UUID
     */
    private record Identities(Map<UUID, InstructorDirectoryEntry> instructors,
                              Map<UUID, String> organisationNames,
                              Map<UUID, String> organisationTowns,
                              Map<UUID, Long> instructorClassCounts,
                              Map<UUID, Long> organisationClassCounts) {
    }
}
