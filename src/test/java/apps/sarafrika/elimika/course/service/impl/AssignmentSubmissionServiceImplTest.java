package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionRequest;
import apps.sarafrika.elimika.course.internal.AssignmentMediaValidationService;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.spi.AssessmentCompletedNotificationRequestedEvent;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionServiceImplTest {

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private GenericSpecificationBuilder<AssignmentSubmission> specificationBuilder;
    @Mock
    private AssignmentMediaValidationService assignmentMediaValidationService;
    @Mock
    private CourseGradeBookService courseGradeBookService;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private DomainSecurityService domainSecurityService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AssignmentSubmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentSubmissionServiceImpl(
                assignmentSubmissionRepository,
                assignmentRepository,
                specificationBuilder,
                assignmentMediaValidationService,
                courseGradeBookService,
                courseEnrollmentRepository,
                lessonRepository,
                domainSecurityService,
                eventPublisher
        );
    }

    @Test
    void submitAssignmentResolvesStudentUuidToActiveCourseEnrollment() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();

        stubAssignmentCourseAndEnrollment(assignmentUuid, lessonUuid, courseUuid,
                activeEnrollment(enrollmentUuid, studentUuid, courseUuid));
        when(courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid))
                .thenReturn(Optional.of(activeEnrollment(enrollmentUuid, studentUuid, courseUuid)));
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid))
                .thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(invocation -> savedSubmission(invocation.getArgument(0)));

        service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(null, studentUuid, "Resolved by student", null),
                false
        );

        ArgumentCaptor<AssignmentSubmission> submissionCaptor = ArgumentCaptor.forClass(AssignmentSubmission.class);
        verify(assignmentSubmissionRepository).save(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getEnrollmentUuid()).isEqualTo(enrollmentUuid);
        assertThat(submissionCaptor.getValue().getAssignmentUuid()).isEqualTo(assignmentUuid);
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);

        verify(eventPublisher).publishEvent(any(AssessmentCompletedNotificationRequestedEvent.class));
    }

    @Test
    void submitAssignmentRejectsClassEnrollmentUuidBeforePersisting() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID classEnrollmentUuid = UUID.randomUUID();

        stubAssignmentAndLesson(assignmentUuid, lessonUuid, courseUuid);
        when(courseEnrollmentRepository.findByUuid(classEnrollmentUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(classEnrollmentUuid, null, "Content", null),
                false
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course enrollment with ID");

        verify(assignmentSubmissionRepository, never()).save(any());
    }

    @Test
    void submitAssignmentRejectsInactiveCourseEnrollment() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();

        stubAssignmentCourseAndEnrollment(assignmentUuid, lessonUuid, courseUuid,
                enrollment(enrollmentUuid, studentUuid, courseUuid, EnrollmentStatus.COMPLETED));

        assertThatThrownBy(() -> service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(enrollmentUuid, null, "Content", null),
                false
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Course enrollment must be active");

        verify(assignmentSubmissionRepository, never()).save(any());
    }

    @Test
    void submitAssignmentResubmitsReturnedSubmissionAndClearsGradeFields() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        AssignmentSubmission existing = existingSubmission(enrollmentUuid, assignmentUuid, SubmissionStatus.RETURNED);
        existing.setScore(BigDecimal.valueOf(50));
        existing.setMaxScore(BigDecimal.valueOf(100));
        existing.setPercentage(BigDecimal.valueOf(50));
        existing.setInstructorComments("Needs revision");
        existing.setGradedByUuid(UUID.randomUUID());

        stubAssignmentCourseAndEnrollment(assignmentUuid, lessonUuid, courseUuid,
                activeEnrollment(enrollmentUuid, studentUuid, courseUuid));
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid))
                .thenReturn(Optional.of(existing));
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(invocation -> savedSubmission(invocation.getArgument(0)));

        service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(enrollmentUuid, null, "Revised work", new String[]{"https://example.test/revised.pdf"}),
                false
        );

        ArgumentCaptor<AssignmentSubmission> submissionCaptor = ArgumentCaptor.forClass(AssignmentSubmission.class);
        verify(assignmentSubmissionRepository).save(submissionCaptor.capture());
        AssignmentSubmission saved = submissionCaptor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(saved.getSubmissionText()).isEqualTo("Revised work");
        assertThat(saved.getFileUrls()).containsExactly("https://example.test/revised.pdf");
        assertThat(saved.getScore()).isNull();
        assertThat(saved.getMaxScore()).isNull();
        assertThat(saved.getPercentage()).isNull();
        assertThat(saved.getInstructorComments()).isNull();
        assertThat(saved.getGradedByUuid()).isNull();
    }

    @Test
    void submitAssignmentRejectsDuplicateSubmittedSubmission() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();

        stubAssignmentCourseAndEnrollment(assignmentUuid, lessonUuid, courseUuid,
                activeEnrollment(enrollmentUuid, studentUuid, courseUuid));
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid))
                .thenReturn(Optional.of(existingSubmission(enrollmentUuid, assignmentUuid, SubmissionStatus.SUBMITTED)));

        assertThatThrownBy(() -> service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(enrollmentUuid, null, "Duplicate", null),
                false
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already submitted");

        verify(assignmentSubmissionRepository, never()).save(any());
    }

    @Test
    void submitAssignmentRejectsUnpublishedAssignment() {
        UUID assignmentUuid = UUID.randomUUID();
        Assignment assignment = assignment(assignmentUuid, UUID.randomUUID(), false);
        when(assignmentRepository.findByUuid(assignmentUuid)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(UUID.randomUUID(), null, "Content", null),
                false
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not published");

        verify(assignmentSubmissionRepository, never()).save(any());
    }

    @Test
    void submitAssignmentPassesUploadedFilesFlagToValidation() {
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        Assignment assignment = assignment(assignmentUuid, lessonUuid, true);
        assignment.setSubmissionTypes(new String[]{"DOCUMENT"});

        when(assignmentRepository.findByUuid(assignmentUuid)).thenReturn(Optional.of(assignment));
        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(lessonUuid, courseUuid)));
        when(courseEnrollmentRepository.findByUuid(enrollmentUuid))
                .thenReturn(Optional.of(activeEnrollment(enrollmentUuid, studentUuid, courseUuid)));
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(assignmentSubmissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignmentUuid))
                .thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(invocation -> savedSubmission(invocation.getArgument(0)));

        service.submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(enrollmentUuid, null, null, null),
                true
        );

        verify(assignmentMediaValidationService)
                .validateSubmissionRequest(eq(new String[]{"DOCUMENT"}), eq(null), eq(null), eq(true));
    }

    private void stubAssignmentCourseAndEnrollment(UUID assignmentUuid,
                                                   UUID lessonUuid,
                                                   UUID courseUuid,
                                                   CourseEnrollment enrollment) {
        stubAssignmentAndLesson(assignmentUuid, lessonUuid, courseUuid);
        when(courseEnrollmentRepository.findByUuid(enrollment.getUuid())).thenReturn(Optional.of(enrollment));
    }

    private void stubAssignmentAndLesson(UUID assignmentUuid, UUID lessonUuid, UUID courseUuid) {
        when(assignmentRepository.findByUuid(assignmentUuid))
                .thenReturn(Optional.of(assignment(assignmentUuid, lessonUuid, true)));
        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(lessonUuid, courseUuid)));
    }

    private Assignment assignment(UUID assignmentUuid, UUID lessonUuid, boolean published) {
        Assignment assignment = new Assignment();
        assignment.setUuid(assignmentUuid);
        assignment.setLessonUuid(lessonUuid);
        assignment.setTitle("Assignment");
        assignment.setSubmissionTypes(new String[]{"TEXT"});
        assignment.setIsPublished(published);
        return assignment;
    }

    private Lesson lesson(UUID lessonUuid, UUID courseUuid) {
        Lesson lesson = new Lesson();
        lesson.setUuid(lessonUuid);
        lesson.setCourseUuid(courseUuid);
        return lesson;
    }

    private CourseEnrollment activeEnrollment(UUID enrollmentUuid, UUID studentUuid, UUID courseUuid) {
        return enrollment(enrollmentUuid, studentUuid, courseUuid, EnrollmentStatus.ACTIVE);
    }

    private CourseEnrollment enrollment(UUID enrollmentUuid,
                                        UUID studentUuid,
                                        UUID courseUuid,
                                        EnrollmentStatus status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUuid(enrollmentUuid);
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(status);
        return enrollment;
    }

    private AssignmentSubmission existingSubmission(UUID enrollmentUuid,
                                                    UUID assignmentUuid,
                                                    SubmissionStatus status) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setUuid(UUID.randomUUID());
        submission.setEnrollmentUuid(enrollmentUuid);
        submission.setAssignmentUuid(assignmentUuid);
        submission.setStatus(status);
        return submission;
    }

    private AssignmentSubmission savedSubmission(AssignmentSubmission submission) {
        if (submission.getUuid() == null) {
            submission.setUuid(UUID.randomUUID());
        }
        return submission;
    }
}
