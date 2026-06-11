package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ProgramRatingSummaryDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewRequest;
import apps.sarafrika.elimika.course.model.ProgramReview;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramReviewRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramReviewServiceImplTest {

    @Mock
    private ProgramReviewRepository programReviewRepository;

    @Mock
    private ProgramEnrollmentRepository programEnrollmentRepository;

    @Mock
    private TrainingProgramRepository trainingProgramRepository;

    private ProgramReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProgramReviewServiceImpl(
                programReviewRepository,
                programEnrollmentRepository,
                trainingProgramRepository
        );
    }

    @Test
    void saveProgramReviewCreatesReviewForActiveEnrollment() {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                eq(studentUuid),
                eq(programUuid),
                eq(Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED))
        )).thenReturn(true);
        when(programReviewRepository.findByProgramUuidAndStudentUuid(programUuid, studentUuid))
                .thenReturn(Optional.empty());
        when(programReviewRepository.save(any(ProgramReview.class))).thenAnswer(invocation -> {
            ProgramReview review = invocation.getArgument(0);
            review.setUuid(UUID.randomUUID());
            return review;
        });

        ProgramReviewDTO saved = service.saveProgramReview(programUuid, request(studentUuid, 5, false));

        ArgumentCaptor<ProgramReview> captor = ArgumentCaptor.forClass(ProgramReview.class);
        verify(programReviewRepository).save(captor.capture());
        ProgramReview review = captor.getValue();

        assertThat(review.getProgramUuid()).isEqualTo(programUuid);
        assertThat(review.getStudentUuid()).isEqualTo(studentUuid);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getIsAnonymous()).isFalse();
        assertThat(saved.uuid()).isNotNull();
    }

    @Test
    void saveProgramReviewUpdatesExistingReviewForCompletedEnrollment() {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        ProgramReview existing = review(programUuid, studentUuid, 3, false);

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                eq(studentUuid),
                eq(programUuid),
                eq(Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED))
        )).thenReturn(true);
        when(programReviewRepository.findByProgramUuidAndStudentUuid(programUuid, studentUuid))
                .thenReturn(Optional.of(existing));
        when(programReviewRepository.save(any(ProgramReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgramReviewDTO saved = service.saveProgramReview(programUuid, request(studentUuid, 4, true));

        assertThat(saved.rating()).isEqualTo(4);
        assertThat(saved.isAnonymous()).isTrue();
        assertThat(saved.studentUuid()).isNull();
        assertThat(existing.getRating()).isEqualTo(4);
        assertThat(existing.getIsAnonymous()).isTrue();
    }

    @Test
    void saveProgramReviewRejectsIneligibleEnrollment() {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                eq(studentUuid),
                eq(programUuid),
                eq(Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED))
        )).thenReturn(false);

        assertThatThrownBy(() -> service.saveProgramReview(programUuid, request(studentUuid, 5, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active or completed program enrollment");

        verify(programReviewRepository, never()).save(any(ProgramReview.class));
    }

    @Test
    void saveProgramReviewRejectsMissingProgram() {
        UUID programUuid = UUID.randomUUID();

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.saveProgramReview(programUuid, request(UUID.randomUUID(), 5, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Training program");
    }

    @Test
    void getReviewsForProgramMasksAnonymousIdentity() {
        UUID programUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        ProgramReview anonymousReview = review(programUuid, studentUuid, 5, true);

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programReviewRepository.findByProgramUuid(programUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(anonymousReview), pageable, 1));

        Page<ProgramReviewDTO> reviews = service.getReviewsForProgram(programUuid, pageable);

        ProgramReviewDTO dto = reviews.getContent().getFirst();
        assertThat(dto.studentUuid()).isNull();
        assertThat(dto.createdBy()).isNull();
        assertThat(dto.updatedBy()).isNull();
        assertThat(dto.rating()).isEqualTo(5);
    }

    @Test
    void getRatingSummaryReturnsNullAverageWhenNoReviewsExist() {
        UUID programUuid = UUID.randomUUID();

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programReviewRepository.countByProgramUuid(programUuid)).thenReturn(0L);

        ProgramRatingSummaryDTO summary = service.getRatingSummary(programUuid);

        assertThat(summary.programUuid()).isEqualTo(programUuid);
        assertThat(summary.averageRating()).isNull();
        assertThat(summary.reviewCount()).isZero();
        verify(programReviewRepository, never()).findAverageRatingForProgram(programUuid);
    }

    @Test
    void getRatingSummaryReturnsAverageAndCount() {
        UUID programUuid = UUID.randomUUID();

        when(trainingProgramRepository.existsByUuid(programUuid)).thenReturn(true);
        when(programReviewRepository.countByProgramUuid(programUuid)).thenReturn(2L);
        when(programReviewRepository.findAverageRatingForProgram(programUuid)).thenReturn(4.5);

        ProgramRatingSummaryDTO summary = service.getRatingSummary(programUuid);

        assertThat(summary.averageRating()).isEqualTo(4.5);
        assertThat(summary.reviewCount()).isEqualTo(2L);
    }

    private ProgramReviewRequest request(UUID studentUuid, int rating, boolean anonymous) {
        return new ProgramReviewRequest(
                studentUuid,
                rating,
                "Excellent pathway",
                "The sequence was practical.",
                anonymous
        );
    }

    private ProgramReview review(UUID programUuid, UUID studentUuid, int rating, boolean anonymous) {
        ProgramReview review = new ProgramReview();
        review.setUuid(UUID.randomUUID());
        review.setProgramUuid(programUuid);
        review.setStudentUuid(studentUuid);
        review.setRating(rating);
        review.setHeadline("Excellent pathway");
        review.setComments("The sequence was practical.");
        review.setIsAnonymous(anonymous);
        review.setCreatedDate(LocalDateTime.now());
        review.setCreatedBy("student@example.com");
        review.setLastModifiedDate(LocalDateTime.now());
        review.setLastModifiedBy("student@example.com");
        return review;
    }
}
