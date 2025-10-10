package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.model.RequestUserMetadata;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestUserMetadataResolver {

    private final DomainSecurityService domainSecurityService;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final UserDomainRepository userDomainRepository;

    public RequestUserMetadata resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticationName = extractAuthenticationName(authentication);

        User user = domainSecurityService.getCurrentUser();
        if (user == null) {
            return RequestUserMetadata.anonymous(authenticationName);
        }

        List<String> domainNames = resolveDomainNames(user.getUuid());

        String fullName = buildFullName(user.getFirstName(), user.getMiddleName(), user.getLastName());

        return new RequestUserMetadata(
                user.getUuid(),
                user.getEmail(),
                fullName,
                user.getKeycloakId(),
                domainNames,
                authenticationName
        );
    }

    private String extractAuthenticationName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private List<String> resolveDomainNames(UUID userUuid) {
        Set<String> domainNames = new LinkedHashSet<>();

        List<UserDomainMapping> standaloneMappings = userDomainMappingRepository.findByUserUuid(userUuid);
        addDomainNames(domainNames, standaloneMappings.stream()
                .map(UserDomainMapping::getUserDomainUuid)
                .collect(Collectors.toSet()));

        List<UserOrganisationDomainMapping> organisationMappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);

        addDomainNames(domainNames, organisationMappings.stream()
                .map(UserOrganisationDomainMapping::getDomainUuid)
                .collect(Collectors.toSet()));

        return new ArrayList<>(domainNames);
    }

    private void addDomainNames(Set<String> domainNames, Set<UUID> domainUuids) {
        for (UUID domainUuid : domainUuids) {
            Optional<UserDomain> domain = userDomainRepository.findByUuid(domainUuid);
            domain.map(UserDomain::getDomainName)
                    .filter(StringUtils::hasText)
                    .ifPresent(domainName -> addDomainNameSafely(domainNames, domainName));
        }
    }

    private void addDomainNameSafely(Set<String> domainNames, String domainName) {
        String normalised = domainName.trim();
        if (StringUtils.hasText(normalised)) {
            domainNames.add(normalised);
        }
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(firstName)) {
            parts.add(firstName.trim());
        }
        if (StringUtils.hasText(middleName)) {
            parts.add(middleName.trim());
        }
        if (StringUtils.hasText(lastName)) {
            parts.add(lastName.trim());
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" ", parts);
    }
}
