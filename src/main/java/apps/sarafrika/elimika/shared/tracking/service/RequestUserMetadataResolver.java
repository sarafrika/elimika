package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.model.RequestUserMetadata;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.List;
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
    private final UserLookupService userLookupService;

    public RequestUserMetadata resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticationName = extractAuthenticationName(authentication);

        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            return RequestUserMetadata.anonymous(authenticationName);
        }

        List<String> domainNames = resolveDomainNames(userUuid);

        String fullName = userLookupService.getUserFullName(userUuid).orElse("Unknown User");
        String email = userLookupService.getUserEmail(userUuid).orElse("unknown@example.com");

        // Get keycloak ID from JWT
        String keycloakId = authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt
                ? jwt.getClaimAsString("sub")
                : null;

        return new RequestUserMetadata(
                userUuid,
                email,
                fullName,
                keycloakId,
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
        return userLookupService.getUserDomains(userUuid).stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
