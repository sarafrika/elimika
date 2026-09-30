package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobRequiredSkill;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceExpander;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceFrequency;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrencePattern;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Feeds the {@code marketplace_jobs} index from {@code class_marketplace_jobs}.
 * <p>
 * Every job is indexed whatever its status; {@link MarketplaceJobSearchScopes} limits non-staff
 * callers to OPEN jobs, exactly as the SQL listing does.
 * <p>
 * Kept current by entity triggers on {@link ClassMarketplaceJob} (which covers the expiry sweep,
 * which saves the expired jobs) and on {@link ClassMarketplaceJobSessionTemplate}, whose rows feed
 * {@code starts_at} and {@code session_count}. Templates are also replaced through a bulk delete,
 * which fires no trigger, so the service enqueues the job explicitly there. Job resources feed no
 * indexed attribute and need no trigger.
 * <p>
 * Organisation, branch, course and program names are copied in when a job is indexed; a rename in
 * the owning module is picked up by the next rebuild of the index.
 * <p>
 * Required skills (schema v2) are the job's own tags, or its course's skills when it has none
 * ({@link JobRequiredSkills}). Own tags trigger re-indexing directly; a change to the course's tags
 * arrives as {@code CourseSkillsChangedEvent} ({@link CourseSkillsChangeIndexer}). A renamed skill is
 * picked up by the nightly rebuild.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceJobSearchSource implements SearchDocumentSource<MarketplaceJobSearchDocument> {

    public static final String INDEX = "marketplace_jobs";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 2,
            List.of("title", "course_name", "program_title", "required_skill_names", "organisation_name", "branch_name",
                    "location_name", "target_groups", "description"),
            List.of("status", "organisation_uuid", "branch_uuid", "course_uuid", "program_uuid",
                    "category_uuid", "location_type", "session_format", "starts_at",
                    "registration_closes_at", "uuid", "created_at", "required_skill_uuids"),
            List.of("created_at", "starts_at"));

    /** The listing's sortable entity properties that have an index counterpart. */
    private static final Map<String, String> SORT_ATTRIBUTES = Map.of(
            "createdDate", "created_at",
            "defaultStartTime", "starts_at");

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobSessionTemplateRepository sessionTemplateRepository;
    private final CourseInfoService courseInfoService;
    private final OrganisationLookupService organisationLookupService;
    private final BranchLocationResolver branchLocationResolver;
    private final JobRequiredSkills jobRequiredSkills;
    private final SkillLookupService skillLookupService;

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<MarketplaceJobSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return toDocuments(jobRepository.findByUuidIn(uuids));
    }

    @Override
    public SearchBatch<MarketplaceJobSearchDocument> loadAfter(long lastId, int batchSize) {
        List<ClassMarketplaceJob> rows = jobRepository.findByIdGreaterThanOrderByIdAsc(lastId, PageRequest.of(0, batchSize));
        if (rows.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        return new SearchBatch<>(toDocuments(rows), rows.getLast().getId());
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(ClassMarketplaceJob.class, BaseEntity::getUuid),
                SearchIndexTrigger.direct(ClassMarketplaceJobSessionTemplate.class,
                        ClassMarketplaceJobSessionTemplate::getJobUuid),
                SearchIndexTrigger.direct(ClassMarketplaceJobRequiredSkill.class,
                        ClassMarketplaceJobRequiredSkill::getJobUuid));
    }

    @Override
    public long countIndexable() {
        return jobRepository.count();
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

    private List<MarketplaceJobSearchDocument> toDocuments(List<ClassMarketplaceJob> jobs) {
        if (jobs.isEmpty()) {
            return List.of();
        }
        List<UUID> jobUuids = SearchValues.distinct(jobs, BaseEntity::getUuid);
        List<UUID> courseUuids = SearchValues.distinct(jobs, ClassMarketplaceJob::getCourseUuid);
        List<UUID> programUuids = SearchValues.distinct(jobs, ClassMarketplaceJob::getProgramUuid);
        List<UUID> organisationUuids = SearchValues.distinct(jobs, ClassMarketplaceJob::getOrganisationUuid);
        List<UUID> branchUuids = SearchValues.distinct(jobs, ClassMarketplaceJob::getBranchUuid);

        Map<UUID, List<ClassMarketplaceJobSessionTemplate>> templatesByJob = new HashMap<>();
        for (ClassMarketplaceJobSessionTemplate template : sessionTemplateRepository.findByJobUuidInOrderByCreatedDateAsc(jobUuids)) {
            templatesByJob.computeIfAbsent(template.getJobUuid(), ignored -> new ArrayList<>()).add(template);
        }
        Map<UUID, String> courseNames = courseUuids.isEmpty() ? Map.of() : courseInfoService.getCourseNames(courseUuids);
        Map<UUID, String> programTitles = programUuids.isEmpty() ? Map.of()
                : courseInfoService.getTrainingProgramTitles(programUuids);
        Map<UUID, String> organisationNames = organisationUuids.isEmpty() ? Map.of()
                : organisationLookupService.findOrganisationNames(organisationUuids);
        Map<UUID, String> branchNames = branchUuids.isEmpty() ? Map.of() : branchLocationResolver.branchNames(branchUuids);
        Map<UUID, JobRequiredSkills.Effective> requiredSkills = jobRequiredSkills.forJobs(jobs);
        List<UUID> skillUuids = requiredSkills.values().stream()
                .flatMap(effective -> effective.tags().stream())
                .map(JobRequiredSkills.Tag::skillUuid)
                .distinct()
                .toList();
        Map<UUID, String> skillNames = new HashMap<>();
        if (!skillUuids.isEmpty()) {
            for (SkillSummary skill : skillLookupService.findByUuids(skillUuids)) {
                skillNames.put(skill.uuid(), skill.name());
            }
        }

        return jobs.stream()
                .map(job -> {
                    List<ClassMarketplaceJobSessionTemplate> templates = templatesByJob.getOrDefault(job.getUuid(), List.of());
                    List<UUID> jobSkills = requiredSkills.containsKey(job.getUuid())
                            ? requiredSkills.get(job.getUuid()).tags().stream()
                                    .map(JobRequiredSkills.Tag::skillUuid)
                                    .filter(skillNames::containsKey)
                                    .toList()
                            : List.of();
                    return new MarketplaceJobSearchDocument(
                            job.getUuid(),
                            job.getTitle(),
                            job.getDescription(),
                            job.getLocationName(),
                            job.getTargetGroups() == null ? List.of() : List.copyOf(job.getTargetGroups()),
                            SearchValues.name(job.getServiceType()),
                            SearchValues.name(job.getStatus()),
                            job.getOrganisationUuid(),
                            lookup(organisationNames, job.getOrganisationUuid()),
                            job.getBranchUuid(),
                            lookup(branchNames, job.getBranchUuid()),
                            job.getCourseUuid(),
                            lookup(courseNames, job.getCourseUuid()),
                            job.getProgramUuid(),
                            lookup(programTitles, job.getProgramUuid()),
                            job.getCategoryUuid(),
                            SearchValues.name(job.getLocationType()),
                            SearchValues.name(job.getSessionFormat()),
                            SearchValues.name(job.getClassVisibility()),
                            SearchValues.epochSeconds(SearchValues.earliest(firstStart(templates), job.getDefaultStartTime())),
                            SearchValues.endOfDay(job.getRegistrationPeriodEndDate()),
                            sessionCount(job.getUuid(), templates),
                            SearchValues.epochSeconds(job.getCreatedDate()),
                            jobSkills,
                            jobSkills.stream().map(skillNames::get).toList());
                })
                .toList();
    }

    private static LocalDateTime firstStart(List<ClassMarketplaceJobSessionTemplate> templates) {
        LocalDateTime first = null;
        for (ClassMarketplaceJobSessionTemplate template : templates) {
            first = SearchValues.earliest(first, template.getStartTime());
        }
        return first;
    }

    /** The sessions the job's templates expand to, the way recruitment holds expand them. */
    private static int sessionCount(UUID jobUuid, List<ClassMarketplaceJobSessionTemplate> templates) {
        int count = 0;
        for (ClassMarketplaceJobSessionTemplate template : templates) {
            try {
                count += RecurrenceExpander.expand(template.getStartTime(), template.getEndTime(),
                        pattern(template), template.getTimezone()).size();
            } catch (RuntimeException ex) {
                log.debug("Could not expand session template {} of job {}: {}", template.getUuid(), jobUuid, ex.getMessage());
                count += 1;
            }
        }
        return count;
    }

    private static RecurrencePattern pattern(ClassMarketplaceJobSessionTemplate template) {
        if (template.getRecurrenceType() == null || template.getRecurrenceType().isBlank()) {
            return null;
        }
        return new RecurrencePattern(
                RecurrenceFrequency.valueOf(template.getRecurrenceType().trim().toUpperCase(Locale.ROOT)),
                template.getIntervalValue(),
                template.getDaysOfWeek(),
                template.getDayOfMonth(),
                template.getEndDate(),
                template.getOccurrenceCount());
    }

    private static String lookup(Map<UUID, String> values, UUID key) {
        return key == null ? null : values.get(key);
    }
}
