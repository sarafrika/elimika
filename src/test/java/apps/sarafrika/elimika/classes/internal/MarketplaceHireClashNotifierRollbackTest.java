package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

/** Notification requests dispatch after commit, as the notifications module's listener does; a refused hire never commits. */
@SpringJUnitConfig(MarketplaceHireClashNotifierRollbackTest.Config.class)
class MarketplaceHireClashNotifierRollbackTest {

    private static final ClassSchedulingConflictDTO CLASH = new ClassSchedulingConflictDTO(
            LocalDateTime.of(2026, 6, 6, 14, 0), LocalDateTime.of(2026, 6, 6, 16, 0),
            List.of("Instructor is already committed to another class job in this window"));

    @Autowired
    private MarketplaceHireClashNotifier notifier;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DispatchedNotifications dispatched;

    @BeforeEach
    void clear() {
        dispatched.events.clear();
    }

    @Test
    void alertsForARefusedHireAreDispatchedEvenThoughTheHireRollsBack() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(hire -> {
            notifier.notifyHireBlocked(job(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(CLASH));
            throw new SchedulingConflictException("Clash", List.of(CLASH));
        })).isInstanceOf(SchedulingConflictException.class);

        assertThat(dispatched.events).extracting(NotificationRequestedEvent::notificationType)
                .containsExactlyInAnyOrder(
                        "CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_ORGANISATION",
                        "CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_INSTRUCTOR");
    }

    @Test
    void aRequestPublishedInsideTheRefusedHireItselfWouldNeverBeDispatched() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(hire -> {
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(UUID.randomUUID(),
                    "CLASS_MARKETPLACE_JOB_HIRE_BLOCKED_ORGANISATION", "INBOX", "Hire blocked", "Clash", null,
                    null, null));
            throw new SchedulingConflictException("Clash", List.of(CLASH));
        })).isInstanceOf(SchedulingConflictException.class);

        assertThat(dispatched.events).isEmpty();
    }

    private static ClassMarketplaceJob job() {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setUuid(UUID.randomUUID());
        job.setOrganisationUuid(UUID.randomUUID());
        job.setTitle("Grade 5 Piano");
        return job;
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new InMemoryTransactionManager();
        }

        @Bean
        MarketplaceHireClashNotifier marketplaceHireClashNotifier(ApplicationEventPublisher eventPublisher) {
            InstructorLookupService instructorLookupService = Mockito.mock(InstructorLookupService.class);
            Mockito.when(instructorLookupService.getInstructorUserUuid(any()))
                    .thenReturn(Optional.of(UUID.randomUUID()));
            return new MarketplaceHireClashNotifier(Mockito.mock(UserLookupService.class), instructorLookupService,
                    Mockito.mock(OrganisationLookupService.class), eventPublisher);
        }

        @Bean
        DispatchedNotifications dispatchedNotifications() {
            return new DispatchedNotifications();
        }
    }

    static class DispatchedNotifications {

        final List<NotificationRequestedEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        public void onDispatch(NotificationRequestedEvent event) {
            events.add(event);
        }
    }

    /** Just enough of a transaction manager to suspend, resume, commit and roll back synchronisations. */
    static class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

        private final Object resourceKey = new Object();

        private record Transaction(boolean existing) {
        }

        @Override
        protected Object doGetTransaction() {
            return new Transaction(TransactionSynchronizationManager.hasResource(resourceKey));
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((Transaction) transaction).existing();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            TransactionSynchronizationManager.bindResource(resourceKey, new Object());
        }

        @Override
        protected Object doSuspend(Object transaction) {
            return TransactionSynchronizationManager.unbindResource(resourceKey);
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            TransactionSynchronizationManager.bindResource(resourceKey, suspendedResources);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            TransactionSynchronizationManager.unbindResourceIfPossible(resourceKey);
        }
    }
}
