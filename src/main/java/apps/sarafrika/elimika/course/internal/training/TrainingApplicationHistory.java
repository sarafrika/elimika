package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.TrainingApplicationEventDTO;
import apps.sarafrika.elimika.course.model.TrainingApplicationEvent;
import apps.sarafrika.elimika.course.repository.TrainingApplicationEventRepository;
import apps.sarafrika.elimika.course.repository.projection.TrainingApplicationFirstOpenedView;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Records and reads the history of training applications, stamping each step with its actor. */
@Component
@RequiredArgsConstructor
public class TrainingApplicationHistory {

    private final TrainingApplicationEventRepository eventRepository;
    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;

    public void record(TrainingApplicationType type, UUID applicationUuid, TrainingApplicationEventType eventType, String note) {
        UUID actorUuid = domainSecurityService.getCurrentUserUuid();
        TrainingApplicationEvent event = new TrainingApplicationEvent();
        event.setApplicationType(type);
        event.setApplicationUuid(applicationUuid);
        event.setEventType(eventType);
        event.setActorUuid(actorUuid);
        event.setActorName(actorName(actorUuid));
        event.setNote(note == null || note.isBlank() ? null : note);
        eventRepository.save(event);
    }

    /** Records the creator's first open; later opens are no-ops. */
    public void recordOpenedByCreator(TrainingApplicationType type, UUID applicationUuid) {
        UUID actorUuid = domainSecurityService.getCurrentUserUuid();
        eventRepository.insertFirstOpenIfAbsent(type.name(), applicationUuid, actorUuid, actorName(actorUuid),
                LocalDateTime.now(ZoneOffset.UTC), currentAuditor());
    }

    public List<TrainingApplicationEventDTO> history(TrainingApplicationType type, UUID applicationUuid) {
        return eventRepository.findByApplicationTypeAndApplicationUuidOrderByCreatedDateDescIdDesc(type, applicationUuid)
                .stream()
                .map(event -> new TrainingApplicationEventDTO(
                        event.getUuid(),
                        event.getApplicationType(),
                        event.getApplicationUuid(),
                        event.getEventType(),
                        event.getActorUuid(),
                        event.getActorName(),
                        event.getNote(),
                        event.getCreatedDate()))
                .toList();
    }

    public Map<UUID, LocalDateTime> firstOpenedAt(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        if (applicationUuids.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findFirstOpened(type, applicationUuids).stream()
                .collect(Collectors.toMap(TrainingApplicationFirstOpenedView::applicationUuid,
                        TrainingApplicationFirstOpenedView::openedAt));
    }

    private String actorName(UUID actorUuid) {
        return actorUuid == null ? null : userLookupService.getUserFullName(actorUuid).orElse(null);
    }

    private static String currentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
