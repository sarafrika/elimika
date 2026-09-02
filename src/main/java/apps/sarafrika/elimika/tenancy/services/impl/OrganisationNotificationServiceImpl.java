package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.dto.NotificationDispatchDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationNotificationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.NotificationDispatch;
import apps.sarafrika.elimika.tenancy.repository.NotificationDispatchRepository;
import apps.sarafrika.elimika.tenancy.services.OrganisationNotificationService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationNotificationServiceImpl implements OrganisationNotificationService {

    /**
     * The audiences the composer offers, mapped to the {@code domain_in_organisation} values a member
     * is affiliated under — "admin" for organisation staff, "instructor", "student", "parent" — not
     * the platform-level user_domain (an org admin is affiliated as "admin", not "organisation_user").
     */
    private static final Map<String, List<String>> AUDIENCE_DOMAINS = Map.of(
            "all", List.of("student", "instructor", "admin", "parent"),
            "students", List.of("student"),
            "instructors", List.of("instructor"),
            "parents", List.of("parent"),
            "staff", List.of("admin")
    );

    private final UserService userService;
    private final NotificationDispatchRepository dispatchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NotificationDispatchDTO send(UUID organisationUuid, SendOrganisationNotificationRequestDTO request,
                                        UUID senderUserUuid) {
        List<String> domains = AUDIENCE_DOMAINS.getOrDefault(
                request.audience() == null ? "" : request.audience().toLowerCase(), List.of());

        // Unique recipients across the audience's domains; the first domain a user is seen in wins,
        // so the notification lands in that dashboard's inbox.
        Map<UUID, Recipient> recipients = new LinkedHashMap<>();
        for (String domain : domains) {
            for (UserDTO user : userService.getUsersByOrganisationAndDomain(organisationUuid, domain)) {
                if (user.uuid() == null) {
                    continue;
                }
                recipients.putIfAbsent(user.uuid(),
                        new Recipient(user.uuid(), user.email(),
                                fullName(user.firstName(), user.lastName()), domain));
            }
        }

        boolean email = "email".equalsIgnoreCase(request.channel());
        Set<String> channels = email ? Set.of("in_app", "email") : Set.of("in_app");
        Map<String, Object> templateVariables = Map.of("title", request.title(), "body", request.message());

        for (Recipient r : recipients.values()) {
            eventPublisher.publishEvent(new NotificationRequestedEvent(
                    null,
                    r.uuid(),
                    r.email(),
                    r.name(),
                    "ORGANISATION_ANNOUNCEMENT",
                    "NORMAL",
                    "INBOX",
                    request.title(),
                    request.message(),
                    null,
                    templateVariables,
                    channels,
                    null,
                    null,
                    organisationUuid,
                    r.domain()));
        }

        NotificationDispatch dispatch = new NotificationDispatch();
        dispatch.setOrganisationUuid(organisationUuid);
        dispatch.setSenderUserUuid(senderUserUuid);
        dispatch.setAudience(request.audience());
        dispatch.setChannel(request.channel());
        dispatch.setTitle(request.title());
        dispatch.setBody(request.message());
        dispatch.setRecipientCount(recipients.size());
        dispatch.setScheduledAt(request.scheduledAt());
        NotificationDispatch saved = dispatchRepository.save(dispatch);

        log.info("Organisation {} broadcast '{}' to {} recipient(s) via {}",
                organisationUuid, request.title(), recipients.size(), request.channel());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDispatchDTO> listSent(UUID organisationUuid, int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        return dispatchRepository
                .findByOrganisationUuidOrderByCreatedDateDesc(organisationUuid, PageRequest.of(0, capped))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private NotificationDispatchDTO toDto(NotificationDispatch d) {
        return new NotificationDispatchDTO(
                d.getUuid(),
                d.getOrganisationUuid(),
                d.getSenderUserUuid(),
                d.getAudience(),
                d.getChannel(),
                d.getTitle(),
                d.getBody(),
                d.getRecipientCount(),
                d.getScheduledAt(),
                d.getCreatedDate());
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }

    private record Recipient(UUID uuid, String email, String name, String domain) {
    }
}
