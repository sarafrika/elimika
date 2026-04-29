package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;

import java.util.List;
import java.util.Objects;
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

    @Override
    public List<UUID> findClassDefinitionUuidsByInstructorUuid(UUID instructorUuid) {
        if (instructorUuid == null) {
            return List.of();
        }
        return classDefinitionRepository.findByDefaultInstructorUuid(instructorUuid)
                .stream()
                .map(ClassDefinition::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public List<UUID> findClassDefinitionUuidsByOrganisationUuid(UUID organisationUuid) {
        if (organisationUuid == null) {
            return List.of();
        }
        return classDefinitionRepository.findByOrganisationUuid(organisationUuid)
                .stream()
                .map(ClassDefinition::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static ClassDefinitionSnapshot toSnapshot(ClassDefinition entity) {
        return new ClassDefinitionSnapshot(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getProgramUuid(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTrainingFee(),
                entity.getClassVisibility(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist()
        );
    }
}
