package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.spi.StudentGroupLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only student group lookups exposed to other modules through the tenancy SPI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGroupLookupServiceImpl implements StudentGroupLookupService {

    private final StudentGroupRepository studentGroupRepository;

    @Override
    public List<UUID> filterGroupsInOrganisation(UUID organisationUuid, Collection<UUID> groupUuids) {
        if (organisationUuid == null) {
            return List.of();
        }
        Collection<UUID> requested = distinct(groupUuids);
        if (requested.isEmpty()) {
            return List.of();
        }
        Map<UUID, StudentGroup> groups = loadGroups(requested);
        return requested.stream()
                .filter(uuid -> {
                    StudentGroup group = groups.get(uuid);
                    return group != null && organisationUuid.equals(group.getOrganisationUuid());
                })
                .toList();
    }

    @Override
    public List<String> getGroupNames(Collection<UUID> groupUuids) {
        Collection<UUID> requested = distinct(groupUuids);
        if (requested.isEmpty()) {
            return List.of();
        }
        Map<UUID, StudentGroup> groups = loadGroups(requested);
        return requested.stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .map(StudentGroup::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private Collection<UUID> distinct(Collection<UUID> groupUuids) {
        if (groupUuids == null || groupUuids.isEmpty()) {
            return List.of();
        }
        return groupUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<UUID, StudentGroup> loadGroups(Collection<UUID> groupUuids) {
        return studentGroupRepository.findByUuidIn(groupUuids).stream()
                .collect(Collectors.toMap(StudentGroup::getUuid, Function.identity(), (first, second) -> first));
    }
}
