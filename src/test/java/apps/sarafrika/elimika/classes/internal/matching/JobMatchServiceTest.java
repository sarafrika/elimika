package apps.sarafrika.elimika.classes.internal.matching;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.JobCandidateDTO;
import apps.sarafrika.elimika.classes.dto.JobMatchDTO;
import apps.sarafrika.elimika.classes.internal.AuditUserResolver;
import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.classes.internal.MarketplaceApplicationHistory;
import apps.sarafrika.elimika.classes.internal.MarketplaceHireClashNotifier;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobRequiredSkill;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionResourceRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationEventRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRequiredSkillRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobResourceRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.search.ClassesSearch;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.impl.ClassMarketplaceJobServiceImpl;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseSkillLookupService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.course.spi.InstructorTrainingApprovals;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchProfile;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchingService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Job matching over the real job service (eligibility, apply, DTO mapping) with its collaborators
 * mocked: what the instructor is told is eligible must be what {@code applyToJob} accepts, and the
 * organisation's view must never carry rates, clash detail or the diary.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobMatchServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-25T08:00:00Z"), ZoneOffset.UTC);
    private static final BigDecimal RATE = new BigDecimal("200.00");

    @Mock private ClassMarketplaceJobRepository jobRepository;
    @Mock private ClassMarketplaceJobApplicationRepository applicationRepository;
    @Mock private ClassMarketplaceJobSessionTemplateRepository sessionTemplateRepository;
    @Mock private ClassMarketplaceJobResourceRepository jobResourceRepository;
    @Mock private ClassDefinitionResourceRepository classDefinitionResourceRepository;
    @Mock private ClassMarketplaceJobRequiredSkillRepository requiredSkillRepository;
    @Mock private ClassMarketplaceJobApplicationEventRepository eventRepository;
    @Mock private CourseInfoService courseInfoService;
    @Mock private CourseTrainingApprovalSpi courseTrainingApprovalSpi;
    @Mock private CourseSkillLookupService courseSkillLookupService;
    @Mock private UserLookupService userLookupService;
    @Mock private apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService organisationAffiliationService;
    @Mock private apps.sarafrika.elimika.tenancy.spi.StudentGroupLookupService studentGroupLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private InstructorMatchingService instructorMatchingService;
    @Mock private DomainSecurityService domainSecurityService;
    @Mock private ClassDefinitionServiceInterface classDefinitionService;
    @Mock private apps.sarafrika.elimika.resourcing.spi.ResourceBookingService resourceBookingService;
    @Mock private apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService instructorTimeHoldService;
    @Mock private apps.sarafrika.elimika.resourcing.spi.ResourceLookupService resourceLookupService;
    @Mock private apps.sarafrika.elimika.availability.spi.AvailabilityService availabilityService;
    @Mock private ObjectProvider<TimetableService> timetableServiceProvider;
    @Mock private TimetableService timetableService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private TrainingBranchLookupService trainingBranchLookupService;
    @Mock private OrganisationLookupService organisationLookupService;
    @Mock private ClassesSearch classesSearch;
    @Mock private SkillLookupService skillLookupService;
    @Mock private DiscoveryTracker discoveryTracker;

    private ClassMarketplaceJobServiceImpl jobService;
    private JobMatchService matchService;

    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();
    /** course -> approved rate for the current instructor. */
    private final Map<UUID, BigDecimal> approvedCourses = new LinkedHashMap<>();
    private final Map<UUID, ClassMarketplaceJob> jobs = new LinkedHashMap<>();
    private final List<ClassMarketplaceJobApplication> applications = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jobService = new ClassMarketplaceJobServiceImpl(
                jobRepository, applicationRepository, sessionTemplateRepository, jobResourceRepository,
                classDefinitionResourceRepository, courseInfoService, courseTrainingApprovalSpi, userLookupService,
                organisationAffiliationService, studentGroupLookupService, instructorLookupService,
                domainSecurityService, new RequestScopedCache(), classDefinitionService, resourceBookingService,
                instructorTimeHoldService, resourceLookupService, availabilityService, timetableServiceProvider,
                eventPublisher, mock(apps.sarafrika.elimika.shared.storage.service.MediaStorageService.class),
                mock(apps.sarafrika.elimika.shared.storage.service.MediaValidationService.class),
                mock(apps.sarafrika.elimika.shared.storage.config.StorageProperties.class),
                new BranchLocationResolver(trainingBranchLookupService),
                new MarketplaceHireClashNotifier(userLookupService, instructorLookupService,
                        organisationLookupService, eventPublisher, new AuditUserResolver(userLookupService)),
                new AuditUserResolver(userLookupService),
                new MarketplaceApplicationHistory(eventRepository, domainSecurityService, userLookupService),
                organisationLookupService,
                classesSearch);
        matchService = new JobMatchService(jobService, jobRepository, classesSearch, courseTrainingApprovalSpi,
                courseInfoService, instructorMatchingService,
                new JobRequiredSkills(requiredSkillRepository, courseSkillLookupService), skillLookupService,
                new BranchLocationResolver(trainingBranchLookupService), domainSecurityService, discoveryTracker,
                CLOCK);

        when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        when(availabilityService.isInstructorAvailable(any(), any(), any())).thenReturn(true);
        when(trainingBranchLookupService.findBranch(any(), any())).thenReturn(Optional.empty());
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(userUuid);
        when(domainSecurityService.isInstructor()).thenReturn(true);
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(domainSecurityService.isVerifiedInstructor()).thenReturn(true);
        when(instructorLookupService.isInstructorAdminVerified(instructorUuid)).thenReturn(Optional.of(true));
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // One consistent world for the batch and the single-job paths.
        when(courseTrainingApprovalSpi.findInstructorApprovals(instructorUuid)).thenAnswer(invocation -> {
            Map<UUID, InstructorTrainingApprovals.ApprovedRateCard> cards = new HashMap<>();
            approvedCourses.forEach((course, rate) -> cards.put(course, (format, location, basis) -> Optional.of(rate)));
            return new InstructorTrainingApprovals(cards, Map.of());
        });
        when(courseTrainingApprovalSpi.isInstructorApproved(any(), eq(instructorUuid)))
                .thenAnswer(invocation -> approvedCourses.containsKey(invocation.<UUID>getArgument(0)));
        when(courseTrainingApprovalSpi.resolveInstructorRate(any(), eq(instructorUuid), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(approvedCourses.get(invocation.<UUID>getArgument(0))));
        when(jobRepository.findByUuidIn(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> wanted = invocation.getArgument(0);
            return jobs.values().stream().filter(job -> wanted.contains(job.getUuid())).toList();
        });
        when(jobRepository.findByUuid(any())).thenAnswer(invocation ->
                Optional.ofNullable(jobs.get(invocation.<UUID>getArgument(0))));
        when(applicationRepository.findByInstructorUuidAndJobUuidIn(eq(instructorUuid), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<UUID> wanted = invocation.getArgument(1);
                    return applications.stream().filter(app -> wanted.contains(app.getJobUuid())).toList();
                });
        when(applicationRepository.findByJobUuidAndInstructorUuid(any(), eq(instructorUuid)))
                .thenAnswer(invocation -> applications.stream()
                        .filter(app -> app.getJobUuid().equals(invocation.getArgument(0))).findFirst());
        when(classesSearch.search(any())).thenAnswer(invocation ->
                new ClassesSearch.Hits(List.copyOf(jobs.keySet()), jobs.size()));
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(instructorUuid,
                profile(instructorUuid, true, Map.of(), null)));
    }

    // ============================================================ instructor -> jobs

    @Test
    void everyJobReturnedAsEligibleIsAcceptedByApplyAndEveryIneligibleOneIsRefused() {
        ClassMarketplaceJob open = job(new BigDecimal("240.00"));
        ClassMarketplaceJob underpaid = job(new BigDecimal("150.00"));
        ClassMarketplaceJob clashing = job(new BigDecimal("260.00"));
        ClassMarketplaceJob applied = job(new BigDecimal("300.00"));
        ClassMarketplaceJob unapproved = job(new BigDecimal("400.00"));
        approvedCourses.remove(unapproved.getCourseUuid());

        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(clashing.getUuid()))
                .thenReturn(List.of(saturdayTemplate(clashing.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(instructorUuid), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(instance(LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 12, 0))));
        ClassMarketplaceJobApplication live = new ClassMarketplaceJobApplication();
        live.setUuid(UUID.randomUUID());
        live.setJobUuid(applied.getUuid());
        live.setInstructorUuid(instructorUuid);
        live.setStatus(ClassMarketplaceJobApplicationStatus.SHORTLISTED);
        applications.add(live);

        JobMatchDTO.Page page = matchService.matchesForCurrentInstructor(50, null);

        List<UUID> returned = page.items().stream().map(item -> item.job().uuid()).toList();
        assertThat(returned).doesNotContain(unapproved.getUuid())
                .containsExactlyInAnyOrder(open.getUuid(), underpaid.getUuid(), clashing.getUuid(), applied.getUuid());
        // Ineligible jobs sit after every eligible one.
        List<Boolean> eligibleFlags = page.items().stream().map(item -> item.match().eligibility().eligible()).toList();
        assertThat(eligibleFlags).isSortedAccordingTo((a, b) -> Boolean.compare(b, a));

        for (JobMatchDTO item : page.items()) {
            UUID jobUuid = item.job().uuid();
            if (item.match().eligibility().eligible()) {
                assertThatCode(() -> jobService.applyToJob(jobUuid, new ClassMarketplaceJobApplicationRequestDTO("Keen")))
                        .as("eligible job %s must accept the application", jobUuid)
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> jobService.applyToJob(jobUuid, new ClassMarketplaceJobApplicationRequestDTO("Keen")))
                        .as("ineligible job %s must refuse the application", jobUuid)
                        .isInstanceOf(RuntimeException.class);
            }
        }
        assertThat(page.items().stream().filter(item -> item.match().eligibility().eligible()))
                .extracting(item -> item.job().uuid())
                .containsExactly(open.getUuid());
    }

    @Test
    void matchedSkillCountReconcilesWithTheStoredRequiredSkillRows() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        UUID python = UUID.randomUUID();
        UUID sql = UUID.randomUUID();
        UUID excel = UUID.randomUUID();
        List<ClassMarketplaceJobRequiredSkill> rows = List.of(
                requiredSkill(job.getUuid(), python, ProficiencyLevel.INTERMEDIATE, true),
                requiredSkill(job.getUuid(), sql, ProficiencyLevel.ADVANCED, false),
                requiredSkill(job.getUuid(), excel, ProficiencyLevel.BEGINNER, false));
        when(requiredSkillRepository.findByJobUuidInOrderByIdAsc(anyCollection())).thenReturn(rows);
        when(skillLookupService.findByUuids(anyCollection())).thenReturn(List.of(
                skill(python, "Python"), skill(sql, "SQL"), skill(excel, "Excel")));
        // Python meets its minimum, SQL is held below it, Excel not at all.
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(instructorUuid,
                profile(instructorUuid, true, Map.of(python, ProficiencyLevel.EXPERT, sql, ProficiencyLevel.BEGINNER), null)));

        JobMatchDTO.Match match = matchService.matchesForCurrentInstructor(20, null).items().getFirst().match();

        long expectedMatched = rows.stream().filter(row -> row.getSkillUuid().equals(python)).count();
        assertThat(match.requiredSkills()).hasSize(rows.size());
        assertThat(match.matchedSkills()).extracting(JobMatchDTO.Skill::skillUuid).containsExactly(python);
        assertThat(match.reasons()).contains("Matches " + expectedMatched + "/" + rows.size() + " required skills");
        assertThat(match.requiredSkills()).extracting(JobMatchDTO.Skill::skillName)
                .containsExactly("Python", "SQL", "Excel");
    }

    @Test
    void aJobWithNoRequiredSkillsCountsNoSkillsInItsReasons() {
        job(new BigDecimal("240.00"));

        JobMatchDTO.Match match = matchService.matchesForCurrentInstructor(20, null).items().getFirst().match();

        assertThat(match.requiredSkills()).isEmpty();
        assertThat(match.reasons()).noneMatch(reason -> reason.startsWith("Matches"));
        assertThat(match.reasons()).contains("Pay is 20% above your approved rate", "No schedule clashes");
    }

    @Test
    void anInstructorWhoHasNotOptedInGetsNoLocationReasonAndNoRadiusFilter() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        job.setLocationType(LocationType.IN_PERSON);
        job.setLocationLatitude(new BigDecimal("-1.292066"));
        job.setLocationLongitude(new BigDecimal("36.821945"));

        JobMatchDTO.Page page = matchService.matchesForCurrentInstructor(20, 5);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().match().reasons()).noneMatch(reason -> reason.contains("away"));
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(classesSearch).search(request.capture());
        assertThat(request.getValue().filter().toString()).doesNotContain("GeoRadius");
    }

    @Test
    void anOptedInInstructorIsToldTheDistanceBandAndSearchedWithinTheRadius() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        job.setLocationType(LocationType.IN_PERSON);
        job.setLocationLatitude(new BigDecimal("-1.29"));
        job.setLocationLongitude(new BigDecimal("36.82"));
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(instructorUuid,
                profile(instructorUuid, true, Map.of(), new SearchGeoPoint(-1.30, 36.85))));

        JobMatchDTO.Page page = matchService.matchesForCurrentInstructor(20, 10);

        assertThat(page.items().getFirst().match().reasons()).contains("About 2-5 km away");
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(classesSearch).search(request.capture());
        assertThat(request.getValue().filter().toString()).contains("GeoRadius");
    }

    @Test
    void matchesRecordImpressionsOnTheJobMatchesSurface() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));

        JobMatchDTO.Page page = matchService.matchesForCurrentInstructor(20, null);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<DiscoveryImpression>> items = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(discoveryTracker).recordImpressions(eq(userUuid), eq("job_matches"), eq(page.recommendationId()),
                eq("rules-v1"), items.capture());
        assertThat(items.getValue()).extracting(DiscoveryImpression::itemUuid).containsExactly(job.getUuid());
    }

    @Test
    void anInstructorApprovedForNothingIsNotSearched() {
        approvedCourses.clear();

        assertThat(matchService.matchesForCurrentInstructor(20, null).items()).isEmpty();
        verify(classesSearch, never()).search(any());
    }

    // ============================================================ job -> instructors

    @Test
    void candidatesNeverIncludeUnverifiedOrUnapprovedInstructors() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        allowOrganisationManager(job);
        UUID approvedVerified = UUID.randomUUID();
        UUID approvedUnverified = UUID.randomUUID();
        UUID unapproved = UUID.randomUUID();
        when(courseTrainingApprovalSpi.approvedInstructorUuidsForCourse(job.getCourseUuid()))
                .thenReturn(Set.of(approvedVerified, approvedUnverified));
        // A stale index hands back all three.
        when(instructorMatchingService.searchVerifiedAmong(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(unapproved, approvedUnverified, approvedVerified));
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(
                approvedVerified, profile(approvedVerified, true, Map.of(), null),
                approvedUnverified, profile(approvedUnverified, false, Map.of(), null),
                unapproved, profile(unapproved, true, Map.of(), null)));
        stubCandidateFacts(Map.of(approvedVerified, RATE));

        JobCandidateDTO.Page page = matchService.candidatesForJob(job.getUuid(), 20);

        assertThat(page.items()).extracting(JobCandidateDTO::instructorUuid).containsExactly(approvedVerified);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<UUID>> among = (ArgumentCaptor) ArgumentCaptor.forClass(Collection.class);
        verify(instructorMatchingService).searchVerifiedAmong(among.capture(), any(), anyInt());
        assertThat(among.getValue()).containsExactlyInAnyOrder(approvedVerified, approvedUnverified);
    }

    @Test
    void theOrganisationNeverReceivesRatesClashesOrTheDiary() throws Exception {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        allowOrganisationManager(job);
        UUID clear = UUID.randomUUID();
        UUID clashing = UUID.randomUUID();
        UUID expensive = UUID.randomUUID();
        when(courseTrainingApprovalSpi.approvedInstructorUuidsForCourse(job.getCourseUuid()))
                .thenReturn(Set.of(clear, clashing, expensive));
        when(instructorMatchingService.searchVerifiedAmong(anyCollection(), any(), anyInt()))
                .thenReturn(List.of(clear, clashing, expensive));
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(
                clear, profile(clear, true, Map.of(), null),
                clashing, profile(clashing, true, Map.of(), null),
                expensive, profile(expensive, true, Map.of(), null)));
        stubCandidateFacts(Map.of(clear, new BigDecimal("211.37"), clashing, new BigDecimal("212.49"),
                expensive, new BigDecimal("987.65")));
        when(sessionTemplateRepository.findByJobUuidOrderByCreatedDateAsc(job.getUuid()))
                .thenReturn(List.of(saturdayTemplate(job.getUuid())));
        when(timetableService.getScheduleForInstructor(eq(clashing), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        instance(LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 12, 0)),
                        instance(LocalDateTime.of(2026, 5, 16, 9, 0), LocalDateTime.of(2026, 5, 16, 12, 0))));

        JobCandidateDTO.Page page = matchService.candidatesForJob(job.getUuid(), 20);

        Map<UUID, JobCandidateDTO> byUuid = new HashMap<>();
        page.items().forEach(item -> byUuid.put(item.instructorUuid(), item));
        assertThat(byUuid.get(clear).match().scheduleClear()).isTrue();
        assertThat(byUuid.get(clear).match().rateWithinBudget()).isTrue();
        assertThat(byUuid.get(clashing).match().scheduleClear()).isFalse();
        assertThat(byUuid.get(expensive).match().rateWithinBudget()).isFalse();
        // Candidates who fit the brief come first.
        assertThat(page.items().getFirst().instructorUuid()).isEqualTo(clear);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(page);
        assertThat(json).doesNotContain("211.37", "212.49", "987.65", "240.00", "240.0")
                .doesNotContain("approved_rate", "instructor_pay", "rate_ok", "schedule_conflicts", "eligibility",
                        "requested_start", "2026-05-09", "2026-05-16", "Existing session")
                .doesNotContainIgnoringCase("clashes with")
                .doesNotContainIgnoringCase("your approved rate")
                .contains("\"schedule_clear\"", "\"rate_within_budget\"", "\"score\"", "\"reasons\"");
        // A match object carries exactly the four summary fields.
        var tree = new ObjectMapper().findAndRegisterModules().readTree(json);
        tree.get("items").forEach(item -> assertThat(item.get("match").fieldNames())
                .toIterable().containsExactlyInAnyOrder("score", "reasons", "schedule_clear", "rate_within_budget"));
    }

    @Test
    void candidatesAreRefusedToSomeoneWhoDoesNotManageThePostingOrganisation() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(false);

        assertThatThrownBy(() -> matchService.candidatesForJob(job.getUuid(), 20))
                .isInstanceOf(AccessDeniedException.class);
        verify(instructorMatchingService, never()).searchVerifiedAmong(anyCollection(), any(), anyInt());
    }

    @Test
    void aCandidateWhoHasNotOptedInGetsNoLocationReasonButStaysACandidate() {
        ClassMarketplaceJob job = job(new BigDecimal("240.00"));
        job.setLocationType(LocationType.IN_PERSON);
        job.setLocationLatitude(new BigDecimal("-1.29"));
        job.setLocationLongitude(new BigDecimal("36.82"));
        allowOrganisationManager(job);
        UUID nearby = UUID.randomUUID();
        UUID privateLocation = UUID.randomUUID();
        when(courseTrainingApprovalSpi.approvedInstructorUuidsForCourse(job.getCourseUuid()))
                .thenReturn(Set.of(nearby, privateLocation));
        when(instructorMatchingService.searchVerifiedAmong(anyCollection(), any(NearMe.class), anyInt()))
                .thenReturn(List.of(nearby, privateLocation));
        when(instructorMatchingService.findMatchProfiles(anyCollection())).thenReturn(Map.of(
                nearby, profile(nearby, true, Map.of(), new SearchGeoPoint(-1.29, 36.83)),
                privateLocation, profile(privateLocation, true, Map.of(), null)));
        stubCandidateFacts(Map.of(nearby, RATE, privateLocation, RATE));

        JobCandidateDTO.Page page = matchService.candidatesForJob(job.getUuid(), 20);

        Map<UUID, List<String>> reasons = new HashMap<>();
        page.items().forEach(item -> reasons.put(item.instructorUuid(), item.match().reasons()));
        assertThat(reasons).containsKeys(nearby, privateLocation);
        assertThat(reasons.get(nearby)).contains("Under 2 km away");
        assertThat(reasons.get(privateLocation)).noneMatch(reason -> reason.contains("away"));
        verify(discoveryTracker).recordImpressions(eq(userUuid), eq("job_candidates"), eq(page.recommendationId()),
                eq("rules-v1"), anyList());
    }

    // ============================================================ fixtures

    private ClassMarketplaceJob job(BigDecimal pay) {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setOrganisationUuid(UUID.randomUUID());
        job.setCourseUuid(UUID.randomUUID());
        job.setTitle("Weekend Data Analysis Bootcamp");
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setClassVisibility(ClassVisibility.PUBLIC);
        job.setSessionFormat(SessionFormat.GROUP);
        job.setLocationType(LocationType.ONLINE);
        job.setRegistrationPeriodStartDate(LocalDate.of(2026, 4, 20));
        job.setRegistrationPeriodEndDate(LocalDate.of(2026, 5, 1));
        job.setAcademicPeriodStartDate(LocalDate.of(2026, 5, 2));
        job.setAcademicPeriodEndDate(LocalDate.of(2026, 6, 6));
        job.setInstructorPay(pay);
        job.setSalePrice(new BigDecimal("500.00"));
        job.setRateBasis(RateBasis.PER_HOUR);
        jobs.put(job.getUuid(), job);
        approvedCourses.put(job.getCourseUuid(), RATE);
        return job;
    }

    private void allowOrganisationManager(ClassMarketplaceJob job) {
        when(domainSecurityService.managesOrganisation(job.getOrganisationUuid())).thenReturn(true);
    }

    /** Verification from the directory, approved rates by instructor, no existing applications. */
    private void stubCandidateFacts(Map<UUID, BigDecimal> rates) {
        when(instructorLookupService.findInstructorDirectoryEntries(anyCollection())).thenAnswer(invocation -> {
            Map<UUID, InstructorDirectoryEntry> entries = new HashMap<>();
            for (UUID uuid : invocation.<Collection<UUID>>getArgument(0)) {
                entries.put(uuid, new InstructorDirectoryEntry(uuid, "Instructor", null, true));
            }
            return entries;
        });
        when(courseTrainingApprovalSpi.resolveInstructorRate(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(rates.get(invocation.<UUID>getArgument(1))));
    }

    private static InstructorMatchProfile profile(UUID uuid, boolean verified, Map<UUID, ProficiencyLevel> skills,
                                                  SearchGeoPoint point) {
        return new InstructorMatchProfile(uuid, "Instructor " + uuid.toString().substring(0, 4), "Nairobi", verified,
                skills, 3, 4.5, 4, 4.3, point);
    }

    private static ClassMarketplaceJobRequiredSkill requiredSkill(UUID jobUuid, UUID skillUuid, ProficiencyLevel min,
                                                                  boolean mandatory) {
        ClassMarketplaceJobRequiredSkill row = new ClassMarketplaceJobRequiredSkill();
        row.setJobUuid(jobUuid);
        row.setSkillUuid(skillUuid);
        row.setMinProficiency(min);
        row.setIsMandatory(mandatory);
        return row;
    }

    private static SkillSummary skill(UUID uuid, String name) {
        return new SkillSummary(uuid, name, name.toLowerCase(), null, List.of(), true);
    }

    private static ClassMarketplaceJobSessionTemplate saturdayTemplate(UUID jobUuid) {
        ClassMarketplaceJobSessionTemplate template = new ClassMarketplaceJobSessionTemplate();
        template.setUuid(UUID.randomUUID());
        template.setJobUuid(jobUuid);
        template.setStartTime(LocalDateTime.of(2026, 5, 2, 9, 0));
        template.setEndTime(LocalDateTime.of(2026, 5, 2, 12, 0));
        template.setTimezone("Africa/Nairobi");
        template.setRecurrenceType("WEEKLY");
        template.setIntervalValue(1);
        template.setDaysOfWeek("SATURDAY");
        template.setOccurrenceCount(6);
        template.setConflictResolution(ConflictResolutionStrategy.FAIL.name());
        return template;
    }

    private static ScheduledInstanceDTO instance(LocalDateTime start, LocalDateTime end) {
        return new ScheduledInstanceDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), start, end, "UTC",
                "Existing session", "ONLINE", null, null, null, 25, SchedulingStatus.SCHEDULED, null, null, null,
                null, null);
    }
}
