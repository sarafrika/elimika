package apps.sarafrika.elimika.classes.internal.matching;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobEligibilityDTO;
import apps.sarafrika.elimika.classes.dto.JobCandidateDTO;
import apps.sarafrika.elimika.classes.dto.JobMatchDTO;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.search.ClassesSearch;
import apps.sarafrika.elimika.classes.search.MarketplaceJobSearchScopes;
import apps.sarafrika.elimika.classes.search.MarketplaceJobSearchSource;
import apps.sarafrika.elimika.classes.service.impl.ClassMarketplaceJobServiceImpl;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.spi.InstructorTrainingApprovals;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchProfile;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchingService;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Job search and matching (rules-v1): open jobs for an instructor, and instructors for an
 * organisation's job. Hard eligibility is the job service's and always decides; this only ranks and
 * explains. Instructor pay never reaches the index, and the organisation side never sees rates,
 * clash details or the diary.
 */
@Service
@Transactional(readOnly = true)
public class JobMatchService {

    public static final String SURFACE_JOB_MATCHES = "job_matches";
    public static final String SURFACE_JOB_CANDIDATES = "job_candidates";

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_MATCH_LIMIT = 50;
    static final int MAX_CANDIDATE_LIMIT = 20;
    /** Index hits considered per request. */
    static final int SEARCH_POOL = 100;
    /** Jobs whose eligibility is checked (the batch eligibility limit). */
    static final int ELIGIBILITY_POOL = 50;
    /** Candidates whose rate is looked up before the final ranking. */
    static final int RATE_POOL = 40;
    /** How far an in-person job looks for opted-in instructors first. */
    static final int CANDIDATE_RADIUS_KM = 25;
    static final int CLOSING_SOON_DAYS = 7;

    private final ClassMarketplaceJobServiceImpl jobService;
    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassesSearch classesSearch;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    private final CourseInfoService courseInfoService;
    private final InstructorMatchingService instructorMatchingService;
    private final JobRequiredSkills jobRequiredSkills;
    private final SkillLookupService skillLookupService;
    private final BranchLocationResolver branchLocationResolver;
    private final DomainSecurityService domainSecurityService;
    private final DiscoveryTracker discoveryTracker;
    private final Clock clock;

    @Autowired
    public JobMatchService(ClassMarketplaceJobServiceImpl jobService,
                           ClassMarketplaceJobRepository jobRepository,
                           ClassesSearch classesSearch,
                           CourseTrainingApprovalSpi courseTrainingApprovalSpi,
                           CourseInfoService courseInfoService,
                           InstructorMatchingService instructorMatchingService,
                           JobRequiredSkills jobRequiredSkills,
                           SkillLookupService skillLookupService,
                           BranchLocationResolver branchLocationResolver,
                           DomainSecurityService domainSecurityService,
                           DiscoveryTracker discoveryTracker) {
        this(jobService, jobRepository, classesSearch, courseTrainingApprovalSpi, courseInfoService,
                instructorMatchingService, jobRequiredSkills, skillLookupService, branchLocationResolver,
                domainSecurityService, discoveryTracker, Clock.systemUTC());
    }

    JobMatchService(ClassMarketplaceJobServiceImpl jobService,
                    ClassMarketplaceJobRepository jobRepository,
                    ClassesSearch classesSearch,
                    CourseTrainingApprovalSpi courseTrainingApprovalSpi,
                    CourseInfoService courseInfoService,
                    InstructorMatchingService instructorMatchingService,
                    JobRequiredSkills jobRequiredSkills,
                    SkillLookupService skillLookupService,
                    BranchLocationResolver branchLocationResolver,
                    DomainSecurityService domainSecurityService,
                    DiscoveryTracker discoveryTracker,
                    Clock clock) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.classesSearch = classesSearch;
        this.courseTrainingApprovalSpi = courseTrainingApprovalSpi;
        this.courseInfoService = courseInfoService;
        this.instructorMatchingService = instructorMatchingService;
        this.jobRequiredSkills = jobRequiredSkills;
        this.skillLookupService = skillLookupService;
        this.branchLocationResolver = branchLocationResolver;
        this.domainSecurityService = domainSecurityService;
        this.discoveryTracker = discoveryTracker;
        this.clock = clock;
    }

    // ===================================================================== instructor -> jobs

    /**
     * Open jobs the current instructor is approved to teach, best fit first and ineligible ones last.
     *
     * @param radiusKm only used when the instructor has opted in to location search and has coordinates
     */
    public JobMatchDTO.Page matchesForCurrentInstructor(Integer limit, Integer radiusKm) {
        UUID instructorUuid = jobService.requireCurrentInstructor();
        int size = Math.clamp(limit == null ? DEFAULT_LIMIT : limit, 1, MAX_MATCH_LIMIT);
        UUID recommendationId = UUID.randomUUID();

        // 1. Hard filter: what the instructor is approved to teach, straight from SQL.
        InstructorTrainingApprovals approvals = courseTrainingApprovalSpi.findInstructorApprovals(instructorUuid);
        Set<UUID> courses = approvals.courses().keySet();
        Set<UUID> programs = approvals.programs().keySet();
        if (courses.isEmpty() && programs.isEmpty()) {
            return new JobMatchDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, List.of());
        }
        InstructorMatchProfile profile = instructorMatchingService.findMatchProfiles(List.of(instructorUuid))
                .get(instructorUuid);
        SearchGeoPoint instructorPoint = profile == null ? null : profile.searchPoint();
        NearMe near = radiusKm != null && instructorPoint != null
                ? new NearMe(instructorPoint.lat(), instructorPoint.lng(), radiusKm)
                : null;

        // 2. The index narrows to open, approved, still-open-for-registration jobs.
        LocalDate today = LocalDate.now(clock);
        List<ClassMarketplaceJob> jobs = searchMatchingJobs(courses, programs, near).stream()
                .filter(job -> job.getStatus() == ClassMarketplaceJobStatus.OPEN)
                .filter(job -> job.getCourseUuid() != null
                        ? courses.contains(job.getCourseUuid())
                        : programs.contains(job.getProgramUuid()))
                .filter(job -> job.getRegistrationPeriodEndDate() == null
                        || !job.getRegistrationPeriodEndDate().isBefore(today))
                .toList();
        if (jobs.isEmpty()) {
            return new JobMatchDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, List.of());
        }

        // 3. Score the hydrated rows (pay included, never indexed).
        Map<UUID, JobRequiredSkills.Effective> required = jobRequiredSkills.forJobs(jobs);
        var branchPins = BranchLocationResolver.branchPinMemo();
        List<ScoredJob> scored = new ArrayList<>();
        for (ClassMarketplaceJob job : jobs) {
            List<JobRequiredSkills.Tag> tags = requiredTags(required.get(job.getUuid()));
            String band = isLocated(job.getLocationType())
                    ? JobMatchScoring.distanceBand(instructorPoint, jobPoint(job, branchPins))
                    : null;
            BigDecimal approvedRate = approvedRate(approvals, job);
            JobMatchScoring.Result result = JobMatchScoring.score(new JobMatchScoring.Inputs(
                    tags,
                    profile == null ? Map.of() : profile.skillLevels(),
                    job.getLocationType(),
                    band,
                    job.getInstructorPay(),
                    approvedRate,
                    profile == null ? 0 : profile.yearsOfExperience(),
                    profile == null ? null : profile.ratingBayes(),
                    job.getRegistrationPeriodEndDate(),
                    today,
                    instructorUuid.equals(job.getPreferredInstructorUuid())));
            scored.add(new ScoredJob(job, tags, band, result));
        }
        scored.sort(Comparator.comparingDouble((ScoredJob s) -> s.result().score()).reversed());

        // 4. Hard eligibility on the best 50, exactly as the eligibility endpoints answer it.
        List<ScoredJob> pool = scored.subList(0, Math.min(ELIGIBILITY_POOL, scored.size()));
        List<ClassMarketplaceJobEligibilityDTO> eligibility = jobService.assessEligibilityForInstructor(
                pool.stream().map(ScoredJob::job).toList(), instructorUuid);
        Map<UUID, ClassMarketplaceJobEligibilityDTO> eligibilityByJob = new HashMap<>();
        eligibility.forEach(answer -> eligibilityByJob.put(answer.jobUuid(), answer));

        // 5. Ineligible last (stable, so each half keeps its score order).
        List<ScoredJob> ordered = new ArrayList<>(pool);
        ordered.sort(Comparator.comparing((ScoredJob s) -> !isEligible(eligibilityByJob.get(s.job().getUuid()))));
        ordered = ordered.subList(0, Math.min(size, ordered.size()));

        List<ClassMarketplaceJobDTO> dtos = jobService.toJobDTOs(ordered.stream().map(ScoredJob::job).toList());
        Map<UUID, SkillSummary> skills = skillNames(ordered.stream().flatMap(s -> s.tags().stream()).toList());
        Map<UUID, String> contextNames = learningContextNames(ordered.stream().map(ScoredJob::job).toList());

        List<JobMatchDTO> items = new ArrayList<>();
        List<DiscoveryImpression> impressions = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            ScoredJob s = ordered.get(i);
            ClassMarketplaceJobEligibilityDTO answer = eligibilityByJob.get(s.job().getUuid());
            List<Reason> reasons = instructorReasons(instructorUuid, s, answer, contextNames, today);
            items.add(new JobMatchDTO(dtos.get(i), new JobMatchDTO.Match(
                    s.result().score(),
                    toSkills(s.tags().stream().filter(tag -> s.result().matchedSkillUuids().contains(tag.skillUuid()))
                            .toList(), skills),
                    toSkills(s.tags(), skills),
                    reasons.stream().map(Reason::text).toList(),
                    answer)));
            impressions.add(new DiscoveryImpression("marketplace_job", s.job().getUuid(), i,
                    reasons.stream().map(Reason::code).toList()));
        }
        track(SURFACE_JOB_MATCHES, recommendationId, impressions);
        return new JobMatchDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, items);
    }

    private List<ClassMarketplaceJob> searchMatchingJobs(Set<UUID> courses, Set<UUID> programs, NearMe near) {
        List<SearchFilter> approved = new ArrayList<>();
        if (!courses.isEmpty()) {
            approved.add(SearchFilter.in("course_uuid", courses));
        }
        if (!programs.isEmpty()) {
            approved.add(SearchFilter.in("program_uuid", programs));
        }
        long now = clock.instant().getEpochSecond();
        // NOT (< now) rather than >= now, so a job with no closing date stays in.
        List<SearchFilter> filters = new ArrayList<>(List.of(
                SearchFilter.or(approved),
                SearchFilter.not(SearchFilter.lt("registration_closes_at", now))));
        if (near != null) {
            filters.add(near.filter());
        }
        List<SearchSort> sort = near != null ? List.of(near.sort()) : List.of(SearchSort.asc("starts_at"));
        SearchRequest request = new SearchRequest(MarketplaceJobSearchSource.INDEX, null, SearchFilter.and(filters),
                MarketplaceJobSearchScopes.forCaller(false, null, false), sort, 0, SEARCH_POOL, List.of(), null);
        List<UUID> hits = classesSearch.search(request).uuids();
        if (hits.isEmpty()) {
            return List.of();
        }
        Map<UUID, ClassMarketplaceJob> byUuid = new HashMap<>();
        jobRepository.findByUuidIn(hits).forEach(job -> byUuid.put(job.getUuid(), job));
        return hits.stream().map(byUuid::get).filter(Objects::nonNull).toList();
    }

    private static BigDecimal approvedRate(InstructorTrainingApprovals approvals, ClassMarketplaceJob job) {
        Optional<BigDecimal> rate = job.getCourseUuid() != null
                ? approvals.courseRate(job.getCourseUuid(), job.getSessionFormat(), job.getLocationType(), job.getRateBasis())
                : approvals.programRate(job.getProgramUuid(), job.getSessionFormat(), job.getLocationType(), job.getRateBasis());
        return rate.filter(value -> value.signum() > 0).orElse(null);
    }

    private List<Reason> instructorReasons(UUID instructorUuid,
                                           ScoredJob s,
                                           ClassMarketplaceJobEligibilityDTO answer,
                                           Map<UUID, String> contextNames,
                                           LocalDate today) {
        List<Reason> reasons = new ArrayList<>();
        ClassMarketplaceJob job = s.job();
        if (instructorUuid.equals(job.getPreferredInstructorUuid())) {
            reasons.add(new Reason("PREFERRED", "The organisation asked for you"));
        }
        skillReason(s.result()).ifPresent(reasons::add);
        reasons.add(approvedReason(job, contextNames));
        if (s.band() != null) {
            reasons.add(new Reason("NEARBY", JobMatchScoring.distanceReason(s.band())));
        }
        if (s.result().payAbovePercent() != null) {
            reasons.add(new Reason("PAY_ABOVE_RATE",
                    "Pay is " + s.result().payAbovePercent() + "% above your approved rate"));
        }
        if (answer != null && answer.scheduleClear()) {
            reasons.add(new Reason("SCHEDULE_CLEAR", "No schedule clashes"));
        }
        if (job.getRegistrationPeriodEndDate() != null) {
            long days = ChronoUnit.DAYS.between(today, job.getRegistrationPeriodEndDate());
            if (days >= 0 && days <= CLOSING_SOON_DAYS) {
                reasons.add(new Reason("CLOSING_SOON", days == 0 ? "Registration closes today"
                        : "Registration closes in " + days + (days == 1 ? " day" : " days")));
            }
        }
        return reasons;
    }

    // ===================================================================== job -> instructors

    /**
     * Verified instructors approved to teach the job's course or program, best fit first. For the
     * posting organisation's managers and platform admins only; a fit summary, never rates or clashes.
     */
    public JobCandidateDTO.Page candidatesForJob(UUID jobUuid, Integer limit) {
        ClassMarketplaceJob job = jobService.loadJobForCandidateReview(jobUuid);
        int size = Math.clamp(limit == null ? DEFAULT_LIMIT : limit, 1, MAX_CANDIDATE_LIMIT);
        UUID recommendationId = UUID.randomUUID();

        Set<UUID> approved = job.getCourseUuid() != null
                ? courseTrainingApprovalSpi.approvedInstructorUuidsForCourse(job.getCourseUuid())
                : courseTrainingApprovalSpi.approvedInstructorUuidsForProgram(job.getProgramUuid());
        if (approved.isEmpty()) {
            return new JobCandidateDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, job.getUuid(), List.of());
        }
        SearchGeoPoint jobPoint = isLocated(job.getLocationType())
                ? jobPoint(job, BranchLocationResolver.branchPinMemo())
                : null;
        NearMe near = job.getLocationType() == LocationType.IN_PERSON && jobPoint != null
                ? new NearMe(jobPoint.lat(), jobPoint.lng(), CANDIDATE_RADIUS_KM)
                : null;
        List<UUID> hits = instructorMatchingService.searchVerifiedAmong(approved, near, SEARCH_POOL);
        Map<UUID, InstructorMatchProfile> profiles = instructorMatchingService.findMatchProfiles(hits);
        // The database, not the index, has the last word on verification and approval.
        List<InstructorMatchProfile> candidates = hits.stream()
                .map(profiles::get)
                .filter(Objects::nonNull)
                .filter(InstructorMatchProfile::adminVerified)
                .filter(profile -> approved.contains(profile.instructorUuid()))
                .toList();
        if (candidates.isEmpty()) {
            return new JobCandidateDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, job.getUuid(), List.of());
        }

        List<JobRequiredSkills.Tag> tags = requiredTags(jobRequiredSkills.forJob(job));
        LocalDate today = LocalDate.now(clock);
        // A first ranking without rates, then rates for the front of the queue only.
        List<ScoredCandidate> ranked = new ArrayList<>(candidates.stream()
                .map(profile -> scoreCandidate(job, jobPoint, tags, profile, null, today))
                .sorted(Comparator.comparingDouble((ScoredCandidate s) -> s.result().score()).reversed())
                .limit(RATE_POOL)
                .toList());
        Map<UUID, BigDecimal> rates = jobService.approvedRatesForJob(job,
                ranked.stream().map(s -> s.profile().instructorUuid()).toList());
        ranked = new ArrayList<>(ranked.stream()
                .map(s -> scoreCandidate(job, jobPoint, tags, s.profile(), rates.get(s.profile().instructorUuid()), today))
                .sorted(Comparator.comparingDouble((ScoredCandidate s) -> s.result().score()).reversed())
                .limit(size)
                .toList());

        Map<UUID, ClassMarketplaceJobEligibilityDTO> eligibility = jobService.assessEligibilityForCandidates(job,
                ranked.stream().map(s -> s.profile().instructorUuid()).toList());
        ranked.sort(Comparator.comparing((ScoredCandidate s) -> !fitsBrief(eligibility.get(s.profile().instructorUuid()))));

        Map<UUID, String> contextNames = learningContextNames(List.of(job));
        List<JobCandidateDTO> items = new ArrayList<>();
        List<DiscoveryImpression> impressions = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            ScoredCandidate s = ranked.get(i);
            InstructorMatchProfile profile = s.profile();
            ClassMarketplaceJobEligibilityDTO answer = eligibility.get(profile.instructorUuid());
            boolean scheduleClear = answer != null && answer.scheduleClear();
            boolean rateWithinBudget = answer != null && answer.rateOk();
            List<Reason> reasons = organisationReasons(job, s, scheduleClear, contextNames);
            items.add(new JobCandidateDTO(profile.instructorUuid(), profile.displayName(), profile.locationName(),
                    profile.adminVerified(), new JobCandidateDTO.Match(s.result().score(),
                    reasons.stream().map(Reason::text).toList(), scheduleClear, rateWithinBudget)));
            impressions.add(new DiscoveryImpression("instructor", profile.instructorUuid(), i,
                    reasons.stream().map(Reason::code).toList()));
        }
        track(SURFACE_JOB_CANDIDATES, recommendationId, impressions);
        return new JobCandidateDTO.Page(recommendationId, JobMatchScoring.MODEL_VERSION, job.getUuid(), items);
    }

    private ScoredCandidate scoreCandidate(ClassMarketplaceJob job,
                                           SearchGeoPoint jobPoint,
                                           List<JobRequiredSkills.Tag> tags,
                                           InstructorMatchProfile profile,
                                           BigDecimal approvedRate,
                                           LocalDate today) {
        String band = isLocated(job.getLocationType())
                ? JobMatchScoring.distanceBand(profile.searchPoint(), jobPoint)
                : null;
        JobMatchScoring.Result result = JobMatchScoring.score(new JobMatchScoring.Inputs(
                tags,
                profile.skillLevels(),
                job.getLocationType(),
                band,
                job.getInstructorPay(),
                approvedRate,
                profile.yearsOfExperience(),
                profile.ratingBayes(),
                job.getRegistrationPeriodEndDate(),
                today,
                profile.instructorUuid().equals(job.getPreferredInstructorUuid())));
        return new ScoredCandidate(profile, band, result);
    }

    /** The organisation's reasons: never the rate, never a clash count or time. */
    private List<Reason> organisationReasons(ClassMarketplaceJob job,
                                             ScoredCandidate s,
                                             boolean scheduleClear,
                                             Map<UUID, String> contextNames) {
        List<Reason> reasons = new ArrayList<>();
        InstructorMatchProfile profile = s.profile();
        if (profile.instructorUuid().equals(job.getPreferredInstructorUuid())) {
            reasons.add(new Reason("PREFERRED", "Your preferred instructor"));
        }
        skillReason(s.result()).ifPresent(reasons::add);
        reasons.add(approvedReason(job, contextNames));
        if (s.band() != null) {
            reasons.add(new Reason("NEARBY", JobMatchScoring.distanceReason(s.band())));
        }
        if (scheduleClear) {
            reasons.add(new Reason("SCHEDULE_CLEAR", "No schedule clashes"));
        }
        int years = (int) Math.floor(profile.yearsOfExperience());
        if (years >= 1) {
            reasons.add(new Reason("EXPERIENCED", years + (years == 1 ? " year" : " years") + " of experience"));
        }
        if (profile.reviewCount() > 0 && profile.ratingAverage() != null) {
            reasons.add(new Reason("RATED", "Rated " + BigDecimal.valueOf(profile.ratingAverage())
                    .setScale(1, RoundingMode.HALF_UP).toPlainString() + "/5 from " + profile.reviewCount()
                    + (profile.reviewCount() == 1 ? " review" : " reviews")));
        }
        return reasons;
    }

    // ===================================================================== shared

    private static Optional<Reason> skillReason(JobMatchScoring.Result result) {
        // With no required skills coverage is 1.0 but there is nothing to count.
        if (result.requiredSkillCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(new Reason("SKILLS_MATCHED", "Matches " + result.matchedSkillUuids().size() + "/"
                + result.requiredSkillCount() + " required skills"));
    }

    private static Reason approvedReason(ClassMarketplaceJob job, Map<UUID, String> contextNames) {
        UUID context = job.getCourseUuid() != null ? job.getCourseUuid() : job.getProgramUuid();
        String name = contextNames.get(context);
        return new Reason("APPROVED", name == null || name.isBlank()
                ? "Approved to teach this " + (job.getCourseUuid() != null ? "course" : "training program")
                : "Approved to teach " + name);
    }

    private Map<UUID, String> learningContextNames(List<ClassMarketplaceJob> jobs) {
        Set<UUID> courseUuids = new HashSet<>();
        Set<UUID> programUuids = new HashSet<>();
        jobs.forEach(job -> {
            if (job.getCourseUuid() != null) {
                courseUuids.add(job.getCourseUuid());
            } else if (job.getProgramUuid() != null) {
                programUuids.add(job.getProgramUuid());
            }
        });
        Map<UUID, String> names = new HashMap<>();
        if (!courseUuids.isEmpty()) {
            names.putAll(courseInfoService.getCourseNames(courseUuids));
        }
        if (!programUuids.isEmpty()) {
            names.putAll(courseInfoService.getTrainingProgramTitles(programUuids));
        }
        return names;
    }

    private Map<UUID, SkillSummary> skillNames(Collection<JobRequiredSkills.Tag> tags) {
        Set<UUID> uuids = new LinkedHashSet<>();
        tags.forEach(tag -> uuids.add(tag.skillUuid()));
        Map<UUID, SkillSummary> byUuid = new HashMap<>();
        if (!uuids.isEmpty()) {
            skillLookupService.findByUuids(uuids).forEach(skill -> byUuid.put(skill.uuid(), skill));
        }
        return byUuid;
    }

    private static List<JobMatchDTO.Skill> toSkills(List<JobRequiredSkills.Tag> tags, Map<UUID, SkillSummary> names) {
        return tags.stream()
                .map(tag -> new JobMatchDTO.Skill(tag.skillUuid(),
                        Optional.ofNullable(names.get(tag.skillUuid())).map(SkillSummary::name).orElse(null),
                        tag.minProficiency(), tag.mandatory()))
                .toList();
    }

    private static List<JobRequiredSkills.Tag> requiredTags(JobRequiredSkills.Effective effective) {
        return effective == null ? List.of() : effective.tags();
    }

    private SearchGeoPoint jobPoint(ClassMarketplaceJob job,
                                    Map<UUID, Optional<BranchLocation>> branchPins) {
        return branchLocationResolver.searchPoint(job.getOrganisationUuid(), job.getBranchUuid(),
                job.getLocationType(), job.getLocationLatitude(), job.getLocationLongitude(), branchPins);
    }

    private static boolean isLocated(LocationType locationType) {
        return locationType == LocationType.IN_PERSON || locationType == LocationType.HYBRID;
    }

    private static boolean isEligible(ClassMarketplaceJobEligibilityDTO answer) {
        return answer != null && answer.eligible();
    }

    private static boolean fitsBrief(ClassMarketplaceJobEligibilityDTO answer) {
        return answer != null && answer.scheduleClear() && answer.rateOk();
    }

    private void track(String surface, UUID recommendationId, List<DiscoveryImpression> impressions) {
        if (impressions.isEmpty()) {
            return;
        }
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid != null) {
            discoveryTracker.recordImpressions(userUuid, surface, recommendationId, JobMatchScoring.MODEL_VERSION,
                    impressions);
        }
    }

    private record Reason(String code, String text) {
    }

    private record ScoredJob(ClassMarketplaceJob job,
                             List<JobRequiredSkills.Tag> tags,
                             String band,
                             JobMatchScoring.Result result) {
    }

    private record ScoredCandidate(InstructorMatchProfile profile, String band, JobMatchScoring.Result result) {
    }
}
