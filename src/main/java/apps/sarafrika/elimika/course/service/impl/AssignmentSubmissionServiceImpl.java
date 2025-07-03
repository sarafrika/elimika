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

    @Override
    public AssignmentSubmissionDTO createAssignmentSubmission(AssignmentSubmissionDTO assignmentSubmissionDTO) {
        AssignmentSubmission submission = AssignmentSubmissionFactory.toEntity(assignmentSubmissionDTO);

        // Set defaults based on AssignmentSubmissionDTO business logic
        if (submission.getStatus() == null) {
            submission.setStatus(SubmissionStatus.DRAFT);
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
        return assignmentSubmissionRepository.findAll(pageable).map(AssignmentSubmissionFactory::toDTO);
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
        return assignmentSubmissionRepository.findAll(spec, pageable).map(AssignmentSubmissionFactory::toDTO);
    }

    // Domain-specific methods leveraging AssignmentSubmissionDTO computed properties
    public AssignmentSubmissionDTO submitAssignment(UUID enrollmentUuid, UUID assignmentUuid,
                                                    String content, String[] fileUrls) {
        // Check if already submitted
        if (assignmentSubmissionRepository.existsByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid)) {
            throw new IllegalStateException("Assignment already submitted");
        }

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

    public AssignmentSubmissionDTO gradeSubmission(UUID submissionUuid, BigDecimal score,
                                                   BigDecimal maxScore, String comments) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, submissionUuid)));

        // Calculate percentage
        BigDecimal percentage = BigDecimal.ZERO;
        if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            percentage = score.divide(maxScore,2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        submission.setScore(score);
        submission.setMaxScore(maxScore);
        submission.setPercentage(percentage);
        submission.setInstructorComments(comments);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.GRADED);

        AssignmentSubmission gradedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(gradedSubmission);
    }

    public AssignmentSubmissionDTO returnForRevision(UUID submissionUuid, String feedback) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SUBMISSION_NOT_FOUND_TEMPLATE, submissionUuid)));

        submission.setInstructorComments(feedback);
        submission.setStatus(SubmissionStatus.RETURNED);

        AssignmentSubmission returnedSubmission = assignmentSubmissionRepository.save(submission);
        return AssignmentSubmissionFactory.toDTO(returnedSubmission);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getPendingGrading(UUID instructorUuid) {
        return assignmentSubmissionRepository.findSubmissionsPendingGradingByInstructor(instructorUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getGradedSubmissions(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuidAndStatus(assignmentUuid, SubmissionStatus.GRADED)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .filter(AssignmentSubmissionDTO::isGraded) // Using computed property
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getSubmissionsRequiringFeedback() {
        return assignmentSubmissionRepository.findByStatusAndInstructorCommentsIsNull(SubmissionStatus.GRADED)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getSubmissionsByStudent(UUID studentUuid) {
        return assignmentSubmissionRepository.findByEnrollmentUuid(studentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getSubmissionsByAssignment(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    // Analytics using AssignmentSubmissionDTO computed properties
    @Transactional(readOnly = true)
    public Map<String, Long> getSubmissionCategoryDistribution(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.groupingBy(
                        AssignmentSubmissionDTO::getSubmissionCategory, // Using computed property
                        Collectors.counting()
                ));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getHighPerformanceSubmissions(UUID assignmentUuid) {
        BigDecimal highPerformanceThreshold = new BigDecimal("85.00");
        return assignmentSubmissionRepository.findByAssignmentUuidAndPercentageGreaterThan(
                        assignmentUuid, highPerformanceThreshold)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getAverageSubmissionScore(UUID assignmentUuid) {
        List<AssignmentSubmission> gradedSubmissions = assignmentSubmissionRepository
                .findByAssignmentUuidAndStatus(assignmentUuid, SubmissionStatus.GRADED);

        return gradedSubmissions.stream()
                .filter(s -> s.getPercentage() != null)
                .mapToDouble(s -> s.getPercentage().doubleValue())
                .average()
                .orElse(0.0);
    }

    @Transactional(readOnly = true)
    public boolean hasSubmitted(UUID studentUuid, UUID assignmentUuid) {
        return assignmentSubmissionRepository.existsByEnrollmentUuidAndAssignmentUuid(
                studentUuid, assignmentUuid);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionDTO getSubmissionByStudentAndAssignment(UUID studentUuid, UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(studentUuid, assignmentUuid)
                .map(AssignmentSubmissionFactory::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getTextSubmissions(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .filter(submission -> "Text Submission".equals(submission.getSubmissionCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getFileSubmissions(UUID assignmentUuid) {
        return assignmentSubmissionRepository.findByAssignmentUuid(assignmentUuid)
                .stream()
                .map(AssignmentSubmissionFactory::toDTO)
                .filter(submission -> "File Submission".equals(submission.getSubmissionCategory()))
                .collect(Collectors.toList());
    }

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