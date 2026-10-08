package apps.sarafrika.elimika.notifications.integration;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.dto.NotificationActionResultDTO;
import apps.sarafrika.elimika.notifications.model.UserNotification;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.notifications.service.impl.UserNotificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The bulk popup_seen action stamps many notifications in one UPDATE and never crosses recipients. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({UserNotificationServiceImpl.class, ObjectMapper.class})
@DisplayName("Bulk popup_seen notification action")
class BulkPopupSeenIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @MockitoBean private NotificationPreferencesService preferencesService;

    @Autowired private UserNotificationServiceImpl service;
    @Autowired private UserNotificationRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();

    @BeforeEach
    void relaxForeignKeys() {
        jdbc.execute("SET LOCAL session_replication_role = replica");
    }

    @Test
    @DisplayName("explicit uuids: only the caller's unseen notifications are stamped")
    void explicitUuidsAreScopedToCaller() {
        UserNotification mine = save(me, "student", NotificationPresentation.POPUP);
        UserNotification mineInbox = save(me, "student", NotificationPresentation.INBOX);
        UserNotification theirs = save(someoneElse, "student", NotificationPresentation.POPUP);

        NotificationActionResultDTO result = service.applyBulkAction(me, null, "popup_seen", null, null, null,
                List.of(mine.getUuid(), mineInbox.getUuid(), theirs.getUuid()));

        assertThat(result.action()).isEqualTo("popup_seen");
        assertThat(result.affectedCount()).isEqualTo(2);
        assertThat(seenAt(mine)).isNotNull();
        assertThat(seenAt(mineInbox)).isNotNull();
        assertThat(seenAt(theirs)).isNull();
    }

    @Test
    @DisplayName("no uuids: every unseen POPUP for the caller's domain (plus account-wide) is stamped")
    void allUnseenPopupsForDomain() {
        UserNotification studentPopup = save(me, "student", NotificationPresentation.POPUP);
        UserNotification accountPopup = save(me, null, NotificationPresentation.POPUP);
        UserNotification instructorPopup = save(me, "instructor", NotificationPresentation.POPUP);
        UserNotification studentInbox = save(me, "student", NotificationPresentation.INBOX);
        UserNotification theirs = save(someoneElse, "student", NotificationPresentation.POPUP);

        NotificationActionResultDTO result = service.applyBulkAction(me, "student", "popup_seen", null, null, null, null);

        assertThat(result.affectedCount()).isEqualTo(2);
        assertThat(seenAt(studentPopup)).isNotNull();
        assertThat(seenAt(accountPopup)).isNotNull();
        assertThat(seenAt(instructorPopup)).isNull();
        assertThat(seenAt(studentInbox)).isNull();
        assertThat(seenAt(theirs)).isNull();
    }

    @Test
    @DisplayName("already-seen popups keep their original timestamp and are not counted")
    void alreadySeenIsIdempotent() {
        UserNotification seen = save(me, "student", NotificationPresentation.POPUP);
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
        seen.setPopupSeenAt(original);
        repository.saveAndFlush(seen);

        NotificationActionResultDTO result = service.applyBulkAction(me, null, "popup_seen", null, null, null,
                List.of(seen.getUuid()));

        assertThat(result.affectedCount()).isZero();
        assertThat(seenAt(seen)).isEqualTo(original);
    }

    @Test
    @DisplayName("oversized uuid lists and unknown actions are rejected")
    void rejectsBadInput() {
        List<UUID> tooMany = IntStream.range(0, 201).mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> service.applyBulkAction(me, null, "popup_seen", null, null, null, tooMany))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.applyBulkAction(me, null, "archive_all", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private UserNotification save(UUID recipient, String domain, NotificationPresentation presentation) {
        UserNotification notification = UserNotification.create(
                recipient, domain, UUID.randomUUID(), NotificationType.CLASS_SCHEDULE_UPDATED,
                NotificationPriority.NORMAL, presentation, "Title", "Body", null, "{}",
                UUID.randomUUID().toString(), LocalDateTime.now());
        // JPA auditing is outside the slice, so stamp the audit columns by hand.
        notification.setCreatedDate(LocalDateTime.now());
        notification.setCreatedBy("test");
        return repository.saveAndFlush(notification);
    }

    private LocalDateTime seenAt(UserNotification notification) {
        entityManager.clear();
        return repository.findById(notification.getId()).orElseThrow().getPopupSeenAt();
    }
}
