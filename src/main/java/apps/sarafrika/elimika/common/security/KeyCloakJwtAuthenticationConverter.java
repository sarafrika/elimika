package apps.sarafrika.elimika.common.security;

import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyCloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Value("${app.keycloak.admin.clientId}")
    private String CLIENT_KEY;

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserService userService;

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES_KEY = "roles";
    private static final String SUB_CLAIM = "sub"; // Keycloak user ID

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt source) {
        // Extract Keycloak user ID from JWT
        String keycloakUserId = source.getClaimAsString(SUB_CLAIM);

        if (keycloakUserId != null) {
            try {
                // Ensure user exists in database
                boolean userExists = userRepository.existsByKeycloakId(keycloakUserId);

                if(userExists){
                    UserRepresentation userRepresentation = keycloakUserService.getUserById(keycloakUserId, "elimika" )
                            .orElseThrow(() -> new RecordNotFoundException("User not found"));

                    userService.createUser(userRepresentation);

                }

                log.info("User verified/created in database");
            } catch (Exception e) {
                log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
            }
        } else {
            log.warn("No 'sub' claim found in JWT token");
        }

        // Extract authorities as before
        Collection<GrantedAuthority> authorities = Stream.concat(
                        new JwtGrantedAuthoritiesConverter().convert(source).stream(),
                        extractResourceRoles(source).stream())
                .collect(Collectors.toSet());

        log.info("Extracted authorities: {}", authorities);

        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
            if (resourceAccess == null) {
                return Collections.emptySet();
            }

            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_KEY);
            if (clientAccess == null) {
                return Collections.emptySet();
            }

            log.info("Extracted client access: {}", clientAccess);

            Collection<String> resourceRoles = (Collection<String>) clientAccess.get(ROLES_KEY);
            if (resourceRoles == null) {
                return Collections.emptySet();
            }

            log.info("Extracted resource roles: {}", resourceRoles);

            return resourceRoles.stream()
                    .filter(Objects::nonNull)
                    .map(role -> new SimpleGrantedAuthority(role.replace("-", ":")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Error extracting resource roles from JWT", e);
            return Collections.emptySet();
        }
    }
}