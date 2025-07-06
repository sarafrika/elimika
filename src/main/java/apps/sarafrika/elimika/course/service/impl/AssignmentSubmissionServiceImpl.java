package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import apps.sarafrika.elimika.course.factory.AssignmentSubmissionFactory;
import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.service.AssignmentSubmissionService;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentSubmissionServiceImpl implements AssignmentSubmissionService {

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final GenericSpecificationBuilder<AssignmentSubmission> specificationBuilder;

    private static final String SUBMISSION_NOT_FOUND_TEMPLATE = "Assignment submission with ID %s not found";

    // ===== BASIC CRUD OPERATIONS =====

    @Override
    public AssignmentSubmissionDTO createAssignmentSubmission(AssignmentSubmissionDTO assignmentSubmissionDTO) {
        AssignmentSubmission submission = AssignmentSubmissionFactory.toEntity(assignmentSubmissionDTO);

        // Set defaults
        if (submission.getStatus() == null) {
            submission.setStatus(SubmissionStatus.SUBMITTED);
        }
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(LocalDateTime.now());
        }

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(savedSubmission);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmissionDTO getAssignmentSubmissionByUuid(UUID uuid) {
        return assignmentSubmissionRepository.findByUuid(uuid)
                .map(AssignmentSubmissionFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentSubmissionDTO> getAllAssignmentSubmissions(Pageable pageable) {
        return assignmentSubmissionRepository.findAll(pageable)
                .map(AssignmentSubmissionFactory::toDTO);
    }

    @Override
    public AssignmentSubmissionDTO updateAssignmentSubmission(UUID uuid, AssignmentSubmissionDTO assignmentSubmissionDTO) {
        AssignmentSubmission existingSubmission = assignmentSubmissionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, uuid)));

        updateSubmissionFields(existingSubmission, assignmentSubmissionDTO);

        AssignmentSubmission updatedSubmission = assignmentSubmissionRepository.save(existingSubmission);
        return AssignmentSubmissionFactory.toDTO(updatedSubmission);
    }

    @Override
    public void deleteAssignmentSubmission(UUID uuid) {
        if (!assignmentSubmissionRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(SUBMISSION_NOT_FOUND_TEMPLATE, uuid));
        }
        assignmentSubmissionRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentSubmissionDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<AssignmentSubmission> spec = specificationBuilder.buildSpecification(
                AssignmentSubmission.class, searchParams);
        return assignmentSubmissionRepository.findAll(spec, pageable)
                .map(AssignmentSubmissionFactory::toDTO);
    }

    // ===== SUBMISSION WORKFLOW OPERATIONS =====

    @Override
    public AssignmentSubmissionDTO submitAssignment(UUID enrollmentUuid, UUID assignmentUuid,
                                                    String content, String[] fileUrls) {
        // Check if student already has a submission for this assignment
        assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Student has already submitted this assignment");
                });

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setEnrollmentUuid(enrollmentUuid);
        submission.setAssignmentUuid(assignmentUuid);
        submission.setSubmissionText(content);
        submission.setFileUrls(fileUrls);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(savedSubmission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getSubmissionsByAssignment(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AssignmentSubmissionDTO gradeSubmission(UUID submissionUuid, BigDecimal score,
                                                   BigDecimal maxScore, String comments) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, submissionUuid)));

        // Validate score
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and maximum score");
        }

        // Calculate percentage
        BigDecimal percentage = score.divide(maxScore, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        submission.setScore(score);
        submission.setMaxScore(maxScore);
        submission.setPercentage(percentage);
        submission.setInstructorComments(comments);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.GRADED);

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(savedSubmission);
    }

    @Override
    public AssignmentSubmissionDTO returnForRevision(UUID submissionUuid, String feedback) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, submissionUuid)));

        submission.setInstructorComments(feedback);
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setGradedAt(LocalDateTime.now());

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(savedSubmission);
    }

    // ===== ANALYTICS OPERATIONS =====

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getSubmissionCategoryDistribution(UUID assignmentUuid) {
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid);

        return submissions.stream()
                .collect(Collectors.groupingBy(
                        submission -> submission.getStatus().name(),
                        Collectors.counting()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageSubmissionScore(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuidAndStatus(assignmentUuid, SubmissionStatus.GRADED)
                .stream()
                .filter(submission -> submission.getPercentage() != null)
                .mapToDouble(submission -> submission.getPercentage().doubleValue())
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getHighPerformanceSubmissions(UUID assignmentUuid) {
        BigDecimal threshold = BigDecimal.valueOf(85.0);

        return assignmentSubmissionRepository.findByAssignmentUuidAndStatus(assignmentUuid, SubmissionStatus.GRADED)
                .stream()
                .filter(submission -> submission.getPercentage() != null &&
                        submission.getPercentage().compareTo(threshold) >= 0)
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getPendingGrading(UUID instructorUuid) {
        // This would typically require a join with Assignment table to filter by instructor
        // For now, returning all submissions with SUBMITTED status
        return assignmentSubmissionRepository.findByStatus(SubmissionStatus.SUBMITTED)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE HELPER METHODS =====

    private void updateSubmissionFields(AssignmentSubmission existingSubmission, AssignmentSubmissionDTO dto) {
        if (dto.enrollmentUuid() != null) {
            existingSubmission.setEnrollmentUuid(dto.enrollmentUuid());
        }
        if (dto.assignmentUuid() != null) {
            existingSubmission.setAssignmentUuid(dto.assignmentUuid());
        }
        if (dto.submissionText() != null) {
            existingSubmission.setSubmissionText(dto.submissionText());
        }
        if (dto.fileUrls() != null) {
            existingSubmission.setFileUrls(dto.fileUrls());
        }
        if (dto.submittedAt() != null) {
            existingSubmission.setSubmittedAt(dto.submittedAt());
        }
        if (dto.status() != null) {
            existingSubmission.setStatus(dto.status());
        }
        if (dto.score() != null) {
            existingSubmission.setScore(dto.score());
        }
        if (dto.maxScore() != null) {
            existingSubmission.setMaxScore(dto.maxScore());
        }
        if (dto.percentage() != null) {
            existingSubmission.setPercentage(dto.percentage());
        }
        if (dto.instructorComments() != null) {
            existingSubmission.setInstructorComments(dto.instructorComments());
        }
        if (dto.gradedAt() != null) {
            existingSubmission.setGradedAt(dto.gradedAt());
        }
        if (dto.gradedByUuid() != null) {
            existingSubmission.setGradedByUuid(dto.gradedByUuid());
        }
    }
}