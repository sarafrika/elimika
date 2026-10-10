package apps.sarafrika.elimika.classes.integration;

import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(StudentCourseOverviewQueryIntegrationTest.TestConfig.class)
@DisplayName("The student course overview reads next sessions and submissions in batched queries")
class StudentCourseOverviewQueryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2031, 5, 1, 0, 0);
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID OTHER_STUDENT = UUID.randomUUID();

    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void skipForeignKeys() {
        // Rows reference students, courses and assessments these queries never read.
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();
    }

    @Test
    @DisplayName("each class yields its current or next sitting the learner actually holds")
    void nextSessionPerClass() {
        UUID weekly = UUID.randomUUID();
        UUID ongoing = UUID.randomUUID();
        UUID finished = UUID.randomUUID();

        enrol(STUDENT, instance(weekly, NOW.minusDays(30), SchedulingStatus.COMPLETED), EnrollmentStatus.ATTENDED);
        enrol(STUDENT, instance(weekly, NOW.plusDays(1), SchedulingStatus.CANCELLED), EnrollmentStatus.ENROLLED);
        enrol(STUDENT, instance(weekly, NOW.plusDays(2), SchedulingStatus.SCHEDULED), EnrollmentStatus.CANCELLED);
        enrol(OTHER_STUDENT, instance(weekly, NOW.plusDays(3), SchedulingStatus.SCHEDULED), EnrollmentStatus.ENROLLED);
        UUID expectedWeekly = instance(weekly, NOW.plusDays(4), SchedulingStatus.SCHEDULED);
        enrol(STUDENT, expectedWeekly, EnrollmentStatus.ENROLLED);
        enrol(STUDENT, instance(weekly, NOW.plusDays(11), SchedulingStatus.SCHEDULED), EnrollmentStatus.ENROLLED);

        UUID expectedOngoing = instance(ongoing, NOW.minusHours(1), SchedulingStatus.ONGOING);
        enrol(STUDENT, expectedOngoing, EnrollmentStatus.ENROLLED);
        enrol(STUDENT, instance(finished, NOW.minusDays(5), SchedulingStatus.COMPLETED), EnrollmentStatus.ATTENDED);

        List<ScheduledInstance> next = scheduledInstanceRepository.findNextSessionsForStudent(
                STUDENT, List.of(weekly, ongoing, finished), NOW);

        assertThat(next).extracting(ScheduledInstance::getUuid)
                .containsExactlyInAnyOrder(expectedWeekly, expectedOngoing);
    }

    @Test
    @DisplayName("only the learner's handed-in work counts; drafts and in-progress attempts do not")
    void submittedAssessments() {
        UUID course = UUID.randomUUID();
        UUID ownEnrolment = courseEnrolment(STUDENT, course);
        UUID otherEnrolment = courseEnrolment(OTHER_STUDENT, course);
        UUID handedIn = UUID.randomUUID();
        UUID drafted = UUID.randomUUID();
        UUID someoneElses = UUID.randomUUID();
        submission(ownEnrolment, handedIn, "submitted");
        submission(ownEnrolment, drafted, "draft");
        submission(otherEnrolment, someoneElses, "graded");

        UUID attempted = UUID.randomUUID();
        UUID unfinished = UUID.randomUUID();
        attempt(ownEnrolment, attempted, "graded");
        attempt(ownEnrolment, unfinished, "in_progress");
        attempt(otherEnrolment, unfinished, "submitted");

        assertThat(assignmentSubmissionRepository.findSubmittedAssignmentUuids(
                STUDENT, List.of(handedIn, drafted, someoneElses), SubmissionStatus.DRAFT))
                .containsExactly(handedIn);
        assertThat(quizAttemptRepository.findSubmittedQuizUuids(
                STUDENT, List.of(attempted, unfinished), AttemptStatus.IN_PROGRESS))
                .containsExactly(attempted);
        assertThat(courseEnrollmentRepository.findByStudentUuidAndCourseUuidIn(STUDENT, List.of(course)))
                .singleElement()
                .satisfies(enrolment -> assertThat(enrolment.getUuid()).isEqualTo(ownEnrolment));
    }

    private UUID instance(UUID classUuid, LocalDateTime start, SchedulingStatus status) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(classUuid);
        instance.setInstructorUuid(UUID.randomUUID());
        instance.setStartTime(start);
        instance.setEndTime(start.plusHours(2));
        instance.setTimezone("UTC");
        instance.setTitle("Session");
        instance.setLocationType("ONLINE");
        instance.setMaxParticipants(20);
        instance.setStatus(status);
        return scheduledInstanceRepository.saveAndFlush(instance).getUuid();
    }

    private void enrol(UUID studentUuid, UUID instanceUuid, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setScheduledInstanceUuid(instanceUuid);
        enrollment.setStatus(status);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private UUID courseEnrolment(UUID studentUuid, UUID courseUuid) {
        UUID uuid = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, created_by) "
                        + "VALUES (?1, ?2, ?3, 'active', 'test')")
                .setParameter(1, uuid).setParameter(2, studentUuid).setParameter(3, courseUuid)
                .executeUpdate();
        return uuid;
    }

    private void submission(UUID enrolmentUuid, UUID assignmentUuid, String status) {
        entityManager.createNativeQuery("INSERT INTO assignment_submissions (uuid, enrollment_uuid, assignment_uuid, status, created_by) "
                        + "VALUES (?1, ?2, ?3, ?4, 'test')")
                .setParameter(1, UUID.randomUUID()).setParameter(2, enrolmentUuid)
                .setParameter(3, assignmentUuid).setParameter(4, status)
                .executeUpdate();
    }

    private void attempt(UUID enrolmentUuid, UUID quizUuid, String status) {
        entityManager.createNativeQuery("INSERT INTO quiz_attempts (uuid, enrollment_uuid, quiz_uuid, attempt_number, status, created_by) "
                        + "VALUES (?1, ?2, ?3, 1, ?4, 'test')")
                .setParameter(1, UUID.randomUUID()).setParameter(2, enrolmentUuid)
                .setParameter(3, quizUuid).setParameter(4, status)
                .executeUpdate();
    }
}
