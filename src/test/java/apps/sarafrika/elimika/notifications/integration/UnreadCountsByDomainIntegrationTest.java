package apps.sarafrika.elimika.notifications.integration;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.model.UserNotification;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository;
import apps.sarafrika.elimika.notifications.service.impl.UserNotificationServiceImpl;
import apps.sarafrika.elimika.notifications.spi.DomainNotificationCounts;
import apps.sarafrika.elimika.notifications.spi.NotificationCountsLookupService;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import apps.sarafrika.elimika.shared.config.JpaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The grouped unread-count query against real PostgreSQL and the real migrations. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = NotificationCountsLookupService.class))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaConfig.class)
class UnreadCountsByDomainIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private UserNotificationRepository repository;
    @Autowired
    private NotificationCountsLookupService countsLookupService;

    @Test
    void perDomainBadgesMatchTheSingleDomainCountsEndpoint() {
        UUID recipient = UUID.randomUUID();
        save(recipient, null, NotificationPresentation.POPUP, false, false);
        save(recipient, "student", NotificationPresentation.POPUP, false, false);
        save(recipient, "student", NotificationPresentation.POPUP, true, false);
        save(recipient, "student", NotificationPresentation.INBOX, false, true);
        save(recipient, "instructor", NotificationPresentation.INBOX, false, false);
        save(UUID.randomUUID(), "student", NotificationPresentation.INBOX, false, false);

        UnreadNotificationSummary summary = countsLookupService.summarizeUnread(recipient, List.of("student", "admin"));

        assertThat(summary.unreadCount()).isEqualTo(4);
        assertThat(summary.popupCount()).isEqualTo(2);
        assertThat(summary.byDomain())
                .containsEntry("student", new DomainNotificationCounts(3, 2))
                .containsEntry("instructor", new DomainNotificationCounts(2, 1))
                .containsEntry("admin", new DomainNotificationCounts(1, 1));
        var single = new UserNotificationServiceImpl(repository, null, new ObjectMapper()).getCounts(recipient, "student");
        assertThat(single.unreadCount()).isEqualTo(3);
        assertThat(single.popupCount()).isEqualTo(2);
    }

    private void save(UUID recipient, String domain, NotificationPresentation presentation, boolean popupSeen, boolean read) {
        UserNotification notification = UserNotification.create(recipient, domain, UUID.randomUUID(),
                NotificationType.values()[0], NotificationPriority.NORMAL, presentation, "Title", "Body",
                null, null, null, LocalDateTime.now());
        if (popupSeen) {
            notification.markPopupSeen(LocalDateTime.now());
        }
        if (read) {
            notification.markRead(LocalDateTime.now());
        }
        repository.saveAndFlush(notification);
    }
}
