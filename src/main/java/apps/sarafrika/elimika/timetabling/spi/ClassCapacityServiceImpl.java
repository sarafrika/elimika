package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.spi.ClassCapacityService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Timetabling-backed implementation of {@link ClassCapacityService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassCapacityServiceImpl implements ClassCapacityService {

    private final TimetableService timetableService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @Override
    public boolean hasCapacity(UUID classDefinitionUuid) {
        boolean hasCapacity = timetableService.hasCapacityForClassDefinition(classDefinitionUuid);
        log.debug("Class capacity check for {} -> {}", classDefinitionUuid, hasCapacity);
        return hasCapacity;
    }

    @Override
    public boolean isWaitlistEnabled(UUID classDefinitionUuid) {
        return classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::allowWaitlist)
                .orElse(Boolean.TRUE);
    }
}
