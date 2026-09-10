package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassMarketplaceJobExpirySchedulerTest {

    @Mock
    private ClassMarketplaceJobRepository jobRepository;
    @Mock
    private ClassMarketplaceJobApplicationRepository applicationRepository;
    @Mock
    private ResourceBookingService resourceBookingService;
    @Mock
    private InstructorTimeHoldService instructorTimeHoldService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClassMarketplaceJobExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ClassMarketplaceJobExpiryScheduler(
                jobRepository, applicationRepository, resourceBookingService,
                instructorTimeHoldService, userLookupService, instructorLookupService, eventPublisher);
    }

    @Test
    void expiresLapsedJobsReleasesHoldsAndNotifiesCreator() throws Exception {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setTitle("Weekend Bootcamp");
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setCreatedBy("manager@org.test");
        UUID creatorUuid = UUID.randomUUID();

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(job));
        when(userLookupService.findUserUuidByEmail("manager@org.test")).thenReturn(Optional.of(creatorUuid));

        invokeExpire();

        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.EXPIRED);
        verify(resourceBookingService).releaseHoldsForJob(job.getUuid(), "Job expired");
        verify(jobRepository).saveAll(List.of(job));
        verify(eventPublisher).publishEvent(any(NotificationRequestedEvent.class));
    }

    @Test
    void unresolvableCreatorSkipsNotificationButStillExpires() throws Exception {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setCreatedBy("unknown@org.test");

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(job));
        when(userLookupService.findUserUuidByEmail("unknown@org.test")).thenReturn(Optional.empty());

        invokeExpire();

        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.EXPIRED);
        verify(resourceBookingService).releaseHoldsForJob(job.getUuid(), "Job expired");
        verify(eventPublisher, never()).publishEvent(any(NotificationRequestedEvent.class));
    }

    @Test
    void noLapsedJobsIsNoOp() throws Exception {
        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of());

        invokeExpire();

        verify(jobRepository, never()).saveAll(anyList());
        verify(resourceBookingService, never()).releaseHoldsForJob(any(), any());
    }

    @Test
    void outstandingApplicantsAreClosedOutAndTold() throws Exception {
        ClassMarketplaceJob job = openJob("Weekend Bootcamp");
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        ClassMarketplaceJobApplication pending =
                application(job.getUuid(), instructorUuid, ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(job));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), anyList()))
                .thenReturn(new ArrayList<>(List.of(pending)));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(userLookupService.getUserEmail(instructorUserUuid)).thenReturn(Optional.of("tutor@example.test"));
        when(userLookupService.getUserFullName(instructorUserUuid)).thenReturn(Optional.of("Ada Tutor"));
        when(userLookupService.findUserUuidByEmail("manager@org.test")).thenReturn(Optional.empty());

        invokeExpire();

        // Left PENDING/SHORTLISTED an applicant waits on an answer that is never coming.
        assertThat(pending.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
        assertThat(pending.getReviewNotes()).contains("expired");
        assertThat(pending.getReviewedAt()).isNotNull();
        verify(applicationRepository).saveAll(anyList());
        // One in-app and one email for the applicant; the creator was unresolvable.
        verify(eventPublisher, times(2)).publishEvent(any(NotificationRequestedEvent.class));
    }

    @Test
    void expiringAnAwaitingClassJobKeepsTheHireItNames() throws Exception {
        ClassMarketplaceJob job = openJob("Contracted Bootcamp");
        job.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);
        UUID instructorUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        job.setAssignedInstructorUuid(instructorUuid);
        job.setAssignedApplicationUuid(applicationUuid);

        ClassMarketplaceJobApplication assigned =
                application(job.getUuid(), instructorUuid, ClassMarketplaceJobApplicationStatus.ASSIGNED);
        assigned.setUuid(applicationUuid);

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(job));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), anyList()))
                .thenReturn(new ArrayList<>(List.of(assigned)));

        invokeExpire();

        // The window closing ends recruitment, and recruitment is over. The class still has to be
        // created, and only AWAITING_CLASS can create one, so the sweep must leave the job alone.
        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.AWAITING_CLASS);
        assertThat(assigned.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.ASSIGNED);
        assertThat(job.getAssignedApplicationUuid()).isEqualTo(applicationUuid);
        assertThat(job.getAssignedInstructorUuid()).isEqualTo(instructorUuid);
        verify(applicationRepository, never()).saveAll(anyList());
        // The class needs those slots, so releasing them would hand the hire's time to somebody else.
        verify(resourceBookingService, never()).releaseHoldsForJob(eq(job.getUuid()), anyString());
        verify(instructorTimeHoldService, never()).releaseHoldsForJob(eq(job.getUuid()), anyString());
    }

    @Test
    void expiringAJobHoldingAHireClosesOnlyTheRestOfTheFunnel() throws Exception {
        ClassMarketplaceJob job = openJob("Weekend Bootcamp");
        UUID hiredInstructorUuid = UUID.randomUUID();
        UUID passedOverInstructorUuid = UUID.randomUUID();

        ClassMarketplaceJobApplication hired =
                application(job.getUuid(), hiredInstructorUuid, ClassMarketplaceJobApplicationStatus.HIRED);
        hired.setReviewNotes("Hired for the September intake");
        ClassMarketplaceJobApplication shortlisted =
                application(job.getUuid(), passedOverInstructorUuid, ClassMarketplaceJobApplicationStatus.SHORTLISTED);

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(job));
        when(applicationRepository.findByJobUuidAndStatusIn(eq(job.getUuid()), anyList()))
                .thenReturn(new ArrayList<>(List.of(hired, shortlisted)));
        when(instructorLookupService.getInstructorUserUuid(passedOverInstructorUuid)).thenReturn(Optional.empty());

        invokeExpire();

        // The overnight sweep is what destroyed the evidence of a real hire; it must not again.
        assertThat(hired.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        assertThat(hired.getReviewNotes()).isEqualTo("Hired for the September intake");
        assertThat(shortlisted.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
        assertThat(job.getStatus()).isNotEqualTo(ClassMarketplaceJobStatus.EXPIRED);
        verify(applicationRepository).saveAll(List.of(shortlisted));
        // Only the passed-over applicant hears that the job lapsed; the hire is not told they lost it.
        verify(instructorLookupService, never()).getInstructorUserUuid(hiredInstructorUuid);
    }

    @Test
    void expiredJobReleasesInstructorHolds() throws Exception {
        ClassMarketplaceJob open = openJob("Weekend Bootcamp");
        ClassMarketplaceJob awaitingClass = openJob("Contracted Bootcamp");
        awaitingClass.setStatus(ClassMarketplaceJobStatus.AWAITING_CLASS);

        when(jobRepository.findExpiredOpenJobs(any(LocalDate.class))).thenReturn(List.of(open, awaitingClass));
        when(userLookupService.findUserUuidByEmail("manager@org.test")).thenReturn(Optional.empty());

        invokeExpire();

        // A lapsed job must free the applicants' diaries, not only the venue and equipment.
        verify(instructorTimeHoldService).releaseHoldsForJob(open.getUuid(), "Job expired");
        verify(instructorTimeHoldService)
                .releaseHoldsForJob(awaitingClass.getUuid(), "Job expired before its class was created");
    }

    private ClassMarketplaceJob openJob(String title) {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setTitle(title);
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setCreatedBy("manager@org.test");
        return job;
    }

    private ClassMarketplaceJobApplication application(UUID jobUuid,
                                                       UUID instructorUuid,
                                                       ClassMarketplaceJobApplicationStatus status) {
        ClassMarketplaceJobApplication application = new ClassMarketplaceJobApplication();
        application.setUuid(UUID.randomUUID());
        application.setJobUuid(jobUuid);
        application.setInstructorUuid(instructorUuid);
        application.setStatus(status);
        return application;
    }

    private void invokeExpire() throws Exception {
        Method method = ClassMarketplaceJobExpiryScheduler.class.getDeclaredMethod("expireLapsedJobs");
        method.setAccessible(true);
        method.invoke(scheduler);
    }
}
