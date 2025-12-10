package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassDefinitionLookupServiceImpl implements ClassDefinitionLookupService {

    private final ClassDefinitionRepository classDefinitionRepository;

    @Override
    public Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionLookupServiceImpl::toSnapshot);
    }

    private static ClassDefinitionSnapshot toSnapshot(ClassDefinition entity) {
        return new ClassDefinitionSnapshot(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTrainingFee(),
                entity.getClassVisibility(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist()
        );
    }
}
