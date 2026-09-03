package apps.sarafrika.elimika.shared.integration;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionCreateRequestDTO;
import apps.sarafrika.elimika.classes.factory.ClassDefinitionFactory;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.course.dto.CourseAssessmentDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.service.impl.CourseAssessmentServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the columns that are {@code NOT NULL DEFAULT x} in the schema but carry their default on
 * the Java side.
 * <p>
 * The distinction matters because nothing in this project uses {@code @DynamicInsert}: Hibernate
 * writes every mapped column on every insert, so a field left null is sent to PostgreSQL as an
 * explicit NULL and the column DEFAULT never runs. Two of these reached production and failed every
 * single request — {@code class_definitions.rate_basis} broke create-class and
 * {@code course_assessments.active} broke add-assessment, each returning nothing more useful than
 * "The operation cannot be completed due to a data constraint violation".
 * <p>
 * A mocked repository cannot show any of this. The insert statement Hibernate generates, and
 * PostgreSQL's reaction to it, only exist when the real entity model and the real migrations are put
 * in the same room. So every assertion here reads the value back out of the database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseAssessmentServiceImpl.class, GenericSpecificationBuilder.class,
        NotNullColumnDefaultsIntegrationTest.TestConfig.class})
@DisplayName("NOT NULL columns whose default lives on the entity")
class NotNullColumnDefaultsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }

        @Bean
        CourseGradeBookService courseGradeBookService() {
            return Mockito.mock(CourseGradeBookService.class);
        }
    }

    @Autowired
    private CourseAssessmentServiceImpl courseAssessmentService;
    @Autowired
    private CourseAssessmentRepository courseAssessmentRepository;
    @Autowired
    private CourseAssessmentLineItemRepository lineItemRepository;
    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizQuestionRepository quizQuestionRepository;
    @Autowired
    private QuizQuestionOptionRepository quizQuestionOptionRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private TrainingProgramRepository trainingProgramRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID courseCreatorUuid;

    @BeforeEach
    void seedCourseCreator() {
        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Creator', ?, ?, 'test')",
                creatorUser, "c" + Long.toHexString(System.nanoTime()) + "@example.com", randomUserNo());
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                        + "VALUES (?, ?, 'Test Creator', true, 'test')",
                courseCreatorUuid, creatorUser);
    }

    // --- the two failures observed in production -----------------------------------------------

    @Test
    @DisplayName("adding a course assessment persists active=true when the request omits it")
    void courseAssessmentDefaultsToActive() {
        Course course = persistCourse();

        // Exactly what the API binds from the frontend payload: no way to express `active` at all.
        CourseAssessmentDTO request = new CourseAssessmentDTO(
                null, course.getUuid(), "Attendance", "Attendance & Participation",
                "Student's participation in class discussions", new BigDecimal("10.00"),
                null, null, null, true, null, null, null, null);

        CourseAssessmentDTO created = courseAssessmentService.createCourseAssessment(course.getUuid(), request);

        assertThat(created.uuid()).isNotNull();
        Boolean persistedActive = jdbc.queryForObject(
                "SELECT active FROM course_assessments WHERE uuid = ?", Boolean.class, created.uuid());
        assertThat(persistedActive).isTrue();
    }

    @Test
    @DisplayName("creating a class persists rate_basis=PER_HOUR when the request omits it")
    void classDefinitionDefaultsToPerHour() {
        ClassDefinition saved = classDefinitionRepository.saveAndFlush(
                ClassDefinitionFactory.toEntity(createRequest(null).toClassDefinitionDTO()));

        assertThat(readRateBasis(saved.getUuid())).isEqualTo("PER_HOUR");
    }

    @Test
    @DisplayName("a class created with an explicit rate basis keeps it — the field is writable, not ignored")
    void classDefinitionHonoursExplicitRateBasis() {
        ClassDefinition saved = classDefinitionRepository.saveAndFlush(
                ClassDefinitionFactory.toEntity(createRequest(RateBasis.PER_SESSION).toClassDefinitionDTO()));

        // Before the fix this could not be expressed: rate_basis was absent from the create request
        // and hardcoded to null on the way through, so per-session pricing was unreachable.
        assertThat(readRateBasis(saved.getUuid())).isEqualTo("PER_SESSION");
        assertThat(saved.getRateBasis()).isEqualTo(RateBasis.PER_SESSION);
    }

    // --- the same bug shape, caught before anyone hit it ----------------------------------------

    @Test
    @DisplayName("a training program saved without a status lands as draft")
    void trainingProgramDefaultsToDraft() {
        TrainingProgram program = new TrainingProgram();
        program.setTitle("Piano Foundations");
        program.setCourseCreatorUuid(courseCreatorUuid);
        program.setTotalDurationHours(4);
        program.setTotalDurationMinutes(0);
        program.setActive(false);
        program.setAdminApproved(false);

        TrainingProgram saved = trainingProgramRepository.saveAndFlush(program);

        assertThat(jdbc.queryForObject("SELECT status FROM training_programs WHERE uuid = ?",
                String.class, saved.getUuid())).isEqualTo("draft");
    }

    @Test
    @DisplayName("a quiz question option saved without a display order lands at 1")
    void quizQuestionOptionDefaultsToFirstPosition() {
        Course course = persistCourse();
        Lesson lesson = persistLesson(course.getUuid());
        Quiz quiz = persistQuiz(lesson.getUuid());
        QuizQuestion question = persistQuestion(quiz.getUuid());

        QuizQuestionOption option = new QuizQuestionOption();
        option.setQuestionUuid(question.getUuid());
        option.setOptionText("Middle C");
        option.setIsCorrect(true);

        QuizQuestionOption saved = quizQuestionOptionRepository.saveAndFlush(option);

        assertThat(jdbc.queryForObject("SELECT display_order FROM quiz_question_options WHERE uuid = ?",
                Integer.class, saved.getUuid())).isEqualTo(1);
    }

    @Test
    @DisplayName("an assignment saved without max points lands at 100")
    void assignmentDefaultsToHundredPoints() {
        Course course = persistCourse();
        Lesson lesson = persistLesson(course.getUuid());

        Assignment assignment = new Assignment();
        assignment.setLessonUuid(lesson.getUuid());
        assignment.setTitle("Composition assignment");
        assignment.setIsPublished(false);

        Assignment saved = assignmentRepository.saveAndFlush(assignment);

        assertThat(jdbc.queryForObject("SELECT max_points FROM assignments WHERE uuid = ?",
                BigDecimal.class, saved.getUuid())).isEqualByComparingTo("100.00");
    }

    // --- fixtures -------------------------------------------------------------------------------

    private String readRateBasis(UUID classUuid) {
        return jdbc.queryForObject(
                "SELECT rate_basis FROM class_definitions WHERE uuid = ?", String.class, classUuid);
    }

    /** A create request the controller would accept, with only the rate basis varying. */
    private ClassDefinitionCreateRequestDTO createRequest(RateBasis rateBasis) {
        return new ClassDefinitionCreateRequestDTO(
                "Artificial Intelligence and Machine Learning", "Weekly cohort", null, null,
                UUID.randomUUID(), null, null, null, null,
                new BigDecimal("1750.00"), new BigDecimal("1750.00"), rateBasis,
                ClassVisibility.PUBLIC, SessionFormat.GROUP,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                null, null, null, null, null, null,
                LocationType.ONLINE, null, null, null, "https://meet.example.com/ai",
                5, true, true, List.of());
    }

    private Course persistCourse() {
        Course course = new Course();
        course.setName("Music Theory");
        course.setCourseCreatorUuid(courseCreatorUuid);
        course.setDescription("Course");
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

    private Lesson persistLesson(UUID courseUuid) {
        Lesson lesson = new Lesson();
        lesson.setCourseUuid(courseUuid);
        lesson.setLessonNumber(1);
        lesson.setTitle("Lesson 1");
        lesson.setStatus(ContentStatus.PUBLISHED);
        lesson.setActive(true);
        return lessonRepository.saveAndFlush(lesson);
    }

    private Quiz persistQuiz(UUID lessonUuid) {
        Quiz quiz = new Quiz();
        quiz.setLessonUuid(lessonUuid);
        quiz.setTitle("Chapter quiz");
        quiz.setStatus(ContentStatus.PUBLISHED);
        quiz.setActive(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private QuizQuestion persistQuestion(UUID quizUuid) {
        QuizQuestion question = new QuizQuestion();
        question.setQuizUuid(quizUuid);
        question.setQuestionText("Which note is middle C?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setPoints(new BigDecimal("1.00"));
        question.setDisplayOrder(1);
        return quizQuestionRepository.saveAndFlush(question);
    }

    private static String randomUserNo() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999));
    }
}
