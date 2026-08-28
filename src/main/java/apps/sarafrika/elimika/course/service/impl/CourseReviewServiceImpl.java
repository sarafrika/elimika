package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.factory.CourseReviewFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseReview;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseReviewRepository;
import apps.sarafrika.elimika.course.service.CourseReviewService;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseReviewServiceImpl implements CourseReviewService {

    private static final Set<String> REVIEW_ELIGIBLE_STATUSES = Set.of("ENROLLED", "ATTENDED", "ABSENT");

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentLookupService enrollmentLookupService;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CourseReviewDTO saveCourseReview(UUID courseUuid, CourseReviewDTO reviewDTO) {
        UUID studentUuid = reviewDTO.studentUuid();

        EnrollmentLookupService.ClassEnrollmentStatusSnapshot enrollment = enrollmentLookupService
                .findMostRecentEnrollmentForCourse(studentUuid, courseUuid)
                .orElseThrow(() -> new IllegalStateException("Student must be enrolled in the course to leave a review."));

        if (!isEligibleEnrollmentStatus(enrollment.status())) {
            throw new IllegalStateException("Student must be enrolled in the course to leave a review.");
        }

        CourseReview review = courseReviewRepository.findByCourseUuidAndStudentUuid(courseUuid, studentUuid)
                .orElseGet(CourseReview::new);

        if (review.getCourseUuid() == null) {
            review.setCourseUuid(courseUuid);
        }
        if (review.getStudentUuid() == null) {
            review.setStudentUuid(studentUuid);
        }

        review.setRating(reviewDTO.rating());
        review.setHeadline(reviewDTO.headline());
        review.setComments(reviewDTO.comments());
        review.setIsAnonymous(Boolean.TRUE.equals(reviewDTO.isAnonymous()));

        CourseReview saved = courseReviewRepository.save(review);
        publishCourseReviewSubmitted(courseUuid, saved);
        return toPublicDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseReviewDTO> getReviewsForCourse(UUID courseUuid) {
        return courseReviewRepository.findByCourseUuid(courseUuid)
                .stream()
                .map(this::toPublicDTO)
                .collect(Collectors.toList());
    }

    private boolean isEligibleEnrollmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return REVIEW_ELIGIBLE_STATUSES.contains(status.toUpperCase());
    }

    private void publishCourseReviewSubmitted(UUID courseUuid, CourseReview review) {
        Course course = courseRepository.findByUuid(courseUuid).orElse(null);
        if (course == null || course.getCourseCreatorUuid() == null || review.getUuid() == null) {
            return;
        }

        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(course.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String courseName = course.getName() == null ? "your course" : course.getName();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "COURSE_REVIEW_SUBMITTED",
                "INBOX",
                "New course review",
                "A learner left a review for " + courseName + ".",
                "/dashboard/course-management/preview/" + course.getUuid(),
                Map.of(
                        "course_uuid", course.getUuid(),
                        "course_name", courseName,
                        "review_uuid", review.getUuid(),
                        "rating", review.getRating()
                ),
                "course-review-submitted:" + review.getUuid() + ":" + reviewContentHash(review)
        ));
    }

    private int reviewContentHash(CourseReview review) {
        return Objects.hash(
                review.getRating(),
                review.getHeadline(),
                review.getComments(),
                review.getIsAnonymous()
        );
    }

    private CourseReviewDTO toPublicDTO(CourseReview review) {
        CourseReviewDTO dto = CourseReviewFactory.toDTO(review);
        if (dto == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(dto.isAnonymous())) {
            return dto;
        }
        return new CourseReviewDTO(
                dto.uuid(),
                dto.courseUuid(),
                null,
                dto.rating(),
                dto.headline(),
                dto.comments(),
                dto.isAnonymous(),
                dto.createdDate(),
                null,
                dto.updatedDate(),
                null
        );
    }
}
