package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.repository.AssignmentAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseRequirementRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.repository.CourseVersionSnapshotRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.repository.LessonPracticeActivityRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The tripwire for {@code snapshotTree()}.
 * <p>
 * A course version is only worth keeping if it records the whole course. The serialiser drifted
 * from the schema once already — v1 snapshots carried no quiz answer options, no assignment
 * attachments and no practice activities, and dropped the scoring columns from quizzes and
 * questions, so a "version" recorded roughly half a course and could not be restored from.
 * <p>
 * Rather than assert a fixed list of keys, which would drift the same way, this walks each entity's
 * declared fields by reflection and demands every one of them either appear in the snapshot or be
 * named in {@link #NOT_CONTENT}. Add a column to any of these tables and this test fails until you
 * decide, explicitly, which of the two it is.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseSnapshotCoverageTest {

    /**
     * Fields that are deliberately absent from a snapshot, by entity.
     * <p>
     * Three kinds only: the foreign key back to the parent, which the JSON nesting already
     * expresses; the {@code source_*_uuid} draft-promotion links, which describe an edit in flight
     * and mean nothing in a finished version; and {@code class_definition_uuid}, which belongs to
     * scheduling rather than to course content.
     */
    private static final Set<String> NOT_CONTENT = Set.of(
            "Lesson.courseUuid", "Lesson.sourceLessonUuid",
            "LessonContent.lessonUuid", "LessonContent.sourceContentUuid",
            "Quiz.lessonUuid", "Quiz.classDefinitionUuid", "Quiz.sourceQuizUuid",
            "QuizQuestion.quizUuid", "QuizQuestion.sourceQuestionUuid",
            "QuizQuestionOption.questionUuid", "QuizQuestionOption.sourceOptionUuid",
            "Assignment.lessonUuid", "Assignment.classDefinitionUuid", "Assignment.sourceAssignmentUuid",
            "AssignmentAttachment.assignmentUuid",
            "LessonPracticeActivity.lessonUuid"
    );

    @Mock private CourseRepository courseRepository;
    @Mock private CourseCategoryMappingRepository mappingRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonContentRepository lessonContentRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private QuizQuestionOptionRepository quizQuestionOptionRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentAttachmentRepository assignmentAttachmentRepository;
    @Mock private LessonPracticeActivityRepository practiceActivityRepository;
    @Mock private CourseAssessmentRepository assessmentRepository;
    @Mock private CourseAssessmentLineItemRepository lineItemRepository;
    @Mock private CourseRequirementRepository requirementRepository;
    @Mock private CourseTrainingRequirementRepository trainingRequirementRepository;
    @Mock private CourseVersionSnapshotRepository snapshotRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private CourseDraftServiceImpl service;

    private final UUID courseUuid = UUID.randomUUID();
    private JsonNode tree;

    @BeforeEach
    void seed() {
        Course course = new Course();
        course.setUuid(courseUuid);
        course.setName("Piano Foundations");

        Lesson lesson = new Lesson();
        lesson.setUuid(UUID.randomUUID());
        lesson.setCourseUuid(courseUuid);
        lesson.setLessonNumber(1);
        lesson.setTitle("Sitting at the instrument");

        LessonContent content = new LessonContent();
        content.setUuid(UUID.randomUUID());
        content.setLessonUuid(lesson.getUuid());
        content.setTitle("Posture");

        Quiz quiz = new Quiz();
        quiz.setUuid(UUID.randomUUID());
        quiz.setLessonUuid(lesson.getUuid());
        quiz.setTitle("Check yourself");
        quiz.setPassingScore(new BigDecimal("60.00"));

        QuizQuestion question = new QuizQuestion();
        question.setUuid(UUID.randomUUID());
        question.setQuizUuid(quiz.getUuid());
        question.setQuestionText("Where do the wrists sit?");
        question.setPoints(new BigDecimal("2.00"));

        QuizQuestionOption option = new QuizQuestionOption();
        option.setUuid(UUID.randomUUID());
        option.setQuestionUuid(question.getUuid());
        option.setOptionText("Level with the keys");
        option.setIsCorrect(true);

        Assignment assignment = new Assignment();
        assignment.setUuid(UUID.randomUUID());
        assignment.setLessonUuid(lesson.getUuid());
        assignment.setTitle("Practice log, week one");
        assignment.setDueDate(LocalDateTime.now());

        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setUuid(UUID.randomUUID());
        attachment.setAssignmentUuid(assignment.getUuid());
        attachment.setOriginalFilename("practice-log.pdf");

        LessonPracticeActivity activity = new LessonPracticeActivity();
        activity.setUuid(UUID.randomUUID());
        activity.setLessonUuid(lesson.getUuid());
        activity.setTitle("Five minutes of scales");

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(mappingRepository.findByCourseUuid(any())).thenReturn(List.of());
        when(lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid)).thenReturn(List.of(lesson));
        when(lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(any())).thenReturn(List.of(content));
        when(quizRepository.findByLessonUuid(any())).thenReturn(List.of(quiz));
        when(quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(any())).thenReturn(List.of(question));
        when(quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(any())).thenReturn(List.of(option));
        when(assignmentRepository.findByLessonUuid(any())).thenReturn(List.of(assignment));
        when(assignmentAttachmentRepository.findByAssignmentUuid(any())).thenReturn(List.of(attachment));
        when(practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(any())).thenReturn(List.of(activity));
        when(assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(any())).thenReturn(List.of());
        when(requirementRepository.findByCourseUuid(any())).thenReturn(List.of());
        when(trainingRequirementRepository.findByCourseUuid(any())).thenReturn(List.of());

        tree = service.snapshotTree(courseUuid);
    }

    @Test
    @DisplayName("Every content column of every table under a course reaches the snapshot")
    void everyContentColumnIsSerialised() {
        JsonNode lesson = tree.path("lessons").path(0);
        JsonNode quiz = lesson.path("quizzes").path(0);
        JsonNode question = quiz.path("questions").path(0);
        JsonNode assignment = lesson.path("assignments").path(0);

        Set<String> missing = new TreeSet<>();
        collectMissing(Lesson.class, lesson, missing);
        collectMissing(LessonContent.class, lesson.path("content").path(0), missing);
        collectMissing(Quiz.class, quiz, missing);
        collectMissing(QuizQuestion.class, question, missing);
        collectMissing(QuizQuestionOption.class, question.path("options").path(0), missing);
        collectMissing(Assignment.class, assignment, missing);
        collectMissing(AssignmentAttachment.class, assignment.path("attachments").path(0), missing);
        collectMissing(LessonPracticeActivity.class, lesson.path("practice_activities").path(0), missing);

        assertThat(missing)
                .withFailMessage("""
                        These columns exist on the entity but never reach a course version snapshot:
                          %s
                        Add each to snapshotTree() in CourseDraftServiceImpl, or, if it genuinely is
                        not course content, name it in CourseSnapshotCoverageTest.NOT_CONTENT with a
                        reason.""", String.join("\n  ", missing))
                .isEmpty();
    }

    @Test
    @DisplayName("The snapshot stamps the schema version it was written under")
    void snapshotIsVersioned() {
        assertThat(tree.path("schema_version").asInt())
                .isEqualTo(CourseDraftServiceImpl.SNAPSHOT_SCHEMA_VERSION);
    }

    @Test
    @DisplayName("A quiz question carries its answer key, not just its wording")
    void answerKeyIsRecorded() {
        JsonNode option = tree.path("lessons").path(0)
                .path("quizzes").path(0)
                .path("questions").path(0)
                .path("options").path(0);

        assertThat(option.path("option_text").asText()).isEqualTo("Level with the keys");
        assertThat(option.path("is_correct").asBoolean()).isTrue();
    }

    private static void collectMissing(Class<?> entity, JsonNode node, Set<String> missing) {
        for (Field field : entity.getDeclaredFields()) {
            if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String qualified = entity.getSimpleName() + "." + field.getName();
            if (NOT_CONTENT.contains(qualified)) {
                continue;
            }
            if (!node.has(snakeCase(field.getName()))) {
                missing.add(qualified);
            }
        }
    }

    private static String snakeCase(String camel) {
        StringBuilder out = new StringBuilder(camel.length() + 4);
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                out.append('_').append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
