package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassRatingSummaryDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewDTO;
import apps.sarafrika.elimika.classes.dto.ClassReviewRequest;
import apps.sarafrika.elimika.classes.model.ClassReview;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassReviewRepository;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassReviewServiceImplTest {

    @Mock
    private ClassReviewRepository classReviewRepository;

    @Mock
    private ClassDefinitionRepository classDefinitionRepository;

    @Mock
    private EnrollmentLookupService enrollmentLookupService;

    private ClassReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassReviewServiceImpl(
                classReviewRepository,
                classDefinitionRepository,
                enrollmentLookupService
        );
    }

    @Test
    void saveClassReviewCreatesReviewForEligibleEnrollment() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid))
                .thenReturn(Optional.of(snapshot("ATTENDED")));
        when(classReviewRepository.findByClassDefinitionUuidAndStudentUuid(classDefinitionUuid, studentUuid))
                .thenReturn(Optional.empty());
        when(classReviewRepository.save(any(ClassReview.class))).thenAnswer(invocation -> {
            ClassReview review = invocation.getArgument(0);
            review.setUuid(UUID.randomUUID());
            return review;
        });

        ClassReviewDTO saved = service.saveClassReview(classDefinitionUuid, request(studentUuid, 5, false));

        ArgumentCaptor<ClassReview> captor = ArgumentCaptor.forClass(ClassReview.class);
        verify(classReviewRepository).save(captor.capture());
        ClassReview review = captor.getValue();

        assertThat(review.getClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(review.getStudentUuid()).isEqualTo(studentUuid);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getIsAnonymous()).isFalse();
        assertThat(saved.uuid()).isNotNull();
    }

    @Test
    void saveClassReviewUpdatesExistingReview() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        ClassReview existing = review(classDefinitionUuid, studentUuid, 3, false);

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid))
                .thenReturn(Optional.of(snapshot("ENROLLED")));
        when(classReviewRepository.findByClassDefinitionUuidAndStudentUuid(classDefinitionUuid, studentUuid))
                .thenReturn(Optional.of(existing));
        when(classReviewRepository.save(any(ClassReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClassReviewDTO saved = service.saveClassReview(classDefinitionUuid, request(studentUuid, 4, true));

        assertThat(saved.rating()).isEqualTo(4);
        assertThat(saved.isAnonymous()).isTrue();
        assertThat(saved.studentUuid()).isNull();
        assertThat(existing.getRating()).isEqualTo(4);
        assertThat(existing.getIsAnonymous()).isTrue();
    }

    @Test
    void saveClassReviewRejectsIneligibleEnrollment() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(studentUuid, classDefinitionUuid))
                .thenReturn(Optional.of(snapshot("WAITLISTED")));

        assertThatThrownBy(() -> service.saveClassReview(classDefinitionUuid, request(studentUuid, 5, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("enrolled in the class");

        verify(classReviewRepository, never()).save(any(ClassReview.class));
    }

    @Test
    void saveClassReviewRejectsMissingClass() {
        UUID classDefinitionUuid = UUID.randomUUID();

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.saveClassReview(classDefinitionUuid, request(UUID.randomUUID(), 5, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Class definition");

        verify(enrollmentLookupService, never())
                .findMostRecentEnrollmentForClassDefinition(any(UUID.class), any(UUID.class));
    }

    @Test
    void getReviewsForClassMasksAnonymousIdentity() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        ClassReview anonymousReview = review(classDefinitionUuid, studentUuid, 5, true);

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(classReviewRepository.findByClassDefinitionUuid(classDefinitionUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(anonymousReview), pageable, 1));

        Page<ClassReviewDTO> reviews = service.getReviewsForClass(classDefinitionUuid, pageable);

        ClassReviewDTO dto = reviews.getContent().getFirst();
        assertThat(dto.studentUuid()).isNull();
        assertThat(dto.createdBy()).isNull();
        assertThat(dto.updatedBy()).isNull();
        assertThat(dto.rating()).isEqualTo(5);
    }

    @Test
    void getRatingSummaryReturnsNullAverageWhenNoReviewsExist() {
        UUID classDefinitionUuid = UUID.randomUUID();

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(classReviewRepository.countByClassDefinitionUuid(classDefinitionUuid)).thenReturn(0L);

        ClassRatingSummaryDTO summary = service.getRatingSummary(classDefinitionUuid);

        assertThat(summary.classDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(summary.averageRating()).isNull();
        assertThat(summary.reviewCount()).isZero();
        verify(classReviewRepository, never()).findAverageRatingForClassDefinition(classDefinitionUuid);
    }

    @Test
    void getRatingSummaryReturnsAverageAndCount() {
        UUID classDefinitionUuid = UUID.randomUUID();

        when(classDefinitionRepository.existsByUuid(classDefinitionUuid)).thenReturn(true);
        when(classReviewRepository.countByClassDefinitionUuid(classDefinitionUuid)).thenReturn(2L);
        when(classReviewRepository.findAverageRatingForClassDefinition(classDefinitionUuid)).thenReturn(4.5);

        ClassRatingSummaryDTO summary = service.getRatingSummary(classDefinitionUuid);

        assertThat(summary.averageRating()).isEqualTo(4.5);
        assertThat(summary.reviewCount()).isEqualTo(2L);
    }

    private ClassReviewRequest request(UUID studentUuid, int rating, boolean anonymous) {
        return new ClassReviewRequest(
                studentUuid,
                rating,
                "Practical class",
                "The class session was clear and hands-on.",
                anonymous
        );
    }

    private ClassReview review(UUID classDefinitionUuid, UUID studentUuid, int rating, boolean anonymous) {
        ClassReview review = new ClassReview();
        review.setUuid(UUID.randomUUID());
        review.setClassDefinitionUuid(classDefinitionUuid);
        review.setStudentUuid(studentUuid);
        review.setRating(rating);
        review.setHeadline("Practical class");
        review.setComments("The class session was clear and hands-on.");
        review.setIsAnonymous(anonymous);
        review.setCreatedDate(LocalDateTime.now());
        review.setCreatedBy("student@example.com");
        review.setLastModifiedDate(LocalDateTime.now());
        review.setLastModifiedBy("student@example.com");
        return review;
    }

    private EnrollmentLookupService.ClassEnrollmentStatusSnapshot snapshot(String status) {
        return new EnrollmentLookupService.ClassEnrollmentStatusSnapshot(
                UUID.randomUUID(),
                status,
                LocalDateTime.now()
        );
    }
}
