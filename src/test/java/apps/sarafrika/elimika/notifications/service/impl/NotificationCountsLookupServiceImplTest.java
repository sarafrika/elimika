package apps.sarafrika.elimika.notifications.service.impl;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository.UnreadCountsByDomain;
import apps.sarafrika.elimika.notifications.spi.DomainNotificationCounts;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCountsLookupServiceImplTest {

    private static final UUID RECIPIENT = UUID.randomUUID();

    private final UserNotificationRepository repository = Mockito.mock(UserNotificationRepository.class);
    private final NotificationCountsLookupServiceImpl service = new NotificationCountsLookupServiceImpl(repository);

    @Test
    void foldsAccountLevelCountsIntoEveryDomainFromOneQuery() {
        when(repository.countUnreadByDomain(RECIPIENT, UserNotificationStatus.UNREAD, NotificationPresentation.POPUP))
                .thenReturn(List.of(row(null, 2, 1), row("student", 3, 0), row("Organization", 4, 2)));

        UnreadNotificationSummary summary = service.summarizeUnread(RECIPIENT, List.of("student", "instructor"));

        assertThat(summary.unreadCount()).isEqualTo(9);
        assertThat(summary.popupCount()).isEqualTo(3);
        assertThat(summary.byDomain())
                .containsEntry("student", new DomainNotificationCounts(5, 1))
                .containsEntry("instructor", new DomainNotificationCounts(2, 1))
                .containsEntry("organisation", new DomainNotificationCounts(6, 3))
                .hasSize(3);
        verify(repository).countUnreadByDomain(RECIPIENT, UserNotificationStatus.UNREAD, NotificationPresentation.POPUP);
    }

    @Test
    void requestedDomainsReadZeroWhenNothingIsUnread() {
        when(repository.countUnreadByDomain(RECIPIENT, UserNotificationStatus.UNREAD, NotificationPresentation.POPUP))
                .thenReturn(List.of());

        UnreadNotificationSummary summary = service.summarizeUnread(RECIPIENT, List.of("admin"));

        assertThat(summary.unreadCount()).isZero();
        assertThat(summary.byDomain()).containsEntry("admin", new DomainNotificationCounts(0, 0)).hasSize(1);
    }

    private static UnreadCountsByDomain row(String domain, long unread, long popup) {
        return new UnreadCountsByDomain() {
            @Override
            public String getDomain() {
                return domain;
            }

            @Override
            public Long getUnreadCount() {
                return unread;
            }

            @Override
            public Long getPopupCount() {
                return popup;
            }
        };
    }
}
