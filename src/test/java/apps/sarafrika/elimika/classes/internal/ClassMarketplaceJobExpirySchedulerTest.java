package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassMarketplaceJobExpirySchedulerTest {

    @Mock
    private ClassMarketplaceJobRepository jobRepository;
    @Mock
    private ResourceBookingService resourceBookingService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClassMarketplaceJobExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ClassMarketplaceJobExpiryScheduler(
                jobRepository, resourceBookingService, userLookupService, eventPublisher);
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

    private void invokeExpire() throws Exception {
        Method method = ClassMarketplaceJobExpiryScheduler.class.getDeclaredMethod("expireLapsedJobs");
        method.setAccessible(true);
        method.invoke(scheduler);
    }
}
