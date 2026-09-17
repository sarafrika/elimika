package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationEventDTO;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplicationEvent;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationEventRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationEventType;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Records and reads the history of marketplace job applications, stamping each step with its actor. */
@Component
@RequiredArgsConstructor
public class MarketplaceApplicationHistory {

    private final ClassMarketplaceJobApplicationEventRepository eventRepository;
    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;

    public void record(ClassMarketplaceJobApplication application, ClassMarketplaceJobApplicationEventType eventType,
                       String note, LocalDateTime interviewAt) {
        UUID actorUuid = domainSecurityService.getCurrentUserUuid();
        eventRepository.save(event(application, eventType, note, interviewAt, actorUuid, actorName(actorUuid)));
    }

    public void recordAll(Collection<ClassMarketplaceJobApplication> applications,
                          ClassMarketplaceJobApplicationEventType eventType, String note) {
        if (applications.isEmpty()) {
            return;
        }
        UUID actorUuid = domainSecurityService.getCurrentUserUuid();
        String actorName = actorName(actorUuid);
        eventRepository.saveAll(applications.stream()
                .map(application -> event(application, eventType, note, null, actorUuid, actorName))
                .toList());
    }

    /** For steps nobody took, such as a job lapsing; the actor stays empty whoever is signed in. */
    public void recordAllBySystem(Collection<ClassMarketplaceJobApplication> applications,
                                  ClassMarketplaceJobApplicationEventType eventType, String note) {
        if (applications.isEmpty()) {
            return;
        }
        eventRepository.saveAll(applications.stream()
                .map(application -> event(application, eventType, note, null, null, null))
                .toList());
    }

    public List<ClassMarketplaceJobApplicationEventDTO> history(UUID applicationUuid) {
        return eventRepository.findByApplicationUuidOrderByCreatedDateDescIdDesc(applicationUuid)
                .stream()
                .map(event -> new ClassMarketplaceJobApplicationEventDTO(
                        event.getUuid(),
                        event.getApplicationUuid(),
                        event.getJobUuid(),
                        event.getEventType(),
                        event.getActorUuid(),
                        event.getActorName(),
                        event.getNote(),
                        event.getInterviewAt(),
                        event.getCreatedDate()))
                .toList();
    }

    private static ClassMarketplaceJobApplicationEvent event(ClassMarketplaceJobApplication application,
                                                             ClassMarketplaceJobApplicationEventType eventType,
                                                             String note,
                                                             LocalDateTime interviewAt,
                                                             UUID actorUuid,
                                                             String actorName) {
        ClassMarketplaceJobApplicationEvent event = new ClassMarketplaceJobApplicationEvent();
        event.setApplicationUuid(application.getUuid());
        event.setJobUuid(application.getJobUuid());
        event.setEventType(eventType);
        event.setActorUuid(actorUuid);
        event.setActorName(actorName);
        event.setNote(note == null || note.isBlank() ? null : note);
        event.setInterviewAt(interviewAt);
        return event;
    }

    private String actorName(UUID actorUuid) {
        return actorUuid == null ? null : userLookupService.getUserFullName(actorUuid).orElse(null);
    }
}
