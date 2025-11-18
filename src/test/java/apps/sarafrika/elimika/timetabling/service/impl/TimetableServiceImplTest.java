package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AgeVerificationService ageVerificationService;

    @Mock
    private apps.sarafrika.elimika.commerce.purchase.spi.paywall.CommercePaywallService commercePaywallService;

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
                ageVerificationService,
                commercePaywallService
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
