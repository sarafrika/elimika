package apps.sarafrika.elimika.classes.spi;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Optional<UUID> findDefaultInstructorUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getDefaultInstructorUuid);
    }

    @Override
    public Optional<UUID> findOrganisationUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getOrganisationUuid);
    }

    @Override
    public Optional<UUID> findBranchUuid(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return Optional.empty();
        }
        return classDefinitionRepository.findByUuid(classDefinitionUuid)
                .map(ClassDefinition::getBranchUuid);
    }

    @Override
    public Map<UUID, UUID> findOrganisationUuids(Collection<UUID> classDefinitionUuids) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> requested = classDefinitionUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UUID> organisationsByClass = new LinkedHashMap<>();
        for (ClassDefinition classDefinition : classDefinitionRepository.findByUuidIn(requested)) {
            if (classDefinition.getUuid() != null && classDefinition.getOrganisationUuid() != null) {
                organisationsByClass.put(classDefinition.getUuid(), classDefinition.getOrganisationUuid());
            }
        }
        return organisationsByClass;
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
                entity.getSalePrice(),
                entity.getInstructorPay(),
                entity.getRateBasis(),
                entity.getClassVisibility(),
                entity.getLocationType(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist(),
                entity.getClassReminderMinutes()
        );
    }
}
