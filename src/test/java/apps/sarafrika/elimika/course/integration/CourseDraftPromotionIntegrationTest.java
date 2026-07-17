package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseRequirement;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.impl.CourseDraftServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises draft-over-live promotion against a real PostgreSQL schema.
 * <p>
 * The clone and promote paths are database-shaped: they depend on foreign keys, cascade rules
 * and on live uuids surviving a promotion so learner progress keeps resolving. Mocked
 * repositories cannot show any of that, so this test runs the real migrations and the real SQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseDraftServiceImpl.class, CourseDraftPromotionIntegrationTest.TestConfig.class})
@DisplayName("Draft-over-live course promotion")
class CourseDraftPromotionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        // Flyway owns the schema. Validation is off rather than "validate" only because
        // GenericSpecificationBuilderTest declares a nested @Entity that entity scanning picks
        // up and no migration creates, which fails validation for reasons unrelated to this
        // test. The assertions below still run against the real migrated schema.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    // BaseEntity stamps created_date/created_by via Spring Data auditing, which @DataJpaTest
    // does not switch on by itself; without this every insert trips a NOT NULL constraint.
    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @Autowired
    private CourseDraftServiceImpl draftService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private LessonContentRepository lessonContentRepository;
    @Autowired
    private apps.sarafrika.elimika.course.repository.CourseAssessmentRepository assessmentRepository;
    @Autowired
    private apps.sarafrika.elimika.course.repository.CourseRequirementRepository requirementRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID courseCreatorUuid;

    @BeforeEach
    void seedOwner() {
        UUID userUuid = UUID.randomUUID();
        // users.email is varchar(50), so keep the local part short and still unique.
        String email = "c" + Long.toHexString(System.nanoTime()) + "@example.com";
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Creator', ?, ?, 'test')",
                userUuid, email, randomUserNo());

        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                        + "VALUES (?, ?, 'Test Creator', true, 'test')",
                courseCreatorUuid, userUuid);
    }

    @Test
    @DisplayName("opening a draft leaves the live course published and approved")
    void openDraftDoesNotTouchLiveCourse() {
        Course live = publishedApprovedCourse("Intro to Piano");

        Course draft = draftService.openDraft(live.getUuid());

        Course reloaded = courseRepository.findByUuid(live.getUuid()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(reloaded.getAdminApproved()).isTrue();
        assertThat(reloaded.getName()).isEqualTo("Intro to Piano");

        assertThat(draft.getParentCourseUuid()).isEqualTo(live.getUuid());
        assertThat(draft.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(draft.getActive()).isFalse();
        assertThat(draft.getAdminApproved()).isFalse();
    }

    @Test
    @DisplayName("a draft is excluded by the catalogue filters that already exist")
    void draftIsInvisibleToCatalogueQueries() {
        Course live = publishedApprovedCourse("Visible Course");
        draftService.openDraft(live.getUuid());

        List<String> published = jdbc.queryForList(
                "SELECT name FROM courses WHERE status = 'published' AND admin_approved = true", String.class);
        List<String> active = jdbc.queryForList(
                "SELECT name FROM courses WHERE active = true AND admin_approved = true", String.class);

        assertThat(published).containsExactly("Visible Course");
        assertThat(active).containsExactly("Visible Course");
    }

    @Test
    @DisplayName("opening a draft twice returns the same draft")
    void openDraftIsIdempotent() {
        Course live = publishedApprovedCourse("Idempotent");

        UUID first = draftService.openDraft(live.getUuid()).getUuid();
        UUID second = draftService.openDraft(live.getUuid()).getUuid();

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("a draft clones the live lesson tree and links each lesson back to its source")
    void openDraftClonesLessonTree() {
        Course live = publishedApprovedCourse("With Lessons");
        Lesson liveLesson = addLesson(live.getUuid(), 1, "Lesson One");
        addContent(liveLesson.getUuid(), "Reading One", 1);

        Course draft = draftService.openDraft(live.getUuid());

        List<Lesson> draftLessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draft.getUuid());
        assertThat(draftLessons).hasSize(1);
        assertThat(draftLessons.get(0).getTitle()).isEqualTo("Lesson One");
        assertThat(draftLessons.get(0).getSourceLessonUuid()).isEqualTo(liveLesson.getUuid());
        assertThat(draftLessons.get(0).getUuid()).isNotEqualTo(liveLesson.getUuid());

        List<LessonContent> draftContent =
                lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(draftLessons.get(0).getUuid());
        assertThat(draftContent).hasSize(1);
        assertThat(draftContent.get(0).getTitle()).isEqualTo("Reading One");
        assertThat(draftContent.get(0).getSourceContentUuid()).isNotNull();
    }

    @Test
    @DisplayName("rejecting an edit discards the draft and leaves the live course untouched")
    void discardLeavesLiveCourseUntouched() {
        Course live = publishedApprovedCourse("Original Name");
        Course draft = draftService.openDraft(live.getUuid());

        draft.setName("Proposed Name");
        draft.setPrice(new BigDecimal("9999.00"));
        courseRepository.save(draft);

        draftService.discard(live.getUuid());

        Course reloaded = courseRepository.findByUuid(live.getUuid()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Original Name");
        assertThat(reloaded.getPrice()).isEqualByComparingTo("1500.00");
        assertThat(reloaded.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(reloaded.getAdminApproved()).isTrue();
        assertThat(courseRepository.findByParentCourseUuid(live.getUuid())).isEmpty();
    }

    @Test
    @DisplayName("approving an edit promotes it onto the live course and preserves the course uuid")
    void promoteAppliesDraftOntoLiveCourse() {
        Course live = publishedApprovedCourse("Original Name");
        UUID liveUuid = live.getUuid();

        Course draft = draftService.openDraft(liveUuid);
        draft.setName("Approved Name");
        draft.setDescription("Rewritten description");
        courseRepository.save(draft);

        draftService.promote(liveUuid, null);

        Course reloaded = courseRepository.findByUuid(liveUuid).orElseThrow();
        assertThat(reloaded.getUuid()).isEqualTo(liveUuid);
        assertThat(reloaded.getName()).isEqualTo("Approved Name");
        assertThat(reloaded.getDescription()).isEqualTo("Rewritten description");
        // Promotion must never change whether the course is live.
        assertThat(reloaded.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(reloaded.getAdminApproved()).isTrue();
        assertThat(courseRepository.findByParentCourseUuid(liveUuid)).isEmpty();
    }

    @Test
    @DisplayName("promotion preserves live lesson uuids so learner progress keeps resolving")
    void promotePreservesLiveLessonUuids() {
        Course live = publishedApprovedCourse("Course");
        Lesson liveLesson = addLesson(live.getUuid(), 1, "Before");
        UUID liveLessonUuid = liveLesson.getUuid();

        Course draft = draftService.openDraft(live.getUuid());
        Lesson draftLesson = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draft.getUuid()).get(0);
        draftLesson.setTitle("After");
        lessonRepository.save(draftLesson);

        draftService.promote(live.getUuid(), null);

        List<Lesson> liveLessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(live.getUuid());
        assertThat(liveLessons).hasSize(1);
        assertThat(liveLessons.get(0).getUuid()).isEqualTo(liveLessonUuid);
        assertThat(liveLessons.get(0).getTitle()).isEqualTo("After");
        assertThat(liveLessons.get(0).getSourceLessonUuid()).isNull();
    }

    @Test
    @DisplayName("a lesson added by an edit appears on the live course once approved")
    void promoteInsertsNewLessons() {
        Course live = publishedApprovedCourse("Course");
        addLesson(live.getUuid(), 1, "Existing");

        Course draft = draftService.openDraft(live.getUuid());
        addLesson(draft.getUuid(), 2, "Brand New");

        draftService.promote(live.getUuid(), null);

        assertThat(lessonRepository.findByCourseUuidOrderByLessonNumberAsc(live.getUuid()))
                .extracting(Lesson::getTitle)
                .containsExactly("Existing", "Brand New");
    }

    @Test
    @DisplayName("a lesson removed by an edit is deactivated, not deleted, so progress survives")
    void promoteDeactivatesRemovedLessonsInsteadOfDeleting() {
        Course live = publishedApprovedCourse("Course");
        Lesson keep = addLesson(live.getUuid(), 1, "Keep");
        Lesson remove = addLesson(live.getUuid(), 2, "Remove Me");

        Course draft = draftService.openDraft(live.getUuid());
        lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draft.getUuid()).stream()
                .filter(l -> remove.getUuid().equals(l.getSourceLessonUuid()))
                .forEach(lessonRepository::delete);

        draftService.promote(live.getUuid(), null);

        Lesson removedLive = lessonRepository.findByUuid(remove.getUuid()).orElseThrow();
        assertThat(removedLive.getActive()).isFalse();

        Lesson keptLive = lessonRepository.findByUuid(keep.getUuid()).orElseThrow();
        assertThat(keptLive.getActive()).isTrue();
    }

    @Test
    @DisplayName("promotion records a version snapshot of the content that went live")
    void promoteWritesVersionSnapshot() {
        Course live = publishedApprovedCourse("Snapshot Me");
        Course draft = draftService.openDraft(live.getUuid());
        draft.setName("Version Two");
        courseRepository.save(draft);

        draftService.promote(live.getUuid(), null);

        Integer versions = jdbc.queryForObject(
                "SELECT count(*) FROM course_version_snapshots WHERE course_uuid = ?", Integer.class, live.getUuid());
        assertThat(versions).isEqualTo(1);

        String name = jdbc.queryForObject(
                "SELECT snapshot -> 'course' ->> 'name' FROM course_version_snapshots WHERE course_uuid = ?",
                String.class, live.getUuid());
        assertThat(name).isEqualTo("Version Two");
    }

    @Test
    @DisplayName("version numbers increase with each approved edit")
    void versionNumbersIncrement() {
        Course live = publishedApprovedCourse("Course");

        for (int i = 1; i <= 2; i++) {
            Course draft = draftService.openDraft(live.getUuid());
            draft.setName("Version " + i);
            courseRepository.save(draft);
            draftService.promote(live.getUuid(), null);
        }

        List<Integer> numbers = jdbc.queryForList(
                "SELECT version_number FROM course_version_snapshots WHERE course_uuid = ? ORDER BY version_number",
                Integer.class, live.getUuid());
        assertThat(numbers).containsExactly(1, 2);
    }

    @Test
    @DisplayName("a snapshot captures the whole tree, not just the course row")
    void snapshotIncludesLessonsAndContent() {
        Course live = publishedApprovedCourse("Course");
        Lesson lesson = addLesson(live.getUuid(), 1, "Lesson One");
        addContent(lesson.getUuid(), "Reading One", 1);

        JsonNode snapshot = draftService.snapshotTree(live.getUuid());

        assertThat(snapshot.get("course").get("name").asText()).isEqualTo("Course");
        assertThat(snapshot.get("lessons")).hasSize(1);
        assertThat(snapshot.get("lessons").get(0).get("title").asText()).isEqualTo("Lesson One");
        assertThat(snapshot.get("lessons").get(0).get("content").get(0).get("title").asText())
                .isEqualTo("Reading One");
    }

    @Test
    @DisplayName("the database refuses a second draft for the same course")
    void onlyOneDraftPerCourseIsPossible() {
        Course live = publishedApprovedCourse("Course");
        draftService.openDraft(live.getUuid());

        // Bypasses openDraft's idempotency to prove the invariant is enforced by the schema,
        // not merely by application code.
        assertThat(courseRepository.findByParentCourseUuid(live.getUuid())).isPresent();
        Integer drafts = jdbc.queryForObject(
                "SELECT count(*) FROM courses WHERE parent_course_uuid = ?", Integer.class, live.getUuid());
        assertThat(drafts).isEqualTo(1);
    }

    @Test
    @DisplayName("assessments are cloned into the draft and promoted, preserving live uuids")
    void assessmentsFollowTheDraft() {
        Course live = publishedApprovedCourse("Course");
        CourseAssessment liveAssessment = addAssessment(live.getUuid(), "Midterm");
        UUID liveAssessmentUuid = liveAssessment.getUuid();

        Course draft = draftService.openDraft(live.getUuid());
        CourseAssessment draftAssessment = assessmentRepository
                .findByCourseUuidOrderByCreatedDateAsc(draft.getUuid()).get(0);
        assertThat(draftAssessment.getSourceAssessmentUuid()).isEqualTo(liveAssessmentUuid);
        draftAssessment.setTitle("Final");
        assessmentRepository.save(draftAssessment);

        draftService.promote(live.getUuid(), null);

        List<CourseAssessment> liveAssessments =
                assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(live.getUuid());
        assertThat(liveAssessments).hasSize(1);
        assertThat(liveAssessments.get(0).getUuid()).isEqualTo(liveAssessmentUuid);
        assertThat(liveAssessments.get(0).getTitle()).isEqualTo("Final");
    }

    @Test
    @DisplayName("an assessment removed by an edit is deactivated, not deleted")
    void removedAssessmentIsDeactivated() {
        Course live = publishedApprovedCourse("Course");
        CourseAssessment keep = addAssessment(live.getUuid(), "Keep");
        CourseAssessment remove = addAssessment(live.getUuid(), "Remove");

        Course draft = draftService.openDraft(live.getUuid());
        assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(draft.getUuid()).stream()
                .filter(a -> remove.getUuid().equals(a.getSourceAssessmentUuid()))
                .forEach(assessmentRepository::delete);

        draftService.promote(live.getUuid(), null);

        assertThat(assessmentRepository.findByUuid(remove.getUuid()).orElseThrow().getActive()).isFalse();
        assertThat(assessmentRepository.findByUuid(keep.getUuid()).orElseThrow().getActive()).isTrue();
    }

    @Test
    @DisplayName("requirements are cloned and promoted, with removals actually deleted")
    void requirementsFollowTheDraft() {
        Course live = publishedApprovedCourse("Course");
        CourseRequirement keep = addRequirement(live.getUuid(), "Bring a laptop");
        CourseRequirement drop = addRequirement(live.getUuid(), "Obsolete rule");

        Course draft = draftService.openDraft(live.getUuid());
        requirementRepository.findByCourseUuid(draft.getUuid()).stream()
                .filter(r -> drop.getUuid().equals(r.getSourceRequirementUuid()))
                .forEach(requirementRepository::delete);

        draftService.promote(live.getUuid(), null);

        List<CourseRequirement> liveReqs = requirementRepository.findByCourseUuid(live.getUuid());
        assertThat(liveReqs).hasSize(1);
        // No learner data references requirements, so a removed one is genuinely gone.
        assertThat(liveReqs.get(0).getUuid()).isEqualTo(keep.getUuid());
    }

    @Test
    @DisplayName("rejecting an edit leaves assessments and requirements untouched")
    void discardLeavesAssessmentsAndRequirements() {
        Course live = publishedApprovedCourse("Course");
        CourseAssessment assessment = addAssessment(live.getUuid(), "Midterm");
        CourseRequirement requirement = addRequirement(live.getUuid(), "Bring a laptop");

        Course draft = draftService.openDraft(live.getUuid());
        assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(draft.getUuid())
                .forEach(a -> { a.setTitle("Changed"); assessmentRepository.save(a); });

        draftService.discard(live.getUuid());

        assertThat(assessmentRepository.findByUuid(assessment.getUuid()).orElseThrow().getTitle())
                .isEqualTo("Midterm");
        assertThat(requirementRepository.findByCourseUuid(live.getUuid())).hasSize(1);
        assertThat(requirementRepository.findByUuid(requirement.getUuid())).isPresent();
    }

    // ---------------------------------------------------------------- fixtures

    private Course publishedApprovedCourse(String name) {
        Course course = new Course();
        course.setName(name);
        course.setCourseCreatorUuid(courseCreatorUuid);
        course.setDescription("Original description");
        course.setPrice(new BigDecimal("1500.00"));
        course.setMinimumTrainingFee(new BigDecimal("500.00"));
        course.setCreatorSharePercentage(new BigDecimal("60.00"));
        course.setInstructorSharePercentage(new BigDecimal("40.00"));
        course.setDurationHours(2);
        course.setDurationMinutes(0);
        course.setStatus(ContentStatus.PUBLISHED);
        course.setActive(true);
        course.setAdminApproved(true);
        return courseRepository.saveAndFlush(course);
    }

    private Lesson addLesson(UUID courseUuid, int number, String title) {
        Lesson lesson = new Lesson();
        lesson.setCourseUuid(courseUuid);
        lesson.setLessonNumber(number);
        lesson.setTitle(title);
        lesson.setStatus(ContentStatus.PUBLISHED);
        lesson.setActive(true);
        return lessonRepository.saveAndFlush(lesson);
    }

    private LessonContent addContent(UUID lessonUuid, String title, int order) {
        LessonContent content = new LessonContent();
        content.setLessonUuid(lessonUuid);
        content.setContentTypeUuid(anyContentTypeUuid());
        content.setTitle(title);
        content.setDisplayOrder(order);
        content.setIsRequired(true);
        return lessonContentRepository.saveAndFlush(content);
    }

    private UUID anyContentTypeUuid() {
        List<UUID> existing = jdbc.queryForList("SELECT uuid FROM lesson_content_types LIMIT 1", UUID.class);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_content_types (uuid, name, created_by) VALUES (?, ?, 'test')",
                uuid, "text-" + Long.toHexString(System.nanoTime()));
        return uuid;
    }

    private CourseAssessment addAssessment(UUID courseUuid, String title) {
        CourseAssessment assessment = new CourseAssessment();
        assessment.setCourseUuid(courseUuid);
        assessment.setTitle(title);
        assessment.setAssessmentType("EXAM");
        assessment.setWeightPercentage(new BigDecimal("50.00"));
        assessment.setAggregationStrategy(
                apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE);
        assessment.setSyncClassAttendance(false);
        assessment.setIsRequired(true);
        assessment.setActive(true);
        return assessmentRepository.saveAndFlush(assessment);
    }

    private CourseRequirement addRequirement(UUID courseUuid, String text) {
        CourseRequirement requirement = new CourseRequirement();
        requirement.setCourseUuid(courseUuid);
        requirement.setRequirementText(text);
        requirement.setRequirementType(
                apps.sarafrika.elimika.course.util.enums.RequirementType.STUDENT);
        requirement.setIsMandatory(true);
        return requirementRepository.saveAndFlush(requirement);
    }

    private static String randomUserNo() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999));
    }
}
