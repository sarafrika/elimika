package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.TrainingApplicationVenueDTO;
import apps.sarafrika.elimika.course.dto.TrainingRequirementAnswerDTO;
import apps.sarafrika.elimika.course.model.TrainingRateUpdate;
import apps.sarafrika.elimika.course.repository.CourseTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Loads the extras for a page of applications in one query per kind of fact. */
@Component
@RequiredArgsConstructor
public class TrainingApplicationExtrasResolver {

    private final CourseTrainingRateUpdateRepository courseRateUpdates;
    private final ProgramTrainingRateUpdateRepository programRateUpdates;
    private final TrainingApplicationHistory history;
    private final TrainingApplicationOffers offers;

    public TrainingApplicationExtras resolve(TrainingApplicationType type, UUID applicationUuid) {
        if (applicationUuid == null) {
            return TrainingApplicationExtras.NONE;
        }
        return resolve(type, List.of(applicationUuid)).getOrDefault(applicationUuid, TrainingApplicationExtras.NONE);
    }

    public Map<UUID, TrainingApplicationExtras> resolve(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        List<UUID> uuids = applicationUuids.stream().filter(uuid -> uuid != null).distinct().toList();
        if (uuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> pendingUpdates = new HashMap<>();
        List<? extends TrainingRateUpdate> pending = type == TrainingApplicationType.PROGRAM
                ? programRateUpdates.findByApplicationUuidInAndStatus(uuids, TrainingRateUpdateStatus.PENDING)
                : courseRateUpdates.findByApplicationUuidInAndStatus(uuids, TrainingRateUpdateStatus.PENDING);
        pending.forEach(update -> pendingUpdates.putIfAbsent(update.getApplicationUuid(), update.getUuid()));

        Map<UUID, LocalDateTime> firstOpened = history.firstOpenedAt(type, uuids);
        Map<UUID, List<TrainingApplicationVenueDTO>> venues = offers.venues(type, uuids);
        Map<UUID, List<TrainingRequirementAnswerDTO>> answers = offers.answers(type, uuids);

        Map<UUID, TrainingApplicationExtras> extras = new HashMap<>();
        uuids.forEach(uuid -> extras.put(uuid, new TrainingApplicationExtras(pendingUpdates.get(uuid), null,
                firstOpened.get(uuid), venues.getOrDefault(uuid, List.of()), answers.getOrDefault(uuid, List.of()))));
        return extras;
    }
}
