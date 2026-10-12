package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Reads the timetables of many classes in two queries, so a learner's dashboard does not call
// GET /classes/{uuid}/schedule once per enrolled class. Access matches that per-class route.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassScheduleBatchService {

    /** Upper bound on class ids per request, matching the other class batch reads. */
    public static final int MAX_BATCH_SIZE = 100;

    private final ClassDefinitionRepository classDefinitionRepository;
    private final ObjectProvider<TimetableService> timetableServiceProvider;

    /** Scheduled instances of the existing classes among {@code classUuids}, by start time. */
    public List<ScheduledInstanceDTO> getSchedules(Collection<UUID> classUuids) {
        Set<UUID> ids = classUuids == null ? Set.of() : classUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("At least one class uuid must be requested");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("At most " + MAX_BATCH_SIZE + " class uuids may be requested at once");
        }
        log.debug("Batch schedule read for {} classes", ids.size());

        List<UUID> existing = classDefinitionRepository.findExistingUuids(ids).stream()
                .distinct()
                .toList();
        TimetableService timetableService = timetableServiceProvider.getIfAvailable();
        if (existing.isEmpty() || timetableService == null) {
            return List.of();
        }
        return timetableService.getScheduledInstancesForClassDefinitions(existing);
    }
}
