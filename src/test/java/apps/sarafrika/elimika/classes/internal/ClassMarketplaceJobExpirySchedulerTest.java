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
                userLookupService, instructorLookupService, eventPublisher);
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
    void expiringAnAwaitingClassJobClearsTheHireItStillNames() throws Exception {
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
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.empty());
        when(userLookupService.findUserUuidByEmail("manager@org.test")).thenReturn(Optional.empty());

        invokeExpire();

        assertThat(job.getStatus()).isEqualTo(ClassMarketplaceJobStatus.EXPIRED);
        assertThat(assigned.getStatus()).isEqualTo(ClassMarketplaceJobApplicationStatus.NOT_SELECTED);
        // The job must not go on naming an instructor whose assignment it just closed.
        assertThat(job.getAssignedApplicationUuid()).isNull();
        assertThat(job.getAssignedInstructorUuid()).isNull();
        verify(resourceBookingService)
                .releaseHoldsForJob(job.getUuid(), "Job expired before its class was created");
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
