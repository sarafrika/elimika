package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseRequirement;
import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import apps.sarafrika.elimika.course.model.CourseVersion;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseRequirementRepository;
import apps.sarafrika.elimika.course.repository.CourseRubricAssociationRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.repository.CourseVersionRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CoursePublishSnapshotService {

    private final CourseRepository courseRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final LessonRepository lessonRepository;
    private final LessonContentRepository lessonContentRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final CourseRequirementRepository courseRequirementRepository;
    private final CourseTrainingRequirementRepository courseTrainingRequirementRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseRubricAssociationRepository courseRubricAssociationRepository;
    private final AssessmentRubricRepository assessmentRubricRepository;
    private final RubricCriteriaRepository rubricCriteriaRepository;
    private final RubricScoringLevelRepository rubricScoringLevelRepository;
    private final RubricScoringRepository rubricScoringRepository;
    private final ObjectMapper objectMapper;

    public void capturePublishedSnapshot(UUID courseUuid) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course with ID " + courseUuid + " not found"));

        if (course.getStatus() != ContentStatus.PUBLISHED) {
            return;
        }

        String snapshotPayload = serializeSnapshot(buildSnapshotPayload(course));
        String snapshotHash = sha256(snapshotPayload);

        if (courseVersionRepository.existsByCourseUuidAndSnapshotHash(courseUuid, snapshotHash)) {
            return;
        }

        int nextVersionNumber = courseVersionRepository.findTopByCourseUuidOrderByVersionNumberDesc(courseUuid)
                .map(CourseVersion::getVersionNumber)
                .map(value -> value + 1)
                .orElse(1);

        CourseVersion courseVersion = new CourseVersion();
        courseVersion.setCourseUuid(courseUuid);
        courseVersion.setVersionNumber(nextVersionNumber);
        courseVersion.setSnapshotHash(snapshotHash);
        courseVersion.setSnapshotPayloadJson(snapshotPayload);
        courseVersion.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));

        courseVersionRepository.save(courseVersion);
    }

    private Map<String, Object> buildSnapshotPayload(Course course) {
        UUID courseUuid = course.getUuid();
        List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid);
        List<UUID> lessonUuids = lessons.stream().map(Lesson::getUuid).toList();

        List<Assignment> assignments = lessonUuids.isEmpty()
                ? List.of()
                : assignmentRepository.findByLessonUuidInOrderByCreatedDateAsc(lessonUuids);

        List<Quiz> quizzes = lessonUuids.isEmpty()
                ? List.of()
                : quizRepository.findByLessonUuidInOrderByCreatedDateAsc(lessonUuids);

        List<UUID> quizUuids = quizzes.stream().map(Quiz::getUuid).toList();
        List<QuizQuestion> questions = quizUuids.isEmpty()
                ? List.of()
                : quizUuids.stream()
                .flatMap(quizUuid -> quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid).stream())
                .toList();

        List<UUID> questionUuids = questions.stream().map(QuizQuestion::getUuid).toList();
        List<QuizQuestionOption> options = questionUuids.isEmpty()
                ? List.of()
                : quizQuestionOptionRepository.findByQuestionUuidInOrderByDisplayOrderAsc(questionUuids);

        List<CourseRubricAssociation> rubricAssociations = courseRubricAssociationRepository.findByCourseUuid(courseUuid)
                .stream()
                .sorted(Comparator.comparing(CourseRubricAssociation::getRubricUuid))
                .toList();

        List<UUID> rubricUuids = rubricAssociations.stream()
                .map(CourseRubricAssociation::getRubricUuid)
                .distinct()
                .sorted()
                .toList();

        List<AssessmentRubric> rubrics = rubricUuids.isEmpty()
                ? List.of()
                : assessmentRubricRepository.findByUuidIn(rubricUuids).stream()
                .sorted(Comparator.comparing(AssessmentRubric::getUuid))
                .toList();

        List<CourseRequirement> courseRequirements = sortBy(
                courseRequirementRepository.findByCourseUuid(courseUuid),
                CourseRequirement::getUuid
        );

        List<CourseTrainingRequirement> courseTrainingRequirements = sortBy(
                courseTrainingRequirementRepository.findByCourseUuid(courseUuid),
                CourseTrainingRequirement::getUuid
        );

        List<CourseAssessment> courseAssessments = sortBy(
                courseAssessmentRepository.findByCourseUuid(courseUuid),
                CourseAssessment::getUuid
        );

        Map<UUID, List<RubricCriteria>> rubricCriteriaByRubricUuid = new LinkedHashMap<>();
        Map<UUID, List<RubricScoringLevel>> rubricLevelsByRubricUuid = new LinkedHashMap<>();
        Map<UUID, List<RubricScoring>> rubricScoringByRubricUuid = new LinkedHashMap<>();

        for (UUID rubricUuid : rubricUuids) {
            List<RubricCriteria> criteria = rubricCriteriaRepository.findByRubricUuidOrderByDisplayOrderAsc(rubricUuid);
            List<RubricScoringLevel> levels = rubricScoringLevelRepository.findByRubricUuidOrderByLevelOrder(rubricUuid);
            List<RubricScoring> scoring = rubricScoringRepository.findByRubricUuid(rubricUuid).stream()
                    .sorted(
                            Comparator.comparing(RubricScoring::getCriteriaUuid)
                                    .thenComparing(RubricScoring::getRubricScoringLevelUuid, Comparator.nullsLast(UUID::compareTo))
                                    .thenComparing(RubricScoring::getUuid)
                    )
                    .toList();

            rubricCriteriaByRubricUuid.put(rubricUuid, criteria);
            rubricLevelsByRubricUuid.put(rubricUuid, levels);
            rubricScoringByRubricUuid.put(rubricUuid, scoring);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshot_schema_version", 1);
        payload.put("course", toMap(course));
        payload.put("lessons", toMapList(lessons));
        payload.put("lesson_contents", toMapList(resolveLessonContents(lessonUuids)));
        payload.put("assignments", toMapList(assignments));
        payload.put("quizzes", toMapList(quizzes));
        payload.put("quiz_questions", toMapList(questions));
        payload.put("quiz_question_options", toMapList(options));
        payload.put("course_requirements", toMapList(courseRequirements));
        payload.put("course_training_requirements", toMapList(courseTrainingRequirements));
        payload.put("course_assessments", toMapList(courseAssessments));
        payload.put("course_rubric_associations", toMapList(rubricAssociations));
        payload.put("rubrics", toMapList(rubrics));
        payload.put("rubric_criteria", toMapList(flatten(rubricCriteriaByRubricUuid)));
        payload.put("rubric_scoring_levels", toMapList(flatten(rubricLevelsByRubricUuid)));
        payload.put("rubric_scoring", toMapList(flatten(rubricScoringByRubricUuid)));
        return payload;
    }

    private List<LessonContent> resolveLessonContents(List<UUID> lessonUuids) {
        List<LessonContent> content = new ArrayList<>();
        for (UUID lessonUuid : lessonUuids) {
            content.addAll(lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(lessonUuid));
        }
        return content;
    }

    private <T> List<T> flatten(Map<UUID, List<T>> map) {
        return map.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private <T, K extends Comparable<K>> List<T> sortBy(List<T> items, Function<T, K> keyExtractor) {
        return items.stream().sorted(Comparator.comparing(keyExtractor)).toList();
    }

    private List<Map<String, Object>> toMapList(List<?> entities) {
        return entities.stream().map(this::toMap).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, LinkedHashMap.class);
    }

    private String serializeSnapshot(Map<String, Object> payload) {
        try {
            return objectMapper.copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize course publish snapshot", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
