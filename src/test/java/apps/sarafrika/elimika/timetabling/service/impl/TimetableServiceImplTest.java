package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.shared.event.timetabling.ClassSessionCompletedEvent;
import apps.sarafrika.elimika.timetabling.internal.SchedulingEventListener.ScheduledInstanceCompletedEvent;
import apps.sarafrika.elimika.timetabling.internal.SchedulingEventListener.ScheduledInstanceStartedEvent;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceRescheduleRequestDTO;
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
import static org.mockito.Mockito.never;
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

    @Mock
    private StudentLookupService studentLookupService;

    @Mock
    private apps.sarafrika.elimika.tenancy.spi.UserLookupService userLookupService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private apps.sarafrika.elimika.resourcing.spi.ResourceBookingService resourceBookingService;

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
                availabilityService,
                studentLookupService,
                userLookupService,
                instructorLookupService,
                resourceBookingService
        );
    }

    @Test
    void startScheduledInstanceSetsOngoingStatusAndActualStartTime() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(enrollmentRepository.countEnrollmentsByScheduledInstanceAndStatus(instanceUuid, EnrollmentStatus.ENROLLED))
                .thenReturn(1L);
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledInstanceDTO result = timetableService.startScheduledInstance(instanceUuid);

        assertThat(result.status()).isEqualTo(SchedulingStatus.ONGOING);
        assertThat(result.startedAt()).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(SchedulingStatus.ONGOING);
        assertThat(instance.getStartedAt()).isNotNull();
        verify(applicationEventPublisher).publishEvent(any(ScheduledInstanceStartedEvent.class));
    }

    @Test
    void startScheduledInstanceDoesNotOverwriteExistingStartTime() {
        UUID instanceUuid = UUID.randomUUID();
        LocalDateTime existingStart = LocalDateTime.of(2026, 4, 28, 9, 0);
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.ONGOING);
        instance.setUuid(instanceUuid);
        instance.setStartedAt(existingStart);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));

        ScheduledInstanceDTO result = timetableService.startScheduledInstance(instanceUuid);

        assertThat(result.startedAt()).isEqualTo(existingStart);
        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
        verify(applicationEventPublisher, never()).publishEvent(any(ScheduledInstanceStartedEvent.class));
    }

    @Test
    void startScheduledInstanceRejectsCancelledInstance() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.CANCELLED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> timetableService.startScheduledInstance(instanceUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be started");

        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
    }

    @Test
    void startScheduledInstanceRejectsWhenNoStudentsAreEnrolled() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(enrollmentRepository.countEnrollmentsByScheduledInstanceAndStatus(instanceUuid, EnrollmentStatus.ENROLLED))
                .thenReturn(0L);

        assertThatThrownBy(() -> timetableService.startScheduledInstance(instanceUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one enrolled student");

        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
        verify(applicationEventPublisher, never()).publishEvent(any(ScheduledInstanceStartedEvent.class));
    }

    @Test
    void rescheduleScheduledInstanceUpdatesScheduledTimeAndIgnoresSelfOverlap() {
        UUID instanceUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);
        LocalDateTime newStart = LocalDateTime.of(2026, 6, 12, 9, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 6, 12, 11, 0);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(availabilityService.isInstructorAvailable(instructorUuid, newStart, newEnd)).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(instructorUuid, newStart, newEnd))
                .thenReturn(List.of(instance));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledInstanceDTO result = timetableService.rescheduleScheduledInstance(
                instanceUuid,
                new ScheduledInstanceRescheduleRequestDTO(newStart, newEnd, "Africa/Nairobi")
        );

        assertThat(result.uuid()).isEqualTo(instanceUuid);
        assertThat(result.startTime()).isEqualTo(newStart);
        assertThat(result.endTime()).isEqualTo(newEnd);
        assertThat(result.timezone()).isEqualTo("Africa/Nairobi");
        assertThat(instance.getStartTime()).isEqualTo(newStart);
        assertThat(instance.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void rescheduleScheduledInstancePropagatesNewWindowToResourceBookings() {
        UUID instanceUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);
        LocalDateTime newStart = LocalDateTime.of(2026, 6, 12, 9, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 6, 12, 11, 0);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(availabilityService.isInstructorAvailable(instructorUuid, newStart, newEnd)).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(instructorUuid, newStart, newEnd))
                .thenReturn(List.of());
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timetableService.rescheduleScheduledInstance(
                instanceUuid,
                new ScheduledInstanceRescheduleRequestDTO(newStart, newEnd, "UTC")
        );

        org.mockito.Mockito.verify(resourceBookingService).rescheduleInstanceBookings(instanceUuid, newStart, newEnd);
    }

    @Test
    void rescheduleScheduledInstanceAbortsWhenLinkedResourcesConflict() {
        UUID instanceUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);
        LocalDateTime originalStart = instance.getStartTime();
        LocalDateTime newStart = LocalDateTime.of(2026, 6, 12, 9, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 6, 12, 11, 0);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(availabilityService.isInstructorAvailable(instructorUuid, newStart, newEnd)).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(instructorUuid, newStart, newEnd))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new apps.sarafrika.elimika.resourcing.spi.ResourceBookingConflictException(
                        "Venue occupied",
                        apps.sarafrika.elimika.resourcing.spi.ResourceValidationReport.withConflicts(List.of())))
                .when(resourceBookingService).rescheduleInstanceBookings(instanceUuid, newStart, newEnd);

        assertThatThrownBy(() -> timetableService.rescheduleScheduledInstance(
                instanceUuid,
                new ScheduledInstanceRescheduleRequestDTO(newStart, newEnd, "UTC")))
                .isInstanceOf(apps.sarafrika.elimika.resourcing.spi.ResourceBookingConflictException.class);

        assertThat(instance.getStartTime()).isEqualTo(originalStart);
        org.mockito.Mockito.verify(scheduledInstanceRepository, org.mockito.Mockito.never())
                .save(any(ScheduledInstance.class));
    }

    @Test
    void cancelScheduledInstanceReleasesLinkedResourceBookings() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.findByScheduledInstanceUuidAndStatus(instanceUuid, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of());

        timetableService.cancelScheduledInstance(instanceUuid, "Venue unavailable");

        org.mockito.Mockito.verify(resourceBookingService)
                .releaseBookingsForInstance(instanceUuid, "Venue unavailable");
    }

    @Test
    void rescheduleScheduledInstanceRejectsCompletedInstance() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.COMPLETED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> timetableService.rescheduleScheduledInstance(
                instanceUuid,
                new ScheduledInstanceRescheduleRequestDTO(
                        LocalDateTime.of(2026, 6, 12, 9, 0),
                        LocalDateTime.of(2026, 6, 12, 11, 0),
                        null
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only scheduled instances can be rescheduled");

        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
        verifyNoInteractions(availabilityService);
    }

    @Test
    void rescheduleScheduledInstanceRejectsOtherInstructorOverlap() {
        UUID instanceUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        instance.setUuid(instanceUuid);
        ScheduledInstance overlapping = buildScheduledInstance(instructorUuid, SchedulingStatus.SCHEDULED);
        LocalDateTime newStart = LocalDateTime.of(2026, 6, 12, 9, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 6, 12, 11, 0);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(availabilityService.isInstructorAvailable(instructorUuid, newStart, newEnd)).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(instructorUuid, newStart, newEnd))
                .thenReturn(List.of(instance, overlapping));

        assertThatThrownBy(() -> timetableService.rescheduleScheduledInstance(
                instanceUuid,
                new ScheduledInstanceRescheduleRequestDTO(newStart, newEnd, null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");

        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
    }

    @Test
    void endScheduledInstanceSetsCompletedStatusAndActualConclusionTime() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.ONGOING);
        instance.setUuid(instanceUuid);
        instance.setStartedAt(LocalDateTime.of(2026, 4, 28, 9, 0));

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledInstanceDTO result = timetableService.endScheduledInstance(instanceUuid);

        assertThat(result.status()).isEqualTo(SchedulingStatus.COMPLETED);
        assertThat(result.concludedAt()).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(SchedulingStatus.COMPLETED);
        assertThat(instance.getConcludedAt()).isNotNull();
        verify(applicationEventPublisher).publishEvent(any(ScheduledInstanceCompletedEvent.class));
        // The internal event is invisible outside timetabling. The shared one is what the payout
        // ledger hears, and it is the only reason the instructor gets paid for this session.
        verify(applicationEventPublisher).publishEvent(any(ClassSessionCompletedEvent.class));
    }

    /**
     * The status-update route can drive an instance to COMPLETED without going through
     * {@code endScheduledInstance}, and used to do so in silence. A session delivered that way earns
     * the instructor exactly the same, so it has to announce itself too.
     */
    @Test
    void updatingStatusToCompletedAnnouncesTheDeliveredSession() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.ONGOING);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timetableService.updateScheduledInstanceStatus(instanceUuid, "COMPLETED");

        verify(applicationEventPublisher).publishEvent(any(ClassSessionCompletedEvent.class));
    }

    @Test
    void reassertingCompletedStatusDoesNotAnnounceTheSessionAgain() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.COMPLETED);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timetableService.updateScheduledInstanceStatus(instanceUuid, "COMPLETED");

        verify(applicationEventPublisher, never()).publishEvent(any(ClassSessionCompletedEvent.class));
    }

    @Test
    void endScheduledInstanceRequiresExplicitStart() {
        UUID instanceUuid = UUID.randomUUID();
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.ONGOING);
        instance.setUuid(instanceUuid);

        when(scheduledInstanceRepository.findByUuid(instanceUuid)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> timetableService.endScheduledInstance(instanceUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be started");

        verify(scheduledInstanceRepository, never()).save(any(ScheduledInstance.class));
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
                        null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR,
                        null,
                        null,
                        20,
                        true,
                        null
                )));
        when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(false);

        assertThatThrownBy(() -> timetableService.enrollStudent(
                new apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO(classDefinitionUuid, studentUuid)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved for enrollment");

        verifyNoInteractions(commercePaywallService);
    }

    @Test
    void scheduleClassDenormalizesClassDetailsFromSnapshot() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        var request = new apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO(
                classDefinitionUuid,
                instructorUuid,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "UTC");

        when(availabilityService.isInstructorAvailable(eq(instructorUuid), any(), any())).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(eq(instructorUuid), any(), any()))
                .thenReturn(List.of());
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classDefinitionUuid,
                        UUID.randomUUID(),
                        null,
                        "Weekend Data Analysis Bootcamp",
                        null,
                        null,
                        null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR,
                        null,
                        apps.sarafrika.elimika.shared.enums.LocationType.HYBRID,
                        24,
                        true,
                        null
                )));
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledInstanceDTO result = timetableService.scheduleClass(request);

        assertThat(result.title()).isEqualTo("Weekend Data Analysis Bootcamp");
        assertThat(result.locationType()).isEqualTo("HYBRID");
        assertThat(result.maxParticipants()).isEqualTo(24);
    }

    @Test
    void scheduleClassFallsBackToPlaceholdersWhenSnapshotMissing() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        var request = new apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO(
                classDefinitionUuid,
                instructorUuid,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "UTC");

        when(availabilityService.isInstructorAvailable(eq(instructorUuid), any(), any())).thenReturn(true);
        when(scheduledInstanceRepository.findOverlappingInstancesForInstructor(eq(instructorUuid), any(), any()))
                .thenReturn(List.of());
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid)).thenReturn(Optional.empty());
        when(scheduledInstanceRepository.save(any(ScheduledInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledInstanceDTO result = timetableService.scheduleClass(request);

        assertThat(result.title()).startsWith("Class: ");
        assertThat(result.locationType()).isEqualTo("ONLINE");
        assertThat(result.maxParticipants()).isEqualTo(25);
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


    // ── Enrolment eligibility is judged from our own records, before any money moves ────────────

    private void ageLimitsAre(UUID classDefinitionUuid, UUID courseUuid, Integer min, Integer max) {
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classDefinitionUuid, courseUuid, null, "Dairy", null,
                        null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, 20, true, 30)));
        when(courseInfoService.getAgeLimits(courseUuid))
                .thenReturn(Optional.of(new apps.sarafrika.elimika.course.spi.CourseInfoService.AgeLimits(min, max)));
    }

    @Test
    void aStudentBelowTheMinimumAgeIsRefusedWithTheirActualAge() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        ageLimitsAre(classUuid, courseUuid, 18, null);
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(userUuid));
        when(userLookupService.getUserDateOfBirth(userUuid))
                .thenReturn(Optional.of(java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusYears(12)));

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.studentAge()).isEqualTo(12);
        assertThat(eligibility.minimumAge()).isEqualTo(18);
        assertThat(eligibility.ageRequirementMet()).isFalse();
        assertThat(eligibility.reason()).contains("ages 18 and over").contains("you are 12");
    }

    @Test
    void aMissingDateOfBirthBlocksRatherThanAssumingAnAge() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        ageLimitsAre(classUuid, courseUuid, 18, null);
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(userUuid));
        when(userLookupService.getUserDateOfBirth(userUuid)).thenReturn(Optional.empty());

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.dateOfBirthOnFile()).isFalse();
        assertThat(eligibility.reason()).contains("date of birth");
    }

    @Test
    void aCourseWithNoAgeLimitDoesNotNeedADateOfBirth() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        when(classDefinitionLookupService.findByUuid(classUuid))
                .thenReturn(Optional.of(new apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classUuid, courseUuid, null, "Dairy", null, null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, 20, true, 30)));
        when(courseInfoService.getAgeLimits(courseUuid)).thenReturn(Optional.empty());
        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of());
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of());

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        // No age limit means no date of birth is demanded; the only thing left to report is that the
        // class has nothing scheduled yet.
        assertThat(eligibility.dateOfBirthOnFile()).isTrue();
        assertThat(eligibility.ageRequirementMet()).isTrue();
        assertThat(eligibility.reason()).contains("no scheduled sessions");
    }

    @Test
    void aSeatHeldAtCheckoutIsNotReadBackAsAnEnrolment() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        ScheduledInstance first = instanceOf(classUuid);
        ScheduledInstance second = instanceOf(classUuid);
        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid))
                .thenReturn(List.of(first, second));
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(
                heldSeat(studentUuid, first.getUuid(), LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(10)),
                heldSeat(studentUuid, second.getUuid(), LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(10))));

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        // The hold taken moments earlier at checkout is this buyer's own seat. Reading it back as an
        // enrolment refused the purchase that took it, which stranded the order and the money.
        assertThat(eligibility.alreadyEnrolled()).isFalse();
        assertThat(eligibility.seatsAvailable()).isTrue();
        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    void enrolmentEligibilityReportsScheduleConflictsBeforeCheckout() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        ScheduledInstance target = instanceOf(classUuid);
        Enrollment conflicting = heldSeat(studentUuid, UUID.randomUUID(), null);
        conflicting.setStatus(EnrollmentStatus.ENROLLED);

        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(target));
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(conflicting));
        when(scheduledInstanceRepository.findByUuid(target.getUuid())).thenReturn(Optional.of(target));
        when(enrollmentRepository.countActiveEnrollmentsByScheduledInstance(target.getUuid())).thenReturn(0L);
        when(enrollmentRepository.findOverlappingEnrollmentsForStudent(
                studentUuid,
                target.getStartTime(),
                target.getEndTime()))
                .thenReturn(List.of(conflicting));

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).contains("overlaps with another class");
    }

    @Test
    void enrollStudentPromotesCheckoutReservationInsteadOfConflictingWithIt() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        when(courseInfoService.isCourseApproved(courseUuid)).thenReturn(true);
        ScheduledInstance target = instanceOf(classUuid);
        Enrollment reservation = heldSeat(
                studentUuid,
                target.getUuid(),
                LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(10));

        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(target));
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(reservation));
        when(enrollmentRepository.findOverlappingEnrollmentsForStudent(
                studentUuid,
                target.getStartTime(),
                target.getEndTime()))
                .thenReturn(List.of(reservation));
        when(enrollmentRepository.save(reservation)).thenReturn(reservation);

        List<EnrollmentDTO> result = timetableService.enrollStudent(
                new apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO(classUuid, studentUuid));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).uuid()).isEqualTo(reservation.getUuid());
        assertThat(reservation.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(reservation.getReservedUntil()).isNull();
        verify(enrollmentRepository).save(reservation);
    }

    @Test
    void aHoldThatLapsedDoesNotLockTheStudentOutOfTheClass() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        ScheduledInstance instance = instanceOf(classUuid);
        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(instance));
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(
                heldSeat(studentUuid, instance.getUuid(), LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(6))));
        when(scheduledInstanceRepository.findByUuid(instance.getUuid())).thenReturn(Optional.of(instance));
        when(enrollmentRepository.countActiveEnrollmentsByScheduledInstance(instance.getUuid())).thenReturn(1L);

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        // A payment that never resolved leaves the hold behind. Treating it as a standing claim would
        // bar this learner from ever buying the class again.
        assertThat(eligibility.alreadyEnrolled()).isFalse();
        assertThat(eligibility.eligible()).isTrue();
    }

    @Test
    void aStudentWithASeatOnEveryInstanceIsStillTurnedAway() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        ScheduledInstance instance = instanceOf(classUuid);
        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(instance));
        Enrollment enrolled = heldSeat(studentUuid, instance.getUuid(), null);
        enrolled.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(enrolled));

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.alreadyEnrolled()).isTrue();
        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).contains("already enrolled");
    }

    @Test
    void aCancelledSeatLetsTheStudentBuyTheClassAgain() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        noAgeLimit(classUuid, courseUuid);
        ScheduledInstance instance = instanceOf(classUuid);
        when(scheduledInstanceRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(instance));
        Enrollment cancelled = heldSeat(studentUuid, instance.getUuid(), null);
        cancelled.setStatus(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findByStudentUuid(studentUuid)).thenReturn(List.of(cancelled));
        when(scheduledInstanceRepository.findByUuid(instance.getUuid())).thenReturn(Optional.of(instance));
        when(enrollmentRepository.countActiveEnrollmentsByScheduledInstance(instance.getUuid())).thenReturn(0L);

        var eligibility = timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.alreadyEnrolled()).isFalse();
        assertThat(eligibility.eligible()).isTrue();
    }

    private void noAgeLimit(UUID classDefinitionUuid, UUID courseUuid) {
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(new apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classDefinitionUuid, courseUuid, null, "Dairy", null,
                        null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, 20, true, 30)));
        when(courseInfoService.getAgeLimits(courseUuid)).thenReturn(Optional.empty());
    }

    private ScheduledInstance instanceOf(UUID classDefinitionUuid) {
        ScheduledInstance instance = buildScheduledInstance(UUID.randomUUID(), SchedulingStatus.SCHEDULED);
        instance.setClassDefinitionUuid(classDefinitionUuid);
        return instance;
    }

    private Enrollment heldSeat(UUID studentUuid, UUID scheduledInstanceUuid, LocalDateTime reservedUntil) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUuid(UUID.randomUUID());
        enrollment.setScheduledInstanceUuid(scheduledInstanceUuid);
        enrollment.setStudentUuid(studentUuid);
        enrollment.setStatus(EnrollmentStatus.RESERVED);
        enrollment.setReservedUntil(reservedUntil);
        return enrollment;
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
