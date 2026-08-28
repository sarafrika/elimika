package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassRatingSummaryDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewRequest;
import apps.sarafrika.elimika.classes.factory.ClassReviewFactory;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassReview;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassReviewRepository;
import apps.sarafrika.elimika.classes.service.ClassReviewService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassReviewServiceImpl implements ClassReviewService {

    private static final Set<String> REVIEW_ELIGIBLE_STATUSES = Set.of("ENROLLED", "ATTENDED", "ABSENT");

    private final ClassReviewRepository classReviewRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final EnrollmentLookupService enrollmentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ClassReviewDTO saveClassReview(UUID classDefinitionUuid, ClassReviewRequest reviewRequest) {
        enforceClassExists(classDefinitionUuid);

        UUID studentUuid = reviewRequest.studentUuid();
        EnrollmentLookupService.ClassEnrollmentStatusSnapshot enrollment = enrollmentLookupService
                .findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid)
                .orElseThrow(() -> new IllegalStateException("Student must be enrolled in the class to leave a review."));

        if (!isEligibleEnrollmentStatus(enrollment.status())) {
            throw new IllegalStateException("Student must be enrolled in the class to leave a review.");
        }

        ClassReview review = classReviewRepository.findByClassDefinitionUuidAndStudentUuid(classDefinitionUuid, studentUuid)
                .orElseGet(ClassReview::new);

        if (review.getClassDefinitionUuid() == null) {
            review.setClassDefinitionUuid(classDefinitionUuid);
        }
        if (review.getStudentUuid() == null) {
            review.setStudentUuid(studentUuid);
        }

        review.setRating(reviewRequest.rating());
        review.setHeadline(reviewRequest.headline());
        review.setComments(reviewRequest.comments());
        review.setIsAnonymous(Boolean.TRUE.equals(reviewRequest.isAnonymous()));

        ClassReview saved = classReviewRepository.save(review);
        publishClassReviewSubmitted(classDefinitionUuid, saved);
        return toPublicDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassReviewDTO> getReviewsForClass(UUID classDefinitionUuid, Pageable pageable) {
        enforceClassExists(classDefinitionUuid);
        return classReviewRepository.findByClassDefinitionUuid(classDefinitionUuid, pageable)
                .map(this::toPublicDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRatingSummaryDTO getRatingSummary(UUID classDefinitionUuid) {
        enforceClassExists(classDefinitionUuid);
        long reviewCount = classReviewRepository.countByClassDefinitionUuid(classDefinitionUuid);
        Double averageRating = reviewCount == 0 ? null : classReviewRepository.findAverageRatingForClassDefinition(classDefinitionUuid);
        return new ClassRatingSummaryDTO(classDefinitionUuid, averageRating, reviewCount);
    }

    private void enforceClassExists(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID is required");
        }
        if (!classDefinitionRepository.existsByUuid(classDefinitionUuid)) {
            throw new ResourceNotFoundException("Class definition with UUID " + classDefinitionUuid + " not found");
        }
    }

    private boolean isEligibleEnrollmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return REVIEW_ELIGIBLE_STATUSES.contains(status.toUpperCase(Locale.ROOT));
    }

    private void publishClassReviewSubmitted(UUID classDefinitionUuid, ClassReview review) {
        ClassDefinition classDefinition = classDefinitionRepository.findByUuid(classDefinitionUuid).orElse(null);
        if (classDefinition == null || classDefinition.getDefaultInstructorUuid() == null || review.getUuid() == null) {
            return;
        }

        UUID recipientUserUuid = instructorLookupService.getInstructorUserUuid(classDefinition.getDefaultInstructorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String classTitle = classDefinition.getTitle() == null ? "your class" : classDefinition.getTitle();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "CLASS_REVIEW_SUBMITTED",
                "INBOX",
                "New class review",
                "A learner left a review for " + classTitle + ".",
                "/dashboard/instructor/training-hub/classes/" + classDefinition.getUuid(),
                Map.of(
                        "class_definition_uuid", classDefinition.getUuid(),
                        "class_title", classTitle,
                        "review_uuid", review.getUuid(),
                        "rating", review.getRating()
                ),
                "class-review-submitted:" + review.getUuid() + ":" + reviewContentHash(review)
        ));
    }

    private int reviewContentHash(ClassReview review) {
        return Objects.hash(
                review.getRating(),
                review.getHeadline(),
                review.getComments(),
                review.getIsAnonymous()
        );
    }

    private ClassReviewDTO toPublicDTO(ClassReview review) {
        ClassReviewDTO dto = ClassReviewFactory.toDTO(review);
        if (dto == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(dto.isAnonymous())) {
            return dto;
        }
        return new ClassReviewDTO(
                dto.uuid(),
                dto.classDefinitionUuid(),
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
