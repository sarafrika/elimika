package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseEditDiffDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.model.CourseVersionSnapshot;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.repository.AssignmentAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseVersionSnapshotRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.repository.LessonPracticeActivityRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.service.CourseDraftService;
import apps.sarafrika.elimika.course.util.CourseRevenueShareValidator;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Draft-over-live editing for published courses.
 * <p>
 * Cloning a course produces a shadow course row plus a copy of every authoring row beneath
 * it. Learner data (progress, attempts, submissions) and class scheduling rows are never
 * cloned — they stay bound to the live tree, which is why promotion updates live rows in
 * place rather than swapping them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseDraftServiceImpl implements CourseDraftService {

    private final CourseRepository courseRepository;
    private final CourseCategoryMappingRepository mappingRepository;
    private final LessonRepository lessonRepository;
    private final LessonContentRepository lessonContentRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final LessonPracticeActivityRepository practiceActivityRepository;
    private final CourseVersionSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    private static final String COURSE_NOT_FOUND = "Course not found with UUID: %s";
    private static final String DRAFT_NOT_FOUND = "No open draft edit for course: %s";

    // ---------------------------------------------------------------- open / find

    @Override
    public Course openDraft(UUID liveCourseUuid) {
        Course live = findCourse(liveCourseUuid);

        Optional<Course> existing = courseRepository.findByParentCourseUuid(liveCourseUuid);
        if (existing.isPresent()) {
            return existing.get();
        }

        Course draft = new Course();
        copyCourseFields(live, draft);
        draft.setCourseCreatorUuid(live.getCourseCreatorUuid());
        draft.setParentCourseUuid(live.getUuid());
        // A draft is DRAFT/inactive/unapproved, which is exactly what the existing catalogue
        // filters exclude — no new conditions are needed to hide it from learners.
        draft.setStatus(ContentStatus.DRAFT);
        draft.setActive(false);
        draft.setAdminApproved(false);
        draft = courseRepository.save(draft);

        cloneCategories(live.getUuid(), draft.getUuid());
        cloneLessonTree(live.getUuid(), draft.getUuid());

        log.info("Opened draft {} for live course {}", draft.getUuid(), liveCourseUuid);
        return draft;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findDraft(UUID liveCourseUuid) {
        return courseRepository.findByParentCourseUuid(liveCourseUuid);
    }

    // ---------------------------------------------------------------- promote

    @Override
    public void promote(UUID liveCourseUuid, UUID pendingEditUuid) {
        Course live = findCourse(liveCourseUuid);
        Course draft = courseRepository.findByParentCourseUuid(liveCourseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DRAFT_NOT_FOUND, liveCourseUuid)));

        copyCourseFields(draft, live);
        CourseRevenueShareValidator.validate(live);
        courseRepository.save(live);

        promoteCategories(draft.getUuid(), live.getUuid());
        promoteLessons(draft.getUuid(), live.getUuid());

        // Snapshot the resulting live tree before the draft goes away, so the version
        // history records what actually went live rather than what was proposed.
        writeSnapshot(live.getUuid(), pendingEditUuid);

        courseRepository.delete(draft);
        log.info("Promoted draft {} onto live course {}", draft.getUuid(), liveCourseUuid);
    }

    @Override
    public void discard(UUID liveCourseUuid) {
        courseRepository.findByParentCourseUuid(liveCourseUuid).ifPresent(draft -> {
            courseRepository.delete(draft);
            log.info("Discarded draft {} for live course {}; live course untouched", draft.getUuid(), liveCourseUuid);
        });
    }

    // ---------------------------------------------------------------- cloning

    private void cloneCategories(UUID liveCourseUuid, UUID draftCourseUuid) {
        for (CourseCategoryMapping mapping : mappingRepository.findByCourseUuid(liveCourseUuid)) {
            CourseCategoryMapping copy = new CourseCategoryMapping();
            copy.setCourseUuid(draftCourseUuid);
            copy.setCategoryUuid(mapping.getCategoryUuid());
            mappingRepository.save(copy);
        }
    }

    private void cloneLessonTree(UUID liveCourseUuid, UUID draftCourseUuid) {
        for (Lesson liveLesson : lessonRepository.findByCourseUuidOrderByLessonNumberAsc(liveCourseUuid)) {
            Lesson draftLesson = new Lesson();
            copyLessonFields(liveLesson, draftLesson);
            draftLesson.setCourseUuid(draftCourseUuid);
            draftLesson.setSourceLessonUuid(liveLesson.getUuid());
            draftLesson = lessonRepository.save(draftLesson);

            cloneLessonContent(liveLesson.getUuid(), draftLesson.getUuid());
            cloneQuizzes(liveLesson.getUuid(), draftLesson.getUuid());
            cloneAssignments(liveLesson.getUuid(), draftLesson.getUuid());
            clonePracticeActivities(liveLesson.getUuid(), draftLesson.getUuid());
        }
    }

    private void cloneLessonContent(UUID liveLessonUuid, UUID draftLessonUuid) {
        for (LessonContent liveContent : lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(liveLessonUuid)) {
            LessonContent copy = new LessonContent();
            copyContentFields(liveContent, copy);
            copy.setLessonUuid(draftLessonUuid);
            copy.setSourceContentUuid(liveContent.getUuid());
            lessonContentRepository.save(copy);
        }
    }

    private void cloneQuizzes(UUID liveLessonUuid, UUID draftLessonUuid) {
        for (Quiz liveQuiz : quizRepository.findByLessonUuid(liveLessonUuid)) {
            Quiz draftQuiz = new Quiz();
            copyQuizFields(liveQuiz, draftQuiz);
            draftQuiz.setLessonUuid(draftLessonUuid);
            draftQuiz.setSourceQuizUuid(liveQuiz.getUuid());
            draftQuiz = quizRepository.save(draftQuiz);

            for (QuizQuestion liveQuestion : quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(liveQuiz.getUuid())) {
                QuizQuestion draftQuestion = new QuizQuestion();
                copyQuestionFields(liveQuestion, draftQuestion);
                draftQuestion.setQuizUuid(draftQuiz.getUuid());
                draftQuestion.setSourceQuestionUuid(liveQuestion.getUuid());
                draftQuestion = quizQuestionRepository.save(draftQuestion);

                for (QuizQuestionOption liveOption :
                        quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(liveQuestion.getUuid())) {
                    QuizQuestionOption draftOption = new QuizQuestionOption();
                    copyOptionFields(liveOption, draftOption);
                    draftOption.setQuestionUuid(draftQuestion.getUuid());
                    draftOption.setSourceOptionUuid(liveOption.getUuid());
                    quizQuestionOptionRepository.save(draftOption);
                }
            }
        }
    }

    private void cloneAssignments(UUID liveLessonUuid, UUID draftLessonUuid) {
        for (Assignment liveAssignment : assignmentRepository.findByLessonUuid(liveLessonUuid)) {
            Assignment draftAssignment = new Assignment();
            copyAssignmentFields(liveAssignment, draftAssignment);
            draftAssignment.setLessonUuid(draftLessonUuid);
            draftAssignment.setSourceAssignmentUuid(liveAssignment.getUuid());
            draftAssignment = assignmentRepository.save(draftAssignment);

            // Attachments carry no learner references, so they are copied wholesale and
            // replaced wholesale on promotion.
            for (AssignmentAttachment liveAttachment :
                    assignmentAttachmentRepository.findByAssignmentUuid(liveAssignment.getUuid())) {
                AssignmentAttachment copy = new AssignmentAttachment();
                copyAttachmentFields(liveAttachment, copy);
                copy.setAssignmentUuid(draftAssignment.getUuid());
                assignmentAttachmentRepository.save(copy);
            }
        }
    }

    private void clonePracticeActivities(UUID liveLessonUuid, UUID draftLessonUuid) {
        for (LessonPracticeActivity liveActivity :
                practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(liveLessonUuid)) {
            LessonPracticeActivity copy = new LessonPracticeActivity();
            copyPracticeActivityFields(liveActivity, copy);
            copy.setLessonUuid(draftLessonUuid);
            practiceActivityRepository.save(copy);
        }
    }

    // ---------------------------------------------------------------- promotion

    private void promoteCategories(UUID draftCourseUuid, UUID liveCourseUuid) {
        Set<UUID> desired = mappingRepository.findByCourseUuid(draftCourseUuid).stream()
                .map(CourseCategoryMapping::getCategoryUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CourseCategoryMapping> current = mappingRepository.findByCourseUuid(liveCourseUuid);
        Set<UUID> currentUuids = current.stream()
                .map(CourseCategoryMapping::getCategoryUuid)
                .collect(Collectors.toSet());

        // Category mappings carry no learner data, so they can be reconciled by delete/insert.
        current.stream()
                .filter(m -> !desired.contains(m.getCategoryUuid()))
                .forEach(mappingRepository::delete);

        desired.stream()
                .filter(categoryUuid -> !currentUuids.contains(categoryUuid))
                .forEach(categoryUuid -> {
                    CourseCategoryMapping mapping = new CourseCategoryMapping();
                    mapping.setCourseUuid(liveCourseUuid);
                    mapping.setCategoryUuid(categoryUuid);
                    mappingRepository.save(mapping);
                });
    }

    /**
     * Reconciles the live lesson tree against the draft. Never deletes: a lesson the edit
     * removed is deactivated instead, because lesson_progress and content_progress reference
     * these rows with RESTRICT and learners keep their completion history.
     */
    private void promoteLessons(UUID draftCourseUuid, UUID liveCourseUuid) {
        List<Lesson> draftLessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draftCourseUuid);
        Map<UUID, Lesson> liveLessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(liveCourseUuid).stream()
                .collect(Collectors.toMap(Lesson::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        Set<UUID> retained = new LinkedHashSet<>();

        for (Lesson draftLesson : draftLessons) {
            Lesson target = draftLesson.getSourceLessonUuid() == null
                    ? null
                    : liveLessons.get(draftLesson.getSourceLessonUuid());

            if (target == null) {
                target = new Lesson();
                target.setCourseUuid(liveCourseUuid);
            }
            copyLessonFields(draftLesson, target);
            target.setCourseUuid(liveCourseUuid);
            target.setSourceLessonUuid(null);
            target = lessonRepository.save(target);
            retained.add(target.getUuid());

            promoteContent(draftLesson.getUuid(), target.getUuid());
            promoteQuizzes(draftLesson.getUuid(), target.getUuid());
            promoteAssignments(draftLesson.getUuid(), target.getUuid());
            promotePracticeActivities(draftLesson.getUuid(), target.getUuid());
        }

        deactivateRemoved(liveLessons.values(), retained);
    }

    private void deactivateRemoved(Iterable<Lesson> liveLessons, Set<UUID> retained) {
        for (Lesson liveLesson : liveLessons) {
            if (!retained.contains(liveLesson.getUuid()) && !Boolean.FALSE.equals(liveLesson.getActive())) {
                liveLesson.setActive(false);
                lessonRepository.save(liveLesson);
                log.debug("Deactivated lesson {} removed by an approved edit", liveLesson.getUuid());
            }
        }
    }

    private void promoteContent(UUID draftLessonUuid, UUID liveLessonUuid) {
        List<LessonContent> draftContent = lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(draftLessonUuid);
        Map<UUID, LessonContent> liveContent =
                lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(liveLessonUuid).stream()
                        .collect(Collectors.toMap(LessonContent::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        Set<UUID> retained = new LinkedHashSet<>();
        for (LessonContent draft : draftContent) {
            LessonContent target = draft.getSourceContentUuid() == null
                    ? null
                    : liveContent.get(draft.getSourceContentUuid());
            if (target == null) {
                target = new LessonContent();
            }
            copyContentFields(draft, target);
            target.setLessonUuid(liveLessonUuid);
            target.setSourceContentUuid(null);
            target = lessonContentRepository.save(target);
            retained.add(target.getUuid());
        }

        // content_progress references lesson_contents with RESTRICT, so removed content is
        // marked not-required and moved out of the way rather than deleted.
        liveContent.values().stream()
                .filter(c -> !retained.contains(c.getUuid()))
                .forEach(c -> {
                    c.setIsRequired(false);
                    c.setDisplayOrder(Integer.MAX_VALUE);
                    lessonContentRepository.save(c);
                });
    }

    private void promoteQuizzes(UUID draftLessonUuid, UUID liveLessonUuid) {
        List<Quiz> draftQuizzes = quizRepository.findByLessonUuid(draftLessonUuid);
        Map<UUID, Quiz> liveQuizzes = quizRepository.findByLessonUuid(liveLessonUuid).stream()
                .collect(Collectors.toMap(Quiz::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        Set<UUID> retained = new LinkedHashSet<>();
        for (Quiz draft : draftQuizzes) {
            Quiz target = draft.getSourceQuizUuid() == null ? null : liveQuizzes.get(draft.getSourceQuizUuid());
            if (target == null) {
                target = new Quiz();
            }
            copyQuizFields(draft, target);
            target.setLessonUuid(liveLessonUuid);
            target.setSourceQuizUuid(null);
            target = quizRepository.save(target);
            retained.add(target.getUuid());

            promoteQuestions(draft.getUuid(), target.getUuid());
        }

        liveQuizzes.values().stream()
                .filter(q -> !retained.contains(q.getUuid()) && !Boolean.FALSE.equals(q.getActive()))
                .forEach(q -> {
                    q.setActive(false);
                    quizRepository.save(q);
                });
    }

    private void promoteQuestions(UUID draftQuizUuid, UUID liveQuizUuid) {
        List<QuizQuestion> draftQuestions = quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(draftQuizUuid);
        Map<UUID, QuizQuestion> liveQuestions = quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(liveQuizUuid).stream()
                .collect(Collectors.toMap(QuizQuestion::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        Set<UUID> retained = new LinkedHashSet<>();
        for (QuizQuestion draft : draftQuestions) {
            QuizQuestion target = draft.getSourceQuestionUuid() == null
                    ? null
                    : liveQuestions.get(draft.getSourceQuestionUuid());
            if (target == null) {
                target = new QuizQuestion();
            }
            copyQuestionFields(draft, target);
            target.setQuizUuid(liveQuizUuid);
            target.setSourceQuestionUuid(null);
            target = quizQuestionRepository.save(target);
            retained.add(target.getUuid());

            promoteOptions(draft.getUuid(), target.getUuid());
        }

        // quiz_responses reference questions, so a removed question is pushed to the end of
        // the order rather than deleted.
        liveQuestions.values().stream()
                .filter(q -> !retained.contains(q.getUuid()))
                .forEach(q -> {
                    q.setDisplayOrder(Integer.MAX_VALUE);
                    quizQuestionRepository.save(q);
                });
    }

    private void promoteOptions(UUID draftQuestionUuid, UUID liveQuestionUuid) {
        List<QuizQuestionOption> draftOptions =
                quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(draftQuestionUuid);
        Map<UUID, QuizQuestionOption> liveOptions =
                quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(liveQuestionUuid).stream()
                        .collect(Collectors.toMap(QuizQuestionOption::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        for (QuizQuestionOption draft : draftOptions) {
            QuizQuestionOption target = draft.getSourceOptionUuid() == null
                    ? null
                    : liveOptions.get(draft.getSourceOptionUuid());
            if (target == null) {
                target = new QuizQuestionOption();
            }
            copyOptionFields(draft, target);
            target.setQuestionUuid(liveQuestionUuid);
            target.setSourceOptionUuid(null);
            quizQuestionOptionRepository.save(target);
        }
    }

    private void promoteAssignments(UUID draftLessonUuid, UUID liveLessonUuid) {
        List<Assignment> draftAssignments = assignmentRepository.findByLessonUuid(draftLessonUuid);
        Map<UUID, Assignment> liveAssignments = assignmentRepository.findByLessonUuid(liveLessonUuid).stream()
                .collect(Collectors.toMap(Assignment::getUuid, Function.identity(), (a, b) -> a, HashMap::new));

        Set<UUID> retained = new LinkedHashSet<>();
        for (Assignment draft : draftAssignments) {
            Assignment target = draft.getSourceAssignmentUuid() == null
                    ? null
                    : liveAssignments.get(draft.getSourceAssignmentUuid());
            if (target == null) {
                target = new Assignment();
            }
            copyAssignmentFields(draft, target);
            target.setLessonUuid(liveLessonUuid);
            target.setSourceAssignmentUuid(null);
            target = assignmentRepository.save(target);
            retained.add(target.getUuid());

            replaceAttachments(draft.getUuid(), target.getUuid());
        }

        // assignment_submissions reference assignments, so a removed assignment is
        // unpublished rather than deleted.
        liveAssignments.values().stream()
                .filter(a -> !retained.contains(a.getUuid()) && !Boolean.FALSE.equals(a.getIsPublished()))
                .forEach(a -> {
                    a.setIsPublished(false);
                    assignmentRepository.save(a);
                });
    }

    private void replaceAttachments(UUID draftAssignmentUuid, UUID liveAssignmentUuid) {
        assignmentAttachmentRepository.findByAssignmentUuid(liveAssignmentUuid)
                .forEach(assignmentAttachmentRepository::delete);
        for (AssignmentAttachment draft : assignmentAttachmentRepository.findByAssignmentUuid(draftAssignmentUuid)) {
            AssignmentAttachment copy = new AssignmentAttachment();
            copyAttachmentFields(draft, copy);
            copy.setAssignmentUuid(liveAssignmentUuid);
            assignmentAttachmentRepository.save(copy);
        }
    }

    private void promotePracticeActivities(UUID draftLessonUuid, UUID liveLessonUuid) {
        practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(liveLessonUuid)
                .forEach(practiceActivityRepository::delete);
        for (LessonPracticeActivity draft : practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(draftLessonUuid)) {
            LessonPracticeActivity copy = new LessonPracticeActivity();
            copyPracticeActivityFields(draft, copy);
            copy.setLessonUuid(liveLessonUuid);
            practiceActivityRepository.save(copy);
        }
    }

    // ---------------------------------------------------------------- snapshot

    private void writeSnapshot(UUID courseUuid, UUID pendingEditUuid) {
        int nextVersion = snapshotRepository.findTopByCourseUuidOrderByVersionNumberDesc(courseUuid)
                .map(s -> s.getVersionNumber() + 1)
                .orElse(1);

        CourseVersionSnapshot snapshot = new CourseVersionSnapshot();
        snapshot.setCourseUuid(courseUuid);
        snapshot.setVersionNumber(nextVersion);
        snapshot.setSnapshot(snapshotTree(courseUuid));
        snapshot.setPendingEditUuid(pendingEditUuid);
        snapshotRepository.save(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public JsonNode snapshotTree(UUID courseUuid) {
        Course course = findCourse(courseUuid);

        ObjectNode root = objectMapper.createObjectNode();
        root.set("course", courseNode(course));

        ArrayNode categories = root.putArray("category_uuids");
        mappingRepository.findByCourseUuid(courseUuid).forEach(m -> categories.add(m.getCategoryUuid().toString()));

        ArrayNode lessons = root.putArray("lessons");
        for (Lesson lesson : lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid)) {
            lessons.add(lessonNode(lesson));
        }
        return root;
    }

    private ObjectNode courseNode(Course course) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uuid", str(course.getUuid()));
        node.put("name", course.getName());
        node.put("description", course.getDescription());
        node.put("objectives", course.getObjectives());
        node.put("prerequisites", course.getPrerequisites());
        node.put("difficulty_uuid", str(course.getDifficultyUuid()));
        node.put("duration_hours", course.getDurationHours());
        node.put("duration_minutes", course.getDurationMinutes());
        node.put("class_limit", course.getClassLimit());
        node.put("price", course.getPrice() == null ? null : course.getPrice().toPlainString());
        node.put("minimum_training_fee",
                course.getMinimumTrainingFee() == null ? null : course.getMinimumTrainingFee().toPlainString());
        node.put("creator_share_percentage",
                course.getCreatorSharePercentage() == null ? null : course.getCreatorSharePercentage().toPlainString());
        node.put("instructor_share_percentage",
                course.getInstructorSharePercentage() == null ? null : course.getInstructorSharePercentage().toPlainString());
        node.put("revenue_share_notes", course.getRevenueShareNotes());
        node.put("age_lower_limit", course.getAgeLowerLimit());
        node.put("age_upper_limit", course.getAgeUpperLimit());
        // Storage keys, not resolved URLs — a snapshot must survive a change of host.
        node.put("thumbnail_url", course.getThumbnailUrl());
        node.put("banner_url", course.getBannerUrl());
        node.put("intro_video_url", course.getIntroVideoUrl());
        return node;
    }

    private ObjectNode lessonNode(Lesson lesson) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uuid", str(lesson.getUuid()));
        node.put("lesson_number", lesson.getLessonNumber());
        node.put("title", lesson.getTitle());
        node.put("description", lesson.getDescription());
        node.put("learning_objectives", lesson.getLearningObjectives());
        node.put("active", lesson.getActive());

        ArrayNode content = node.putArray("content");
        for (LessonContent c : lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(lesson.getUuid())) {
            ObjectNode cn = content.addObject();
            cn.put("uuid", str(c.getUuid()));
            cn.put("content_type_uuid", str(c.getContentTypeUuid()));
            cn.put("title", c.getTitle());
            cn.put("description", c.getDescription());
            cn.put("content_text", c.getContentText());
            cn.put("file_url", c.getFileUrl());
            cn.put("display_order", c.getDisplayOrder());
            cn.put("is_required", c.getIsRequired());
        }

        ArrayNode quizzes = node.putArray("quizzes");
        for (Quiz q : quizRepository.findByLessonUuid(lesson.getUuid())) {
            ObjectNode qn = quizzes.addObject();
            qn.put("uuid", str(q.getUuid()));
            qn.put("title", q.getTitle());
            qn.put("description", q.getDescription());
            qn.put("active", q.getActive());
            ArrayNode questions = qn.putArray("questions");
            for (QuizQuestion question : quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(q.getUuid())) {
                ObjectNode qq = questions.addObject();
                qq.put("uuid", str(question.getUuid()));
                qq.put("question_text", question.getQuestionText());
                qq.put("display_order", question.getDisplayOrder());
            }
        }

        ArrayNode assignments = node.putArray("assignments");
        for (Assignment a : assignmentRepository.findByLessonUuid(lesson.getUuid())) {
            ObjectNode an = assignments.addObject();
            an.put("uuid", str(a.getUuid()));
            an.put("title", a.getTitle());
            an.put("description", a.getDescription());
            an.put("is_published", a.getIsPublished());
        }
        return node;
    }

    // ---------------------------------------------------------------- diff

    @Override
    @Transactional(readOnly = true)
    public CourseEditDiffDTO diff(UUID liveCourseUuid) {
        Course live = findCourse(liveCourseUuid);
        Course draft = courseRepository.findByParentCourseUuid(liveCourseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DRAFT_NOT_FOUND, liveCourseUuid)));

        List<CourseEditDiffDTO.FieldChange> changes = new ArrayList<>();
        ObjectNode liveNode = courseNode(live);
        ObjectNode draftNode = courseNode(draft);
        draftNode.fieldNames().forEachRemaining(field -> {
            if ("uuid".equals(field)) {
                return;
            }
            String liveValue = text(liveNode.get(field));
            String draftValue = text(draftNode.get(field));
            if (!Objects.equals(liveValue, draftValue)) {
                changes.add(new CourseEditDiffDTO.FieldChange(field, liveValue, draftValue));
            }
        });

        List<Lesson> draftLessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draft.getUuid());
        Set<UUID> liveLessonUuids = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(liveCourseUuid).stream()
                .map(Lesson::getUuid)
                .collect(Collectors.toSet());

        int added = (int) draftLessons.stream().filter(l -> l.getSourceLessonUuid() == null).count();
        Set<UUID> keptSources = draftLessons.stream()
                .map(Lesson::getSourceLessonUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int removed = (int) liveLessonUuids.stream().filter(u -> !keptSources.contains(u)).count();
        int modified = countModifiedLessons(draftLessons);

        return new CourseEditDiffDTO(liveCourseUuid, draft.getUuid(), changes, added, removed, modified);
    }

    private int countModifiedLessons(List<Lesson> draftLessons) {
        int modified = 0;
        for (Lesson draftLesson : draftLessons) {
            if (draftLesson.getSourceLessonUuid() == null) {
                continue;
            }
            Optional<Lesson> live = lessonRepository.findByUuid(draftLessonSource(draftLesson));
            if (live.isPresent() && !sameLessonContent(live.get(), draftLesson)) {
                modified++;
            }
        }
        return modified;
    }

    private UUID draftLessonSource(Lesson lesson) {
        return lesson.getSourceLessonUuid();
    }

    private boolean sameLessonContent(Lesson live, Lesson draft) {
        return Objects.equals(live.getTitle(), draft.getTitle())
                && Objects.equals(live.getDescription(), draft.getDescription())
                && Objects.equals(live.getLearningObjectives(), draft.getLearningObjectives())
                && Objects.equals(live.getLessonNumber(), draft.getLessonNumber());
    }

    // ---------------------------------------------------------------- authoring routing

    @Override
    public UUID resolveEditableCourseUuid(UUID courseUuid) {
        Course course = findCourse(courseUuid);
        if (!CoursePendingEditServiceImpl.requiresReview(course)) {
            return courseUuid;
        }
        return openDraft(courseUuid).getUuid();
    }

    @Override
    public UUID resolveEditableLessonUuid(UUID courseUuid, UUID lessonUuid) {
        Course course = findCourse(courseUuid);
        Lesson lesson = lessonRepository.findByUuid(lessonUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with UUID: " + lessonUuid));

        if (!CoursePendingEditServiceImpl.requiresReview(course)) {
            assertBelongsTo(lesson, courseUuid);
            return lessonUuid;
        }

        UUID draftCourseUuid = openDraft(courseUuid).getUuid();

        // The creator may name either the live lesson or the draft's copy of it.
        if (draftCourseUuid.equals(lesson.getCourseUuid())) {
            return lessonUuid;
        }
        assertBelongsTo(lesson, courseUuid);

        return lessonRepository.findByCourseUuidOrderByLessonNumberAsc(draftCourseUuid).stream()
                .filter(l -> lessonUuid.equals(l.getSourceLessonUuid()))
                .findFirst()
                .map(Lesson::getUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lesson " + lessonUuid + " has no draft counterpart for course " + courseUuid));
    }

    private void assertBelongsTo(Lesson lesson, UUID courseUuid) {
        if (!courseUuid.equals(lesson.getCourseUuid())) {
            throw new ResourceNotFoundException(
                    "Lesson " + lesson.getUuid() + " does not belong to course " + courseUuid);
        }
    }

    @Override
    public UUID resolveEditableContentUuid(UUID courseUuid, UUID lessonUuid, UUID contentUuid) {
        UUID targetLessonUuid = resolveEditableLessonUuid(courseUuid, lessonUuid);

        LessonContent content = lessonContentRepository.findByUuid(contentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson content not found with UUID: " + contentUuid));

        if (targetLessonUuid.equals(content.getLessonUuid())) {
            return contentUuid;
        }
        if (!lessonUuid.equals(content.getLessonUuid())) {
            throw new ResourceNotFoundException(
                    "Lesson content " + contentUuid + " does not belong to lesson " + lessonUuid);
        }

        return lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(targetLessonUuid).stream()
                .filter(c -> contentUuid.equals(c.getSourceContentUuid()))
                .findFirst()
                .map(LessonContent::getUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lesson content " + contentUuid + " has no draft counterpart for course " + courseUuid));
    }

    // ---------------------------------------------------------------- field copies

    /**
     * Copies the authored content of a course. Deliberately excludes identity, ownership and
     * lifecycle ({@code status}, {@code active}, {@code adminApproved}, {@code parentCourseUuid}),
     * so promoting a draft can never change whether the live course is published or approved.
     */
    private void copyCourseFields(Course from, Course to) {
        to.setName(from.getName());
        to.setDifficultyUuid(from.getDifficultyUuid());
        to.setDescription(from.getDescription());
        to.setObjectives(from.getObjectives());
        to.setPrerequisites(from.getPrerequisites());
        to.setDurationHours(from.getDurationHours());
        to.setDurationMinutes(from.getDurationMinutes());
        to.setClassLimit(from.getClassLimit());
        to.setPrice(from.getPrice());
        to.setMinimumTrainingFee(from.getMinimumTrainingFee());
        to.setCreatorSharePercentage(from.getCreatorSharePercentage());
        to.setInstructorSharePercentage(from.getInstructorSharePercentage());
        to.setRevenueShareNotes(from.getRevenueShareNotes());
        to.setAgeLowerLimit(from.getAgeLowerLimit());
        to.setAgeUpperLimit(from.getAgeUpperLimit());
        to.setThumbnailUrl(from.getThumbnailUrl());
        to.setIntroVideoUrl(from.getIntroVideoUrl());
        to.setBannerUrl(from.getBannerUrl());
    }

    private void copyLessonFields(Lesson from, Lesson to) {
        to.setLessonNumber(from.getLessonNumber());
        to.setTitle(from.getTitle());
        to.setDescription(from.getDescription());
        to.setLearningObjectives(from.getLearningObjectives());
        to.setStatus(from.getStatus());
        to.setActive(from.getActive());
    }

    private void copyContentFields(LessonContent from, LessonContent to) {
        to.setContentTypeUuid(from.getContentTypeUuid());
        to.setTitle(from.getTitle());
        to.setDescription(from.getDescription());
        to.setContentText(from.getContentText());
        to.setFileUrl(from.getFileUrl());
        to.setFileSizeBytes(from.getFileSizeBytes());
        to.setMimeType(from.getMimeType());
        to.setDisplayOrder(from.getDisplayOrder());
        to.setIsRequired(from.getIsRequired());
    }

    private void copyQuizFields(Quiz from, Quiz to) {
        to.setTitle(from.getTitle());
        to.setDescription(from.getDescription());
        to.setInstructions(from.getInstructions());
        to.setTimeLimitMinutes(from.getTimeLimitMinutes());
        to.setAttemptsAllowed(from.getAttemptsAllowed());
        to.setPassingScore(from.getPassingScore());
        to.setRubricUuid(from.getRubricUuid());
        to.setStatus(from.getStatus());
        to.setActive(from.getActive());
        to.setScope(from.getScope());
        to.setClassDefinitionUuid(from.getClassDefinitionUuid());
    }

    private void copyQuestionFields(QuizQuestion from, QuizQuestion to) {
        to.setQuestionText(from.getQuestionText());
        to.setQuestionType(from.getQuestionType());
        to.setPoints(from.getPoints());
        to.setDisplayOrder(from.getDisplayOrder());
    }

    private void copyOptionFields(QuizQuestionOption from, QuizQuestionOption to) {
        to.setOptionText(from.getOptionText());
        to.setIsCorrect(from.getIsCorrect());
        to.setDisplayOrder(from.getDisplayOrder());
    }

    private void copyAssignmentFields(Assignment from, Assignment to) {
        to.setTitle(from.getTitle());
        to.setDescription(from.getDescription());
        to.setInstructions(from.getInstructions());
        to.setDueDate(from.getDueDate());
        to.setMaxPoints(from.getMaxPoints());
        to.setRubricUuid(from.getRubricUuid());
        to.setSubmissionTypes(from.getSubmissionTypes());
        to.setIsPublished(from.getIsPublished());
        to.setScope(from.getScope());
        to.setClassDefinitionUuid(from.getClassDefinitionUuid());
    }

    private void copyAttachmentFields(AssignmentAttachment from, AssignmentAttachment to) {
        to.setOriginalFilename(from.getOriginalFilename());
        to.setStoredFilename(from.getStoredFilename());
        to.setFileUrl(from.getFileUrl());
        to.setFileSizeBytes(from.getFileSizeBytes());
        to.setMimeType(from.getMimeType());
    }

    private void copyPracticeActivityFields(LessonPracticeActivity from, LessonPracticeActivity to) {
        to.setTitle(from.getTitle());
        to.setInstructions(from.getInstructions());
        to.setActivityType(from.getActivityType());
        to.setGrouping(from.getGrouping());
        to.setEstimatedMinutes(from.getEstimatedMinutes());
        to.setMaterials(from.getMaterials());
        to.setExpectedOutput(from.getExpectedOutput());
        to.setDisplayOrder(from.getDisplayOrder());
        to.setStatus(from.getStatus());
        to.setActive(from.getActive());
    }

    // ---------------------------------------------------------------- helpers

    private Course findCourse(UUID uuid) {
        return courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND, uuid)));
    }

    private static String str(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
