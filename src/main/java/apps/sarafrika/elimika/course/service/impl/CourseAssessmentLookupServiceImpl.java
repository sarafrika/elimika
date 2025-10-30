package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.spi.CourseAssessmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAssessmentLookupServiceImpl implements CourseAssessmentLookupService {

    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;

    @Override
    public Optional<CourseAssignmentSummary> getAssignmentSummary(UUID assignmentUuid) {
        if (assignmentUuid == null) {
            return Optional.empty();
        }
        return assignmentRepository.findByUuid(assignmentUuid)
                .map(assignment -> new CourseAssignmentSummary(
                        assignment.getUuid(),
                        assignment.getLessonUuid(),
                        assignment.getTitle()
                ));
    }

    @Override
    public Optional<CourseQuizSummary> getQuizSummary(UUID quizUuid) {
        if (quizUuid == null) {
            return Optional.empty();
        }
        return quizRepository.findByUuid(quizUuid)
                .map(quiz -> new CourseQuizSummary(
                        quiz.getUuid(),
                        quiz.getLessonUuid(),
                        quiz.getTitle()
                ));
    }
}
