package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentCourseEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceImplTest {

    @Mock
    private ScheduledInstanceRepository scheduledInstanceRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GenericSpecificationBuilder<ScheduledInstance> scheduledInstanceSpecBuilder;

    @Mock
    private GenericSpecificationBuilder<Enrollment> enrollmentSpecBuilder;

    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;

    @Mock
    private CourseInfoService courseInfoService;

    @Mock
    private LearnerProgressLookupService learnerProgressLookupService;

    @Mock
    private AgeVerificationService ageVerificationService;

    @Mock
    private apps.sarafrika.elimika.commerce.spi.paywall.CommercePaywallService commercePaywallService;

    @Mock
    private AvailabilityService availabilityService;

    private TimetableServiceImpl timetableService;

    @BeforeEach
    void setUp() {
        timetableService = new TimetableServiceImpl(
                scheduledInstanceRepository,
                enrollmentRepository,
                applicationEventPublisher,
                scheduledInstanceSpecBuilder,
                enrollmentSpecBuilder,
                classDefinitionLookupService,
                courseInfoService,
                learnerProgressLookupService,
                ageVerificationService,
                commercePaywallService,
                availabilityService
        );
    }

    @Test
    void getScheduleForInstructorFiltersCancelledInstances() {
        UUID instructorUuid = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(1);

        ScheduledInstance active = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        ScheduledInstance cancelled = buildScheduledInstance(instructorUuid, SchedulingStatus.CANCELLED);

        when(scheduledInstanceRepository.findByInstructorAndTimeRange(
                eq(instructorUuid),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(active, cancelled));

        List<ScheduledInstanceDTO> result = timetableService.getScheduleForInstructor(instructorUuid, start, end);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(ScheduledInstanceDTO::uuid)
                .isEqualTo(active.getUuid());
    }

    @Test
    void getScheduleForStudentFiltersCancelledInstancesAndEnrollments() {
        UUID studentUuid = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(1);

        Enrollment activeEnrollment = buildEnrollment(EnrollmentStatus.ENROLLED);
        Enrollment cancelledEnrollment = buildEnrollment(EnrollmentStatus.CANCELLED);

        ScheduledInstance activeInstance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        activeInstance.setUuid(activeEnrollment.getScheduledInstanceUuid());

        ScheduledInstance cancelledInstance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.CANCELLED);
        cancelledInstance.setUuid(cancelledEnrollment.getScheduledInstanceUuid());

        when(enrollmentRepository.findByStudentAndTimeRange(
                eq(studentUuid),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(activeEnrollment, cancelledEnrollment));

        when(scheduledInstanceRepository.findByUuid(activeEnrollment.getScheduledInstanceUuid()))
                .thenReturn(Optional.of(activeInstance));
        List<StudentScheduleDTO> result = timetableService.getScheduleForStudent(studentUuid, start, end);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(StudentScheduleDTO::scheduledInstanceUuid)
                .isEqualTo(activeInstance.getUuid());
    }

    @Test
    void getEnrollmentsForStudentReturnsEnrollmentDtos() {
        UUID studentUuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        Enrollment firstEnrollment = buildEnrollment(EnrollmentStatus.WAITLISTED);
        firstEnrollment.setStudentUuid(studentUuid);

        Enrollment secondEnrollment = buildEnrollment(EnrollmentStatus.ATTENDED);
        secondEnrollment.setStudentUuid(studentUuid);

        when(enrollmentRepository.findPageByStudentUuidOrderByScheduledInstanceStartTime(studentUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(firstEnrollment, secondEnrollment), pageable, 2));

        Page<EnrollmentDTO> result = timetableService.getEnrollmentsForStudent(studentUuid, pageable);

        assertThat(result.getContent())
                .hasSize(2)
                .extracting(EnrollmentDTO::uuid)
                .containsExactly(firstEnrollment.getUuid(), secondEnrollment.getUuid());
        assertThat(result.getContent())
                .extracting(EnrollmentDTO::status)
                .containsExactly(EnrollmentStatus.WAITLISTED, EnrollmentStatus.ATTENDED);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(enrollmentRepository).findPageByStudentUuidOrderByScheduledInstanceStartTime(studentUuid, pageable);
    }

    @Test
    void getClassEnrollmentsForStudentGroupsEnrollmentsByClassDefinition() {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        Enrollment firstEnrollment = buildEnrollment(EnrollmentStatus.ATTENDED);
        firstEnrollment.setStudentUuid(studentUuid);
        firstEnrollment.setCreatedDate(LocalDateTime.of(2026, 4, 10, 8, 0));

        Enrollment secondEnrollment = buildEnrollment(EnrollmentStatus.ENROLLED);
        secondEnrollment.setStudentUuid(studentUuid);
        secondEnrollment.setCreatedDate(LocalDateTime.of(2026, 4, 12, 8, 0));

        ScheduledInstance firstInstance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.COMPLETED);
        firstInstance.setUuid(firstEnrollment.getScheduledInstanceUuid());
        firstInstance.setClassDefinitionUuid(classDefinitionUuid);
        firstInstance.setTitle("Java Fundamentals");
        firstInstance.setStartTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        ScheduledInstance secondInstance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        secondInstance.setUuid(secondEnrollment.getScheduledInstanceUuid());
        secondInstance.setClassDefinitionUuid(classDefinitionUuid);
        secondInstance.setTitle("Java Fundamentals");
        secondInstance.setStartTime(LocalDateTime.of(2026, 4, 12, 9, 0));

        when(enrollmentRepository.findClassDefinitionUuidsByStudentUuid(studentUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(classDefinitionUuid), pageable, 1));
        when(enrollmentRepository.findByStudentUuidAndClassDefinitionUuidIn(eq(studentUuid), anyCollection()))
                .thenReturn(List.of(firstEnrollment, secondEnrollment));
        when(scheduledInstanceRepository.findByUuidIn(anyCollection()))
                .thenReturn(List.of(firstInstance, secondInstance));

        Page<StudentClassEnrollmentSummaryDTO> result = timetableService.getClassEnrollmentsForStudent(studentUuid, pageable);

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .satisfies(summary -> {
                    assertThat(summary.class_definition_uuid()).isEqualTo(classDefinitionUuid);
                    assertThat(summary.class_title()).isEqualTo("Java Fundamentals");
                    assertThat(summary.latest_enrollment_status()).isEqualTo(EnrollmentStatus.ENROLLED);
                    assertThat(summary.scheduled_instance_count()).isEqualTo(2);
                    assertThat(summary.latest_scheduled_instance_start_time())
                            .isEqualTo(LocalDateTime.of(2026, 4, 12, 9, 0));
                });
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getCourseEnrollmentsForStudentReturnsCourseSummaries() {
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(learnerProgressLookupService.findCourseProgress(studentUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(new LearnerCourseProgressView(
                        UUID.randomUUID(),
                        courseUuid,
                        "Backend Engineering",
                        "ACTIVE",
                        null,
                        LocalDateTime.of(2026, 4, 12, 10, 0)
                )), pageable, 1));

        Page<StudentCourseEnrollmentSummaryDTO> result = timetableService.getCourseEnrollmentsForStudent(studentUuid, pageable);

        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .satisfies(summary -> {
                    assertThat(summary.course_uuid()).isEqualTo(courseUuid);
                    assertThat(summary.course_name()).isEqualTo("Backend Engineering");
                    assertThat(summary.enrollment_status()).isEqualTo("ACTIVE");
                });
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void enrollStudentRejectsUnapprovedCourseContent() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classDefinitionUuid,
                        courseUuid,
                        null,
                        "Sample Class",
                        null,
                        null,
                        null,
                        20,
                        true
                )));
        when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(false);

        assertThatThrownBy(() -> timetableService.enrollStudent(
                new apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO(classDefinitionUuid, studentUuid)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved for enrollment");

        verifyNoInteractions(commercePaywallService);
    }

    private ScheduledInstance buildScheduledInstance(UUID instructorUuid, SchedulingStatus status) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setUuid(UUID.randomUUID());
        instance.setInstructorUuid(instructorUuid);
        instance.setClassDefinitionUuid(UUID.randomUUID());
        instance.setStartTime(LocalDateTime.now().plusHours(1));
        instance.setEndTime(LocalDateTime.now().plusHours(2));
        instance.setTimezone("UTC");
        instance.setTitle("Sample Class");
        instance.setLocationType("ONLINE");
        instance.setMaxParticipants(25);
        instance.setStatus(status);
        return instance;
    }

    private Enrollment buildEnrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUuid(UUID.randomUUID());
        enrollment.setScheduledInstanceUuid(UUID.randomUUID());
        enrollment.setStudentUuid(UUID.randomUUID());
        enrollment.setStatus(status);
        return enrollment;
    }
}
