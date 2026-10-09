package apps.sarafrika.elimika.notifications.service.impl;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository.UnreadCountsByDomain;
import apps.sarafrika.elimika.notifications.spi.DomainNotificationCounts;
import apps.sarafrika.elimika.notifications.spi.NotificationCountsLookupService;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Answers every domain's badge from one GROUP BY instead of one count query per domain. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class NotificationCountsLookupServiceImpl implements NotificationCountsLookupService {

    private final UserNotificationRepository userNotificationRepository;

    @Override
    public UnreadNotificationSummary summarizeUnread(UUID recipientUuid, Collection<String> domains) {
        Map<String, DomainNotificationCounts> rows = new HashMap<>();
        long unreadTotal = 0;
        long popupTotal = 0;
        for (UnreadCountsByDomain row : userNotificationRepository.countUnreadByDomain(
                recipientUuid, UserNotificationStatus.UNREAD, NotificationPresentation.POPUP)) {
            long unread = row.getUnreadCount() == null ? 0 : row.getUnreadCount();
            long popup = row.getPopupCount() == null ? 0 : row.getPopupCount();
            rows.merge(normalizeDomain(row.getDomain()), new DomainNotificationCounts(unread, popup), NotificationCountsLookupServiceImpl::add);
            unreadTotal += unread;
            popupTotal += popup;
        }

        DomainNotificationCounts accountLevel = rows.getOrDefault(null, new DomainNotificationCounts(0, 0));
        Set<String> keys = new LinkedHashSet<>();
        if (domains != null) {
            domains.stream().map(NotificationCountsLookupServiceImpl::normalizeDomain).forEach(keys::add);
        }
        keys.addAll(rows.keySet());
        keys.remove(null);

        Map<String, DomainNotificationCounts> byDomain = new TreeMap<>();
        for (String key : keys) {
            byDomain.put(key, add(rows.getOrDefault(key, new DomainNotificationCounts(0, 0)), accountLevel));
        }
        return new UnreadNotificationSummary(unreadTotal, popupTotal, byDomain);
    }

    private static DomainNotificationCounts add(DomainNotificationCounts a, DomainNotificationCounts b) {
        return new DomainNotificationCounts(a.unreadCount() + b.unreadCount(), a.popupCount() + b.popupCount());
    }

    // Same normalisation as UserNotificationServiceImpl, so the badges match /notifications/counts.
    private static String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        return "organization".equals(normalized) ? "organisation" : normalized;
    }
}
