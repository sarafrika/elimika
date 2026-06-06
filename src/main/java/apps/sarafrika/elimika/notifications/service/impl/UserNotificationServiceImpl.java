package apps.sarafrika.elimika.notifications.service.impl;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.dto.NotificationActionResultDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationCountsDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationDTO;
import apps.sarafrika.elimika.notifications.model.UserNotification;
import apps.sarafrika.elimika.notifications.model.UserNotificationRepository;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.notifications.service.UserNotificationService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationPreferencesService preferencesService;
    private final ObjectMapper objectMapper;

    @Override
    public NotificationDTO createFromEvent(NotificationEvent event) {
        if (event.getRecipientId() == null) {
            throw new IllegalArgumentException("Notification recipient is required");
        }
        if (!preferencesService.isNotificationEnabled(event.getRecipientId(), event.getNotificationType(), "in_app")) {
            log.debug("In-app notification {} blocked by user preferences", event.getNotificationId());
            return null;
        }

        String dedupeKey = resolveDedupeKey(event);
        Optional<UserNotification> existing = userNotificationRepository.findByRecipientUuidAndDedupeKey(
                event.getRecipientId(),
                dedupeKey
        );
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }

        UserNotification notification = UserNotification.create(
                event.getRecipientId(),
                event.getNotificationId(),
                event.getNotificationType(),
                event.getPriority() != null ? event.getPriority() : NotificationPriority.NORMAL,
                event.getPresentation() != null ? event.getPresentation() : NotificationPresentation.INBOX,
                limit(event.getTitle(), 255),
                event.getBody(),
                event.getActionUrl(),
                writeMetadata(event.getTemplateVariables()),
                dedupeKey,
                event.getCreatedAt()
        );

        return toDTO(userNotificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> listNotifications(
            UUID recipientUuid,
            UserNotificationStatus status,
            NotificationPresentation presentation,
            NotificationType type,
            Boolean popupSeen,
            Pageable pageable
    ) {
        return userNotificationRepository.findAll(
                filter(recipientUuid, status, presentation, type, popupSeen),
                pageable
        ).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountsDTO getCounts(UUID recipientUuid) {
        long unreadCount = userNotificationRepository.countByRecipientUuidAndStatus(
                recipientUuid,
                UserNotificationStatus.UNREAD
        );
        long popupCount = userNotificationRepository.countByRecipientUuidAndStatusAndPresentationAndPopupSeenAtIsNull(
                recipientUuid,
                UserNotificationStatus.UNREAD,
                NotificationPresentation.POPUP
        );
        return new NotificationCountsDTO(unreadCount, popupCount);
    }

    @Override
    public NotificationDTO applyAction(UUID recipientUuid, UUID notificationUuid, String action) {
        UserNotification notification = userNotificationRepository.findByUuidAndRecipientUuid(notificationUuid, recipientUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Notification with UUID %s not found", notificationUuid)));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        switch (normalizeAction(action)) {
            case "read" -> notification.markRead(now);
            case "archive" -> notification.markArchived(now);
            case "popup_seen" -> notification.markPopupSeen(now);
            default -> throw new IllegalArgumentException(
                    "Unsupported notification action: " + action + ". Allowed values: read, archive, popup_seen");
        }
        return toDTO(userNotificationRepository.save(notification));
    }

    @Override
    public NotificationActionResultDTO applyBulkAction(
            UUID recipientUuid,
            String action,
            UserNotificationStatus status,
            NotificationPresentation presentation,
            NotificationType type
    ) {
        String normalizedAction = normalizeAction(action);
        if (!"read_all".equals(normalizedAction)) {
            throw new IllegalArgumentException("Unsupported bulk notification action: " + action + ". Allowed values: read_all");
        }

        int affected = userNotificationRepository.markUnreadAsRead(
                recipientUuid,
                type,
                presentation,
                UserNotificationStatus.UNREAD,
                UserNotificationStatus.READ,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        return new NotificationActionResultDTO(normalizedAction, affected);
    }

    private Specification<UserNotification> filter(
            UUID recipientUuid,
            UserNotificationStatus status,
            NotificationPresentation presentation,
            NotificationType type,
            Boolean popupSeen
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("recipientUuid"), recipientUuid));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (presentation != null) {
                predicates.add(criteriaBuilder.equal(root.get("presentation"), presentation));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("notificationType"), type));
            }
            if (popupSeen != null) {
                if (popupSeen) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("popupSeenAt")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("popupSeenAt")));
                }
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private NotificationDTO toDTO(UserNotification notification) {
        return new NotificationDTO(
                notification.getUuid(),
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getCategory(),
                notification.getPriority(),
                notification.getPresentation(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActionUrl(),
                readMetadata(notification.getMetadataJson()),
                notification.getOccurredAt(),
                notification.getPopupSeenAt(),
                notification.getReadAt(),
                notification.getArchivedAt(),
                notification.getCreatedDate()
        );
    }

    private String resolveDedupeKey(NotificationEvent event) {
        if (StringUtils.hasText(event.getDedupeKey())) {
            return event.getDedupeKey();
        }
        return event.getRecipientId() + ":" + event.getNotificationType() + ":" + event.getNotificationId();
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toLowerCase();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String writeMetadata(Map<String, Object> metadata) {
        Map<String, Object> cleaned = metadata == null ? Map.of() : new LinkedHashMap<>(metadata);
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception ex) {
            log.warn("Failed to serialise notification metadata", ex);
            return "{}";
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Failed to read notification metadata", ex);
            return Map.of();
        }
    }
}
