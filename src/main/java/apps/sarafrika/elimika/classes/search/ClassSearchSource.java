package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Feeds the {@code classes} index from {@code class_definitions}.
 * <p>
 * Every class is indexed, active or not and in either visibility: the listing scope
 * ({@link ClassSearchScopes}) decides who sees what, exactly as the SQL listing does.
 * <p>
 * Kept current by entity triggers on {@link ClassDefinition} and on {@link ClassSessionTemplate}
 * (whose start times feed {@code starts_at}). The {@code ClassDefined}, {@code ClassDefinitionUpdated}
 * and {@code ClassDefinitionDeactivated} events are all raised next to a {@code ClassDefinition}
 * save, so the entity trigger already covers them.
 * <p>
 * Names owned by other modules (course, program, organisation, branch, instructor) and the linked
 * content's approval are copied in when a class is indexed; a rename or an approval change in the
 * owning module does not re-index the classes that show it (except an instructor's user rename, see
 * {@code ClassInstructorNameChangeIndexer}). The nightly full rebuild
 * ({@code search.full-rebuild-cron}) or a manual one refreshes them, and module reads re-check
 * visibility and approval against the database, so a stale copy can mis-rank but never leak.
 */
@Component
@RequiredArgsConstructor
public class ClassSearchSource implements SearchDocumentSource<ClassSearchDocument> {

    public static final String INDEX = "classes";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 1,
            List.of("title", "course_name", "program_title", "organisation_name", "branch_name",
                    "instructor_name", "location_name", "description"),
            List.of("uuid", "course_uuid", "program_uuid", "organisation_uuid", "branch_uuid",
                    "default_instructor_uuid", "category_uuid", "is_active", "class_visibility",
                    "content_approved", "location_type", "session_format", "starts_at",
                    "registration_closes_at", "sale_price", "created_at"),
            List.of("starts_at", "sale_price", "created_at", "title"));

    /** The listing's sortable entity properties that have an index counterpart. */
    private static final Map<String, String> SORT_ATTRIBUTES = Map.of(
            "title", "title",
            "createdDate", "created_at",
            "defaultStartTime", "starts_at");

    private final ClassDefinitionRepository classDefinitionRepository;
    private final ClassSessionTemplateRepository sessionTemplateRepository;
    private final CourseInfoService courseInfoService;
    private final OrganisationLookupService organisationLookupService;
    private final InstructorLookupService instructorLookupService;
    private final BranchLocationResolver branchLocationResolver;

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<ClassSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return toDocuments(classDefinitionRepository.findByUuidIn(uuids));
    }

    @Override
    public SearchBatch<ClassSearchDocument> loadAfter(long lastId, int batchSize) {
        List<ClassDefinition> rows = classDefinitionRepository.findByIdGreaterThanOrderByIdAsc(
                lastId, PageRequest.of(0, batchSize));
        if (rows.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        return new SearchBatch<>(toDocuments(rows), rows.getLast().getId());
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(ClassDefinition.class, BaseEntity::getUuid),
                SearchIndexTrigger.direct(ClassSessionTemplate.class, ClassSessionTemplate::getClassDefinitionUuid));
    }

    @Override
    public long countIndexable() {
        return classDefinitionRepository.count();
    }

    /**
     * The index ordering for a listing's {@link Sort}. Properties without an index counterpart are
     * dropped, leaving relevance; the listing's own allow-list has already refused anything else.
     */
    public static List<SearchSort> sortFor(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return List.of();
        }
        return sort.stream()
                .filter(order -> SORT_ATTRIBUTES.containsKey(order.getProperty()))
                .map(order -> new SearchSort(SORT_ATTRIBUTES.get(order.getProperty()),
                        order.isDescending() ? SearchSort.Direction.DESC : SearchSort.Direction.ASC))
                .toList();
    }

    private List<ClassSearchDocument> toDocuments(List<ClassDefinition> classes) {
        if (classes.isEmpty()) {
            return List.of();
        }
        List<UUID> classUuids = SearchValues.distinct(classes, BaseEntity::getUuid);
        List<UUID> courseUuids = SearchValues.distinct(classes, ClassDefinition::getCourseUuid);
        List<UUID> programUuids = SearchValues.distinct(classes, ClassDefinition::getProgramUuid);
        List<UUID> organisationUuids = SearchValues.distinct(classes, ClassDefinition::getOrganisationUuid);
        List<UUID> branchUuids = SearchValues.distinct(classes, ClassDefinition::getBranchUuid);
        List<UUID> instructorUuids = SearchValues.distinct(classes, ClassDefinition::getDefaultInstructorUuid);

        Map<UUID, LocalDateTime> firstSessionByClass = new HashMap<>();
        for (ClassSessionTemplate template : sessionTemplateRepository.findByClassDefinitionUuidIn(classUuids)) {
            firstSessionByClass.merge(template.getClassDefinitionUuid(), template.getStartTime(), SearchValues::earliest);
        }
        Map<UUID, String> courseNames = courseUuids.isEmpty() ? Map.of() : courseInfoService.getCourseNames(courseUuids);
        Map<UUID, String> programTitles = programUuids.isEmpty() ? Map.of()
                : courseInfoService.getTrainingProgramTitles(programUuids);
        Set<UUID> approvedCourses = courseUuids.isEmpty() ? Set.of()
                : courseInfoService.findApprovedCourseUuids(courseUuids);
        Set<UUID> approvedPrograms = programUuids.isEmpty() ? Set.of()
                : courseInfoService.findApprovedTrainingProgramUuids(programUuids);
        Map<UUID, String> organisationNames = organisationUuids.isEmpty() ? Map.of()
                : organisationLookupService.findOrganisationNames(organisationUuids);
        Map<UUID, String> branchNames = branchUuids.isEmpty() ? Map.of() : branchLocationResolver.branchNames(branchUuids);
        Map<UUID, InstructorDirectoryEntry> instructors = instructorUuids.isEmpty() ? Map.of()
                : instructorLookupService.findInstructorDirectoryEntries(instructorUuids);

        return classes.stream()
                .map(definition -> {
                    InstructorDirectoryEntry instructor = definition.getDefaultInstructorUuid() == null ? null
                            : instructors.get(definition.getDefaultInstructorUuid());
                    return new ClassSearchDocument(
                            definition.getUuid(),
                            definition.getTitle(),
                            definition.getDescription(),
                            definition.getLocationName(),
                            definition.getCourseUuid(),
                            lookup(courseNames, definition.getCourseUuid()),
                            definition.getProgramUuid(),
                            lookup(programTitles, definition.getProgramUuid()),
                            definition.getOrganisationUuid(),
                            lookup(organisationNames, definition.getOrganisationUuid()),
                            definition.getBranchUuid(),
                            lookup(branchNames, definition.getBranchUuid()),
                            definition.getDefaultInstructorUuid(),
                            instructor == null ? null : instructor.displayName(),
                            definition.getCategoryUuid(),
                            Boolean.TRUE.equals(definition.getIsActive()),
                            SearchValues.name(definition.getClassVisibility()),
                            contentApproved(definition, approvedCourses, approvedPrograms),
                            SearchValues.name(definition.getLocationType()),
                            SearchValues.name(definition.getSessionFormat()),
                            SearchValues.epochSeconds(SearchValues.earliest(
                                    firstSessionByClass.get(definition.getUuid()), definition.getDefaultStartTime())),
                            SearchValues.endOfDay(definition.getRegistrationPeriodEndDate()),
                            definition.getSalePrice(),
                            SearchValues.epochSeconds(definition.getCreatedDate()));
                })
                .toList();
    }

    /** The same rule as the listing's {@code isLinkedContentApproved}: the linked course, else program, is approved. */
    private static boolean contentApproved(ClassDefinition definition, Set<UUID> approvedCourses, Set<UUID> approvedPrograms) {
        if (definition.getCourseUuid() != null) {
            return approvedCourses.contains(definition.getCourseUuid());
        }
        if (definition.getProgramUuid() != null) {
            return approvedPrograms.contains(definition.getProgramUuid());
        }
        return true;
    }

    private static String lookup(Map<UUID, String> values, UUID key) {
        return key == null ? null : values.get(key);
    }
}
