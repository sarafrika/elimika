package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseRequirement;
import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRequirementRepository;
import apps.sarafrika.elimika.course.repository.CourseRubricAssociationRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishedCourseVersionTriggerService {

    private final CoursePublishSnapshotService coursePublishSnapshotService;
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
    private final RubricCriteriaRepository rubricCriteriaRepository;
    private final RubricScoringLevelRepository rubricScoringLevelRepository;
    private final RubricScoringRepository rubricScoringRepository;

    public void captureByCourseUuid(UUID courseUuid) {
        if (courseUuid == null) {
            return;
        }
        coursePublishSnapshotService.capturePublishedSnapshot(courseUuid);
    }

    public void captureByCourseUuids(Collection<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return;
        }
        courseUuids.stream().filter(uuid -> uuid != null).distinct().forEach(this::captureByCourseUuid);
    }

    public void captureByLessonUuid(UUID lessonUuid) {
        lessonRepository.findByUuid(lessonUuid).map(Lesson::getCourseUuid).ifPresent(this::captureByCourseUuid);
    }

    public void captureByLessonContentUuid(UUID lessonContentUuid) {
        lessonContentRepository.findByUuid(lessonContentUuid)
                .map(LessonContent::getLessonUuid)
                .ifPresent(this::captureByLessonUuid);
    }

    public void captureByAssignmentUuid(UUID assignmentUuid) {
        assignmentRepository.findByUuid(assignmentUuid)
                .map(Assignment::getLessonUuid)
                .ifPresent(this::captureByLessonUuid);
    }

    public void captureByQuizUuid(UUID quizUuid) {
        quizRepository.findByUuid(quizUuid).map(Quiz::getLessonUuid).ifPresent(this::captureByLessonUuid);
    }

    public void captureByQuizQuestionUuid(UUID questionUuid) {
        quizQuestionRepository.findByUuid(questionUuid)
                .map(QuizQuestion::getQuizUuid)
                .ifPresent(this::captureByQuizUuid);
    }

    public void captureByQuizQuestionOptionUuid(UUID optionUuid) {
        quizQuestionOptionRepository.findByUuid(optionUuid)
                .map(QuizQuestionOption::getQuestionUuid)
                .ifPresent(this::captureByQuizQuestionUuid);
    }

    public void captureByCourseRequirementUuid(UUID requirementUuid) {
        courseRequirementRepository.findByUuid(requirementUuid)
                .map(CourseRequirement::getCourseUuid)
                .ifPresent(this::captureByCourseUuid);
    }

    public void captureByCourseTrainingRequirementUuid(UUID requirementUuid) {
        courseTrainingRequirementRepository.findByUuid(requirementUuid)
                .map(CourseTrainingRequirement::getCourseUuid)
                .ifPresent(this::captureByCourseUuid);
    }

    public void captureByCourseAssessmentUuid(UUID assessmentUuid) {
        courseAssessmentRepository.findByUuid(assessmentUuid)
                .map(CourseAssessment::getCourseUuid)
                .ifPresent(this::captureByCourseUuid);
    }

    public void captureByCourseRubricAssociationUuid(UUID associationUuid) {
        courseRubricAssociationRepository.findByUuid(associationUuid)
                .map(CourseRubricAssociation::getCourseUuid)
                .ifPresent(this::captureByCourseUuid);
    }

    public void captureByRubricUuid(UUID rubricUuid) {
        captureByCourseUuids(
                courseRubricAssociationRepository.findByRubricUuid(rubricUuid)
                        .stream()
                        .map(CourseRubricAssociation::getCourseUuid)
                        .toList()
        );
    }

    public void captureByRubricCriteriaUuid(UUID criteriaUuid) {
        rubricCriteriaRepository.findByUuid(criteriaUuid)
                .map(RubricCriteria::getRubricUuid)
                .ifPresent(this::captureByRubricUuid);
    }

    public void captureByRubricScoringLevelUuid(UUID scoringLevelUuid) {
        rubricScoringLevelRepository.findByUuid(scoringLevelUuid)
                .map(RubricScoringLevel::getRubricUuid)
                .ifPresent(this::captureByRubricUuid);
    }

    public void captureByRubricScoringUuid(UUID scoringUuid) {
        rubricScoringRepository.findByUuid(scoringUuid)
                .map(RubricScoring::getCriteriaUuid)
                .ifPresent(this::captureByRubricCriteriaUuid);
    }
}
