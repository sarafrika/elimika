package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ProgramRatingSummaryDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewDTO;
import apps.sarafrika.elimika.course.dto.ProgramReviewRequest;
import apps.sarafrika.elimika.course.factory.ProgramReviewFactory;
import apps.sarafrika.elimika.course.model.ProgramReview;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramReviewRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramReviewService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramReviewServiceImpl implements ProgramReviewService {

    private static final Set<EnrollmentStatus> REVIEW_ELIGIBLE_STATUSES = Set.of(
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.COMPLETED
    );

    private final ProgramReviewRepository programReviewRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final TrainingProgramRepository trainingProgramRepository;

    @Override
    public ProgramReviewDTO saveProgramReview(UUID programUuid, ProgramReviewRequest reviewRequest) {
        enforceProgramExists(programUuid);

        UUID studentUuid = reviewRequest.studentUuid();
        if (!programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                studentUuid, programUuid, REVIEW_ELIGIBLE_STATUSES)) {
            throw new IllegalStateException("Student must have an active or completed program enrollment to leave a review.");
        }

        ProgramReview review = programReviewRepository.findByProgramUuidAndStudentUuid(programUuid, studentUuid)
                .orElseGet(ProgramReview::new);

        if (review.getProgramUuid() == null) {
            review.setProgramUuid(programUuid);
        }
        if (review.getStudentUuid() == null) {
            review.setStudentUuid(studentUuid);
        }

        review.setRating(reviewRequest.rating());
        review.setHeadline(reviewRequest.headline());
        review.setComments(reviewRequest.comments());
        review.setIsAnonymous(Boolean.TRUE.equals(reviewRequest.isAnonymous()));

        ProgramReview saved = programReviewRepository.save(review);
        return toPublicDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramReviewDTO> getReviewsForProgram(UUID programUuid, Pageable pageable) {
        enforceProgramExists(programUuid);
        return programReviewRepository.findByProgramUuid(programUuid, pageable)
                .map(this::toPublicDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramRatingSummaryDTO getRatingSummary(UUID programUuid) {
        enforceProgramExists(programUuid);
        long reviewCount = programReviewRepository.countByProgramUuid(programUuid);
        Double averageRating = reviewCount == 0 ? null : programReviewRepository.findAverageRatingForProgram(programUuid);
        return new ProgramRatingSummaryDTO(programUuid, averageRating, reviewCount);
    }

    private void enforceProgramExists(UUID programUuid) {
        if (programUuid == null) {
            throw new IllegalArgumentException("Program UUID is required");
        }
        if (!trainingProgramRepository.existsByUuid(programUuid)) {
            throw new ResourceNotFoundException("Training program with UUID " + programUuid + " not found");
        }
    }

    private ProgramReviewDTO toPublicDTO(ProgramReview review) {
        ProgramReviewDTO dto = ProgramReviewFactory.toDTO(review);
        if (dto == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(dto.isAnonymous())) {
            return dto;
        }
        return new ProgramReviewDTO(
                dto.uuid(),
                dto.programUuid(),
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
