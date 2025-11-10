package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of User Lookup Service
 * <p>
 * Provides read-only access to user and domain information.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupServiceImpl implements UserLookupService {

    private final UserRepository userRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;

    @Override
    public Optional<UUID> findUserUuidByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(User::getUuid);
    }

    @Override
    public Optional<UUID> findUserUuidByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUuid);
    }

    @Override
    public boolean userExists(UUID userUuid) {
        return userRepository.existsByUuid(userUuid);
    }

    @Override
    public Optional<String> getUserEmail(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .map(User::getEmail);
    }

    @Override
    public Optional<String> getUserFullName(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .map(user -> user.getFirstName() + " " + user.getLastName());
    }

    @Override
    public Optional<LocalDate> getUserDateOfBirth(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .map(User::getDob);
    }

    @Override
    public boolean userHasDomain(UUID userUuid, UserDomain domain) {
        List<UserDomainMapping> mappings = userDomainMappingRepository.findByUserUuid(userUuid);
        return mappings.stream()
                .anyMatch(mapping -> {
                    String domainName = mapping.getUserDomain().getDomainName();
                    try {
                        return domain == UserDomain.valueOf(domainName);
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                });
    }

    @Override
    public List<UserDomain> getUserDomains(UUID userUuid) {
        List<UserDomainMapping> mappings = userDomainMappingRepository.findByUserUuid(userUuid);
        return mappings.stream()
                .map(mapping -> {
                    String domainName = mapping.getUserDomain().getDomainName();
                    try {
                        return UserDomain.valueOf(domainName);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(domain -> domain != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userHasAnyDomain(UUID userUuid, UserDomain... domains) {
        for (UserDomain domain : domains) {
            if (userHasDomain(userUuid, domain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<UUID> getUserOrganizations(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findByUserUuid(userUuid);
        return mappings.stream()
                .map(UserOrganisationDomainMapping::getOrganisationUuid)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean userBelongsToOrganization(UUID userUuid, UUID organizationUuid) {
        return userOrganisationDomainMappingRepository
                .existsByUserUuidAndOrganisationUuid(userUuid, organizationUuid);
    }

    @Override
    public boolean existsByKeycloakId(String keycloakId) {
        return userRepository.existsByKeycloakId(keycloakId);
    }
}
