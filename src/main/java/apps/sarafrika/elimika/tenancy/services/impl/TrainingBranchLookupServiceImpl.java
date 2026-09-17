package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.spi.BranchContact;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only training branch lookups exposed to other modules through the tenancy SPI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingBranchLookupServiceImpl implements TrainingBranchLookupService {

    private final TrainingBranchRepository trainingBranchRepository;

    @Override
    public Optional<BranchLocation> findBranch(UUID organisationUuid, UUID branchUuid) {
        if (organisationUuid == null || branchUuid == null) {
            return Optional.empty();
        }
        return trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(branchUuid, organisationUuid)
                .map(TrainingBranchLookupServiceImpl::toLocation);
    }

    @Override
    public Map<UUID, String> findBranchNames(Collection<UUID> branchUuids) {
        if (branchUuids == null || branchUuids.isEmpty()) {
            return Map.of();
        }
        Set<UUID> requested = branchUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        for (TrainingBranch branch : trainingBranchRepository.findByUuidIn(requested)) {
            if (branch.getUuid() != null && branch.getBranchName() != null) {
                names.putIfAbsent(branch.getUuid(), branch.getBranchName());
            }
        }
        return names;
    }

    @Override
    public Map<UUID, BranchContact> findBranchContacts(Collection<UUID> branchUuids) {
        if (branchUuids == null || branchUuids.isEmpty()) {
            return Map.of();
        }
        Set<UUID> requested = branchUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BranchContact> contacts = new LinkedHashMap<>();
        for (TrainingBranch branch : trainingBranchRepository.findByUuidIn(requested)) {
            BranchContact contact = new BranchContact(branch.getUuid(), blankToNull(branch.getPocName()),
                    blankToNull(branch.getPocTelephone()), blankToNull(branch.getPocEmail()));
            if (branch.getUuid() != null && (contact.name() != null || contact.phone() != null || contact.email() != null)) {
                contacts.putIfAbsent(branch.getUuid(), contact);
            }
        }
        return contacts;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BranchLocation toLocation(TrainingBranch branch) {
        return new BranchLocation(
                branch.getUuid(),
                branch.getOrganisationUuid(),
                branch.getBranchName(),
                branch.getAddress(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.isActive()
        );
    }
}
