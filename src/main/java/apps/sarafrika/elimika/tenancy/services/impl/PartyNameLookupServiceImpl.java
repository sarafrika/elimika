package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.spi.PartyNameLookupService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Names a person by first and last name and an organisation by its name; no contact details leave here. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyNameLookupServiceImpl implements PartyNameLookupService {

    private final UserRepository userRepository;
    private final OrganisationLookupService organisationLookupService;

    @Override
    public Map<UUID, String> findUserNames(Collection<UUID> userUuids) {
        List<UUID> requested = userUuids == null ? List.of()
                : userUuids.stream().filter(Objects::nonNull).distinct().toList();
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        for (User user : userRepository.findByUuidIn(requested)) {
            String name = Stream.of(user.getFirstName(), user.getLastName())
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
            if (user.getUuid() != null && !name.isEmpty()) {
                names.put(user.getUuid(), name);
            }
        }
        return names;
    }

    @Override
    public Map<UUID, String> findOrganisationNames(Collection<UUID> organisationUuids) {
        return organisationLookupService.findOrganisationNames(organisationUuids);
    }
}
