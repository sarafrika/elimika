package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO;
import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO.ModelScore;
import apps.sarafrika.elimika.course.dto.RecommendationReasonDTO;
import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateRetriever;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore;
import apps.sarafrika.elimika.course.internal.recommend.CourseFeatureRefresher;
import apps.sarafrika.elimika.course.internal.recommend.LearnerContextLoader;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationEvaluator;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationScorer;
import apps.sarafrika.elimika.course.service.impl.CourseRecommendationServiceImpl;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliationLookup;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Course recommendations "rules-v2" against the real schema: the learner profile SQL, the relational
 * fallback and the search path's SQL hydration, exclusions, access, impressions, statement counts and the
 * offline evaluator. The other modules' answers (age, skill goals, affiliations, guardian scope) come from
 * mocks of their shared SPIs; {@code LearnerProfileLookupServiceImplTest} covers the student side.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseCandidateStore.class, CourseCandidateRetriever.class, LearnerContextLoader.class,
        RecommendationEvaluator.class, CourseRecommendationServiceImpl.class, CourseFeatureRefresher.class,
        CourseRecommendationIntegrationTest.TestConfig.class})
@DisplayName("Course recommendations v2")
class CourseRecommendationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    /** Statements prepared on the data source, for the N+1 check. */
    static final AtomicInteger STATEMENTS = new AtomicInteger();

    static class TestConfig {
        @Bean
        MinorLearnerLookupService minorLearnerLookupService() {
            return (studentUuids, asOf) -> Set.of();
        }

        @Bean
        static BeanPostProcessor countingDataSource() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return bean instanceof DataSource dataSource && !(bean instanceof CountingDataSource)
                            ? new CountingDataSource(dataSource) : bean;
                }
            };
        }
    }

    static class CountingDataSource extends DelegatingDataSource {
        CountingDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return counting(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return counting(super.getConnection(username, password));
        }

        private static Connection counting(Connection connection) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("prepareStatement") || method.getName().equals("createStatement")) {
                            STATEMENTS.incrementAndGet();
                        }
                        try {
                            return method.invoke(connection, args);
                        } catch (java.lang.reflect.InvocationTargetException ex) {
                            throw ex.getCause();
                        }
                    });
        }
    }

    @MockitoBean private DomainSecurityService domainSecurityService;
    @MockitoBean private LearnerProfileLookupService learnerProfileLookupService;
    @MockitoBean private LearnerAffiliationLookup learnerAffiliationLookup;
    @MockitoBean private DiscoveryTracker discoveryTracker;
    @MockitoBean private OrganisationLookupService organisationLookupService;
    @MockitoBean private InstructorLookupService instructorLookupService;
    @MockitoBean private SearchGateway searchGateway;
    @MockitoBean private SearchAvailability searchAvailability;

    @Autowired private CourseRecommendationServiceImpl service;
    @Autowired private CourseFeatureRefresher refresher;
    @Autowired private JdbcTemplate jdbc;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private UUID creatorUuid;
    private UUID viewerUser;
    private UUID learner;
    private UUID programming;
    private UUID design;
    private UUID music;
    private UUID cooking;
    private Map<Integer, UUID> levels;

    @BeforeEach
    void seed() {
        creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Creator', 'test')",
                creatorUuid, user());
        String tag = Long.toHexString(System.nanoTime());
        programming = category("Programming " + tag);
        design = category("Design " + tag);
        music = category("Music " + tag);
        cooking = category("Cooking " + tag);
        levels = new HashMap<>();
        jdbc.query("SELECT uuid, level_order FROM course_difficulty_levels", rs -> {
            levels.put(rs.getInt("level_order"), rs.getObject("uuid", UUID.class));
        });

        viewerUser = user();
        learner = student();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(viewerUser);
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(learner);
        when(learnerAffiliationLookup.findAffiliations(any())).thenReturn(LearnerAffiliations.none());
        when(learnerProfileLookupService.findLearnerAge(eq(learner), any())).thenReturn(OptionalInt.of(12));
    }

    @Test
    @DisplayName("a student with only enrolments gets real recommendations with reasons; enrolled, drafts and "
            + "courses outside the age band never appear; impressions are recorded")
    void pureStudent() {
        UUID basics = course("Python Basics", programming, 1);
        UUID sketching = course("Sketching", design, 1);
        UUID intermediate = course("Python Intermediate", programming, 2);
        UUID adultsOnly = courseWith("Python for Adults", programming, 2, 18, null, "published", true);
        UUID smallChildren = courseWith("Design for Tots", design, 1, null, 10, "published", true);
        UUID draft = courseWith("Python Draft", programming, 2, null, null, "draft", true);
        UUID unapproved = courseWith("Python Unapproved", programming, 2, null, null, "published", false);
        UUID harmony = course("Harmony", music, 1);
        enrol(learner, basics, "completed", 100);
        enrol(learner, sketching, "active", 50);
        refresher.refresh(now);

        List<RecommendedCourseDTO> result = service.recommendForCaller(null, null, null, 6);

        assertThat(result).isNotEmpty();
        assertThat(result).extracting(RecommendedCourseDTO::courseUuid)
                .doesNotContain(basics, sketching, adultsOnly, smallChildren, draft, unapproved)
                .contains(intermediate, harmony);
        RecommendedCourseDTO first = result.getFirst();
        assertThat(first.courseUuid()).isEqualTo(intermediate);
        assertThat(first.reasons().getFirst())
                .isEqualTo(new RecommendationReasonDTO("NEXT_STEP", "Next step after Python Basics", basics));
        assertThat(first.reason()).isEqualTo("Next step after Python Basics");
        assertThat(result).allSatisfy(item -> {
            assertThat(item.reasons()).isNotEmpty();
            assertThat(item.modelVersion()).isEqualTo("rules-v2");
            assertThat(item.surface()).isEqualTo("for_you");
            assertThat(item.recommendationId()).isEqualTo(first.recommendationId());
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryImpression>> items = ArgumentCaptor.forClass(List.class);
        verify(discoveryTracker).recordImpressions(eq(viewerUser), eq("course_for_you"), eq(first.recommendationId()),
                eq("rules-v2"), items.capture());
        assertThat(items.getValue()).hasSize(result.size());
        assertThat(items.getValue().getFirst().itemUuid()).isEqualTo(intermediate);
        assertThat(items.getValue().getFirst().reasonCodes()).startsWith("NEXT_STEP");
        assertThat(items.getValue()).extracting(DiscoveryImpression::position).containsExactly(
                java.util.stream.IntStream.range(0, result.size()).boxed().toArray(Integer[]::new));
    }

    @Test
    @DisplayName("without a date of birth only courses with no age limit are recommended")
    void unknownAge() {
        when(learnerProfileLookupService.findLearnerAge(eq(learner), any())).thenReturn(OptionalInt.empty());
        UUID basics = course("Python Basics", programming, 1);
        UUID open = course("Python Intermediate", programming, 2);
        UUID adults = courseWith("Python for Adults", programming, 2, 18, null, "published", true);
        UUID teens = courseWith("Python for Teens", programming, 2, 13, 17, "published", true);
        enrol(learner, basics, "completed", 100);

        assertThat(service.recommendForCaller(null, null, "for_you", 6)).extracting(RecommendedCourseDTO::courseUuid)
                .contains(open).doesNotContain(adults, teens, basics);
    }

    @Test
    @DisplayName("co-enrolment: a pair shared by 5 learners gives 'Often taken after', never naming anyone")
    void coEnrolment() {
        UUID basics = course("Python Basics", programming, 1);
        UUID viz = course("Data Viz", design, 1);
        enrol(learner, basics, "completed", 100);
        for (int i = 0; i < 5; i++) {
            UUID other = student();
            enrol(other, basics, "completed", 100);
            enrol(other, viz, "active", 30);
        }
        refresher.refresh(now);

        RecommendedCourseDTO item = service.recommendForCaller(null, null, null, 6).stream()
                .filter(r -> r.courseUuid().equals(viz)).findFirst().orElseThrow();
        assertThat(item.reasons()).extracting(RecommendationReasonDTO::code).contains("CO_ENROLLED");
        assertThat(item.reasons()).filteredOn(r -> r.code().equals("CO_ENROLLED"))
                .containsExactly(new RecommendationReasonDTO("CO_ENROLLED", "Often taken after Python Basics", basics));
    }

    @Test
    @DisplayName("next_steps: a course whose mandatory prerequisite is in progress shows 'Complete X first' and stays off for_you")
    void nextSteps() {
        UUID sql = course("SQL 101", programming, 1);
        UUID tuning = course("SQL Tuning", programming, 2);
        jdbc.update("INSERT INTO course_prerequisites (course_uuid, prerequisite_course_uuid, is_mandatory, created_by) "
                + "VALUES (?, ?, true, 'test')", tuning, sql);
        enrol(learner, sql, "active", 40);

        List<RecommendedCourseDTO> next = service.recommendForCaller(null, null, "next_steps", 6);
        assertThat(next).extracting(RecommendedCourseDTO::courseUuid).containsExactly(tuning);
        assertThat(next.getFirst().reasons().getFirst())
                .isEqualTo(new RecommendationReasonDTO("PREREQUISITE_PENDING", "Complete SQL 101 first", sql));
        assertThat(next.getFirst().surface()).isEqualTo("next_steps");

        assertThat(service.recommendForCaller(null, null, "for_you", 6)).extracting(RecommendedCourseDTO::courseUuid)
                .doesNotContain(tuning);
    }

    @Test
    @DisplayName("diversity: at most 2 per category among the first 6, with one course from a category the learner has not tried")
    void diversityCaps() {
        UUID basics = course("Python Basics", programming, 1);
        UUID sketching = course("Sketching", design, 1);
        enrol(learner, basics, "completed", 100);
        enrol(learner, sketching, "completed", 100);
        for (int i = 0; i < 5; i++) {
            course("Programming " + i, programming, 1);
            course("Design " + i, design, 1);
        }
        course("Harmony", music, 1);
        course("Baking", cooking, 1);

        List<RecommendedCourseDTO> result = service.recommendForCaller(null, null, null, 6);
        assertThat(result).hasSize(6);
        Map<UUID, Integer> perCategory = new HashMap<>();
        for (RecommendedCourseDTO item : result) {
            jdbc.queryForList("SELECT category_uuid FROM course_category_mappings WHERE course_uuid = ?", UUID.class,
                    item.courseUuid()).forEach(c -> perCategory.merge(c, 1, Integer::sum));
        }
        assertThat(perCategory.values()).allMatch(count -> count <= RecommendationScorer.PER_CATEGORY_CAP);
        assertThat(perCategory.getOrDefault(music, 0) + perCategory.getOrDefault(cooking, 0)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("guardian with a FULL or ACADEMICS share reads a ward's list; other guardians and other users get 403")
    void guardianAccess() {
        UUID guardian = user();
        UUID ward = learner;
        UUID otherWard = student();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(guardian);
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);
        when(learnerProfileLookupService.guardianCanViewAcademics(guardian, ward)).thenReturn(true);
        when(learnerProfileLookupService.guardianCanViewAcademics(guardian, otherWard)).thenReturn(false);
        UUID basics = course("Python Basics", programming, 1);
        UUID intermediate = course("Python Intermediate", programming, 2);
        enrol(ward, basics, "completed", 100);

        List<RecommendedCourseDTO> result = service.recommendForCaller(null, ward, null, 6);
        assertThat(result).extracting(RecommendedCourseDTO::courseUuid).contains(intermediate).doesNotContain(basics);
        verify(discoveryTracker).recordImpressions(eq(guardian), eq("course_for_you"), any(), eq("rules-v2"), any());

        assertThatThrownBy(() -> service.recommendForCaller(null, otherWard, null, 6))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.recommendForCaller(UUID.randomUUID(), null, null, 6))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("similar works anonymously for a public course, records nothing, and 404s for a draft")
    void similarAnonymous() {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(null);
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);
        UUID basics = course("Python Basics", programming, 1);
        UUID sibling = course("Python Testing", programming, 1);
        UUID other = course("Harmony", music, 1);
        UUID draft = courseWith("Python Draft", programming, 1, null, null, "draft", true);

        List<RecommendedCourseDTO> similar = service.findSimilar(basics, 6);

        assertThat(similar).extracting(RecommendedCourseDTO::courseUuid).doesNotContain(basics, draft);
        assertThat(similar.getFirst().courseUuid()).isEqualTo(sibling);
        assertThat(similar.getFirst().reasons().getFirst().code()).isEqualTo("CATEGORY");
        assertThat(similar).extracting(RecommendedCourseDTO::surface).containsOnly("similar");
        assertThat(similar).extracting(RecommendedCourseDTO::courseUuid).contains(other);
        verify(discoveryTracker, never()).recordImpressions(any(), anyString(), any(), any(), any());

        assertThatThrownBy(() -> service.findSimilar(draft, 6)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("search path: one multi-search with the public + age-band scope, 'more like this' with LAST, "
            + "and SQL hydration drops stale hits")
    void searchPath() {
        UUID basics = course("Python Basics", programming, 1);
        UUID intermediate = course("Python Intermediate", programming, 2);
        UUID draft = courseWith("Python Draft", programming, 2, null, null, "draft", true);
        enrol(learner, basics, "completed", 100);
        when(searchAvailability.isReadEnabled("courses")).thenReturn(true);
        when(searchGateway.multiSearchPerIndex(any())).thenAnswer(invocation -> {
            List<SearchRequest> requests = invocation.getArgument(0);
            return requests.stream().map(r -> new SearchPage(List.of(
                    new SearchHit(intermediate, Map.of(), null, 0.9, null),
                    new SearchHit(draft, Map.of(), null, 0.8, null),
                    new SearchHit(basics, Map.of(), null, 0.7, null)), 3, 0, r.size(), Map.of())).toList();
        });

        List<RecommendedCourseDTO> result = service.recommendForCaller(null, null, null, 6);

        assertThat(result).extracting(RecommendedCourseDTO::courseUuid).containsExactly(intermediate);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SearchRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(searchGateway).multiSearchPerIndex(captor.capture());
        List<SearchRequest> requests = captor.getValue();
        assertThat(requests).allSatisfy(r -> {
            assertThat(r.index()).isEqualTo("courses");
            assertThat(r.scope().filter().toString()).contains("is_public").contains("age_lower_limit").contains("12");
            assertThat(r.filter().toString()).contains(basics.toString());
        });
        assertThat(requests).filteredOn(SearchRequest::hasText).singleElement().satisfies(r -> {
            assertThat(r.text()).isEqualTo("Python Basics");
            assertThat(r.matchingStrategy()).isEqualTo(SearchRequest.MatchingStrategy.LAST);
        });
        assertThat(requests).anySatisfy(r -> assertThat(r.filter()).isInstanceOf(SearchFilter.And.class));
    }

    @Test
    @DisplayName("no N+1: the statement count does not grow with the learner's enrolments or the catalogue")
    void statementCountIsFlat() {
        UUID small = student();
        UUID large = student();
        when(learnerProfileLookupService.findLearnerAge(any(), any())).thenReturn(OptionalInt.of(30));
        for (int i = 0; i < 2; i++) {
            enrol(small, course("Small " + i, programming, 1), "completed", 100);
        }
        for (int i = 0; i < 10; i++) {
            UUID c = course("Large " + i, i % 2 == 0 ? programming : design, 1 + i % 3);
            enrol(large, c, i % 3 == 0 ? "active" : "completed", 60);
        }
        for (int i = 0; i < 20; i++) {
            course("Catalogue " + i, i % 2 == 0 ? music : cooking, 1 + i % 3);
        }
        refresher.refresh(now);

        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(small);
        STATEMENTS.set(0);
        assertThat(service.recommendForCaller(null, null, null, 6)).isNotEmpty();
        int smallCount = STATEMENTS.get();

        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(large);
        STATEMENTS.set(0);
        assertThat(service.recommendForCaller(null, null, null, 20)).isNotEmpty();
        int largeCount = STATEMENTS.get();

        // Enrolments, co-enrolment neighbours, candidates (affiliation offers skipped: none).
        assertThat(smallCount).isBetween(1, 4);
        assertThat(largeCount).isEqualTo(smallCount);
    }

    @Test
    @DisplayName("offline evaluation: leave-last-out recall@6 and coverage; rules-v2 beats popularity and newest-first")
    void evaluation() {
        UUID basics = courseAt("Python Basics", programming, 1, 400);
        UUID intermediate = courseAt("Python Intermediate", programming, 2, 300);
        UUID crowd = courseAt("Crowd Pleaser", music, 1, 200);
        for (int i = 0; i < 8; i++) {
            courseAt("New " + i, cooking, 1, i);
        }
        for (int i = 0; i < 6; i++) {
            UUID s = student();
            enrolAt(s, basics, "completed", 100, 60);
            enrolAt(s, intermediate, "active", 10, 5);
        }
        for (int i = 0; i < 4; i++) {
            enrolAt(student(), crowd, "active", 10, 20);
        }

        RecommendationEvaluationDTO evaluation = service.evaluate();

        assertThat(evaluation.k()).isEqualTo(6);
        assertThat(evaluation.learnersEvaluated()).isEqualTo(6);
        Map<String, ModelScore> byModel = new HashMap<>();
        evaluation.models().forEach(m -> byModel.put(m.model(), m));
        assertThat(byModel).containsOnlyKeys("rules-v2", "popularity", "legacy-newest");
        assertThat(byModel.get("rules-v2").recallAtK()).isEqualTo(1.0);
        assertThat(byModel.get("rules-v2").recallAtK()).isGreaterThan(byModel.get("legacy-newest").recallAtK());
        assertThat(byModel.get("rules-v2").recallAtK()).isGreaterThanOrEqualTo(byModel.get("popularity").recallAtK());
        assertThat(byModel.values()).allSatisfy(m -> {
            assertThat(m.recallAtK()).isBetween(0.0, 1.0);
            assertThat(m.coverage()).isBetween(0.0, 1.0);
        });
    }

    // ----------------------------------------------------------------- plumbing

    private UUID user() {
        UUID uuid = UUID.randomUUID();
        String tag = Long.toHexString(System.nanoTime());
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, 'test')",
                uuid, String.format("%09d", Math.floorMod(uuid.hashCode(), 1_000_000_000)), "u" + tag + "@t.io");
        return uuid;
    }

    private UUID student() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Learner', 'test')",
                uuid, user());
        return uuid;
    }

    private UUID category(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private UUID course(String name, UUID category, int level) {
        return courseWith(name, category, level, null, null, "published", true);
    }

    private UUID courseAt(String name, UUID category, int level, int createdDaysAgo) {
        UUID uuid = course(name, category, level);
        jdbc.update("UPDATE courses SET created_date = ? WHERE uuid = ?",
                Timestamp.from(now.minus(createdDaysAgo, ChronoUnit.DAYS)), uuid);
        return uuid;
    }

    private UUID courseWith(String name, UUID category, int level, Integer ageLower, Integer ageUpper, String status,
                            boolean adminApproved) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, difficulty_uuid, "
                        + "age_lower_limit, age_upper_limit, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, name, creatorUuid, status, "published".equals(status), adminApproved, levels.get(level), ageLower, ageUpper);
        jdbc.update("INSERT INTO course_category_mappings (uuid, course_uuid, category_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", UUID.randomUUID(), uuid, category);
        return uuid;
    }

    private void enrol(UUID student, UUID course, String status, int progress) {
        enrolAt(student, course, status, progress, 10);
    }

    private void enrolAt(UUID student, UUID course, String status, int progress, int enrolledDaysAgo) {
        Timestamp enrolled = Timestamp.from(now.minus(enrolledDaysAgo, ChronoUnit.DAYS));
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, progress_percentage, "
                        + "enrollment_date, completion_date, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                UUID.randomUUID(), student, course, status, progress, enrolled,
                "completed".equals(status) ? Timestamp.from(now.minus(Math.max(0, enrolledDaysAgo - 1), ChronoUnit.DAYS)) : null);
    }
}
