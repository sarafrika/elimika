package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.factory.CourseReviewFactory;
import apps.sarafrika.elimika.course.model.CourseReview;
import apps.sarafrika.elimika.course.repository.CourseReviewRepository;
import apps.sarafrika.elimika.course.service.CourseReviewService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseReviewServiceImpl implements CourseReviewService {

    private static final Set<String> REVIEW_ELIGIBLE_STATUSES = Set.of("ENROLLED", "ATTENDED", "ABSENT");

    private final CourseReviewRepository courseReviewRepository;
    private final EnrollmentLookupService enrollmentLookupService;

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
