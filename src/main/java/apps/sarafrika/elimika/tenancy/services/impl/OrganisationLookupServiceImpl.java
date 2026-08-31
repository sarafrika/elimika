package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only organisation identity exposed to other modules through the tenancy SPI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganisationLookupServiceImpl implements OrganisationLookupService {

    private final OrganisationRepository organisationRepository;

    @Override
    public Optional<String> findOrganisationName(UUID organisationUuid) {
        if (organisationUuid == null) {
            return Optional.empty();
        }
        return organisationRepository.findByUuid(organisationUuid)
                .map(Organisation::getName)
                .filter(name -> !name.isBlank());
    }

    @Override
    public Map<UUID, String> findOrganisationNames(Collection<UUID> organisationUuids) {
        Collection<UUID> requested = distinct(organisationUuids);
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> names = new LinkedHashMap<>();
        for (Organisation organisation : organisationRepository.findByUuidIn(requested)) {
            String name = organisation.getName();
            if (organisation.getUuid() != null && name != null && !name.isBlank()) {
                names.put(organisation.getUuid(), name);
            }
        }
        return names;
    }

    private Collection<UUID> distinct(Collection<UUID> organisationUuids) {
        if (organisationUuids == null || organisationUuids.isEmpty()) {
            return List.of();
        }
        return organisationUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
