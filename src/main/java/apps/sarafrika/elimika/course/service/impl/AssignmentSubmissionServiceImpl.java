package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionRequest;
import apps.sarafrika.elimika.course.factory.AssignmentSubmissionFactory;
import apps.sarafrika.elimika.course.internal.AssignmentMediaValidationService;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.AssignmentSubmissionService;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.spi.AssessmentCompletedNotificationRequestedEvent;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
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
    private final AssignmentRepository assignmentRepository;
    private final GenericSpecificationBuilder<AssignmentSubmission> specificationBuilder;
    private final AssignmentMediaValidationService assignmentMediaValidationService;
    private final CourseGradeBookService courseGradeBookService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LessonRepository lessonRepository;
    private final DomainSecurityService domainSecurityService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SUBMISSION_NOT_FOUND_TEMPLATE = "Assignment submission with ID %s not found";

    // ===== BASIC CRUD OPERATIONS =====

    @Override
    public AssignmentSubmissionDTO createAssignmentSubmission(AssignmentSubmissionDTO assignmentSubmissionDTO) {
        Assignment assignment = assignmentRepository.findByUuid(assignmentSubmissionDTO.assignmentUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Assignment with ID %s not found", assignmentSubmissionDTO.assignmentUuid())));

        assignmentMediaValidationService.validateSubmissionRequest(
                assignment.getSubmissionTypes(),
                assignmentSubmissionDTO.submissionText(),
                assignmentSubmissionDTO.fileUrls()
        );

        AssignmentSubmission submission = AssignmentSubmissionFactory.toEntity(assignmentSubmissionDTO);

        // Set defaults
        if (submission.getStatus() == null) {
            submission.setStatus(SubmissionStatus.SUBMITTED);
        }
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(LocalDateTime.now());
        }

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        publishAssessmentCompletedNotification(savedSubmission, assignment.getTitle(), "assignment");
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
    public AssignmentSubmissionDTO submitAssignment(UUID assignmentUuid,
                                                    AssignmentSubmissionRequest request,
                                                    boolean hasUploadedFiles) {
        if (request == null) {
            throw new IllegalArgumentException("Submission request is required");
        }

        Assignment assignment = getAssignmentOrThrow(assignmentUuid);
        enforcePublishedAssignment(assignment);
        assignmentMediaValidationService.validateSubmissionRequest(
                assignment.getSubmissionTypes(),
                request.submissionText(),
                request.fileUrls(),
                hasUploadedFiles
        );
        CourseEnrollment enrollment = resolveCourseEnrollment(request, assignment);
        enforceSubmitterCanUseEnrollment(enrollment);

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByEnrollmentUuidAndAssignmentUuid(enrollment.getUuid(), assignmentUuid)
                .map(existing -> prepareResubmission(existing, request))
                .orElseGet(() -> newSubmission(enrollment.getUuid(), assignmentUuid, request));

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);
        publishAssessmentCompletedNotification(savedSubmission, assignment.getTitle(), "assignment");
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
        courseGradeBookService.syncAssignmentGrade(
                savedSubmission.getAssignmentUuid(),
                savedSubmission.getEnrollmentUuid(),
                savedSubmission.getScore(),
                savedSubmission.getMaxScore(),
                savedSubmission.getInstructorComments(),
                savedSubmission.getGradedAt(),
                savedSubmission.getGradedByUuid()
        );
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

    private Assignment getAssignmentOrThrow(UUID assignmentUuid) {
        return assignmentRepository.findByUuid(assignmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Assignment with ID %s not found", assignmentUuid)));
    }

    private void enforcePublishedAssignment(Assignment assignment) {
        if (!Boolean.TRUE.equals(assignment.getIsPublished())) {
            throw new IllegalStateException("Assignment is not published.");
        }
    }

    private CourseEnrollment resolveCourseEnrollment(AssignmentSubmissionRequest request, Assignment assignment) {
        UUID courseUuid = resolveAssignmentCourseUuid(assignment);

        CourseEnrollment enrollment;
        if (request.enrollmentUuid() != null) {
            enrollment = courseEnrollmentRepository.findByUuid(request.enrollmentUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Course enrollment with ID %s not found", request.enrollmentUuid())));
            if (!courseUuid.equals(enrollment.getCourseUuid())) {
                throw new IllegalArgumentException("Course enrollment does not belong to the assignment course.");
            }
            if (request.studentUuid() != null && !request.studentUuid().equals(enrollment.getStudentUuid())) {
                throw new IllegalArgumentException("student_uuid does not match enrollment_uuid.");
            }
        } else {
            if (request.studentUuid() == null) {
                throw new IllegalArgumentException("Either enrollment_uuid or student_uuid is required.");
            }
            enrollment = courseEnrollmentRepository.findByStudentUuidAndCourseUuid(request.studentUuid(), courseUuid)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Active course enrollment for student %s and assignment course not found",
                                    request.studentUuid())));
        }

        if (!EnrollmentStatus.ACTIVE.equals(enrollment.getStatus())) {
            throw new IllegalStateException("Course enrollment must be active to submit assignments.");
        }

        return enrollment;
    }

    private UUID resolveAssignmentCourseUuid(Assignment assignment) {
        Lesson lesson = lessonRepository.findByUuid(assignment.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Lesson with ID %s not found", assignment.getLessonUuid())));
        return lesson.getCourseUuid();
    }

    private void enforceSubmitterCanUseEnrollment(CourseEnrollment enrollment) {
        if (domainSecurityService.isInstructorOrAdmin()) {
            return;
        }
        if (!domainSecurityService.isStudentWithUuid(enrollment.getStudentUuid())) {
            throw new AccessDeniedException("Students may only submit assignments for their own course enrollment.");
        }
    }

    private AssignmentSubmission prepareResubmission(AssignmentSubmission existingSubmission,
                                                    AssignmentSubmissionRequest request) {
        SubmissionStatus status = existingSubmission.getStatus();
        if (status != SubmissionStatus.DRAFT && status != SubmissionStatus.RETURNED) {
            throw new IllegalStateException("Student has already submitted this assignment");
        }

        existingSubmission.setSubmissionText(request.submissionText());
        existingSubmission.setFileUrls(request.fileUrls());
        existingSubmission.setSubmittedAt(LocalDateTime.now());
        existingSubmission.setStatus(SubmissionStatus.SUBMITTED);
        existingSubmission.setScore(null);
        existingSubmission.setMaxScore(null);
        existingSubmission.setPercentage(null);
        existingSubmission.setInstructorComments(null);
        existingSubmission.setGradedAt(null);
        existingSubmission.setGradedByUuid(null);
        return existingSubmission;
    }

    private AssignmentSubmission newSubmission(UUID enrollmentUuid,
                                               UUID assignmentUuid,
                                               AssignmentSubmissionRequest request) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setEnrollmentUuid(enrollmentUuid);
        submission.setAssignmentUuid(assignmentUuid);
        submission.setSubmissionText(request.submissionText());
        submission.setFileUrls(request.fileUrls());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return submission;
    }

    private void publishAssessmentCompletedNotification(AssignmentSubmission submission,
                                                        String assessmentTitle,
                                                        String assessmentType) {
        if (submission.getEnrollmentUuid() == null) {
            return;
        }
        CourseEnrollment enrollment = courseEnrollmentRepository.findByUuid(submission.getEnrollmentUuid()).orElse(null);
        if (enrollment == null || enrollment.getStudentUuid() == null) {
            return;
        }

        String title = assessmentTitle == null || assessmentTitle.isBlank() ? "Assessment" : assessmentTitle;
        eventPublisher.publishEvent(new AssessmentCompletedNotificationRequestedEvent(
                enrollment.getStudentUuid(),
                enrollment.getCourseUuid(),
                submission.getEnrollmentUuid(),
                submission.getAssignmentUuid(),
                submission.getUuid(),
                title,
                assessmentType
        ));
    }
}
