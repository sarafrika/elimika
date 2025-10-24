package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.shared.spi.analytics.CourseAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseAnalyticsSnapshot;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseAnalyticsServiceImpl implements CourseAnalyticsService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;

    @Override
    public CourseAnalyticsSnapshot captureSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.countByStatus(ContentStatus.PUBLISHED);
        long inReviewCourses = courseRepository.countByStatus(ContentStatus.IN_REVIEW);
        long draftCourses = courseRepository.countByStatus(ContentStatus.DRAFT);
        long archivedCourses = courseRepository.countByStatus(ContentStatus.ARCHIVED);

        long totalCourseEnrollments = courseEnrollmentRepository.count();
        long activeCourseEnrollments = courseEnrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);
        long newCourseEnrollments7d = courseEnrollmentRepository.countByEnrollmentDateAfter(sevenDaysAgo);
        long completedCourseEnrollments30d = courseEnrollmentRepository
                .countByStatusAndCompletionDateAfter(EnrollmentStatus.COMPLETED, thirtyDaysAgo);

        BigDecimal averageProgressRaw = courseEnrollmentRepository.calculateAverageProgressPercentage();
        double averageCourseProgress = averageProgressRaw == null ? 0.0 : averageProgressRaw.doubleValue();

        long totalTrainingPrograms = trainingProgramRepository.count();
        long publishedTrainingPrograms = trainingProgramRepository.countByIsPublishedTrue();
        long activeTrainingPrograms = trainingProgramRepository.countByActiveTrue();

        long programEnrollments = programEnrollmentRepository.count();
        long completedProgramEnrollments30d = programEnrollmentRepository
                .countByStatusAndCompletionDateAfter(EnrollmentStatus.COMPLETED, thirtyDaysAgo);

        return new CourseAnalyticsSnapshot(
                totalCourses,
                publishedCourses,
                inReviewCourses,
                draftCourses,
                archivedCourses,
                totalCourseEnrollments,
                activeCourseEnrollments,
                newCourseEnrollments7d,
                completedCourseEnrollments30d,
                averageCourseProgress,
                totalTrainingPrograms,
                publishedTrainingPrograms,
                activeTrainingPrograms,
                programEnrollments,
                completedProgramEnrollments30d
        );
    }
}
