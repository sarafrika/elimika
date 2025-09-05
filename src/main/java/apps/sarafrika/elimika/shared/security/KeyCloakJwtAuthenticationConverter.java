package apps.sarafrika.elimika.shared.security;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class KeyCloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Value("${app.keycloak.admin.clientId}")
    private String CLIENT_KEY;

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES_KEY = "roles";

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt source) {
        log.debug("Converting JWT token to authentication");

        // Extract authorities from JWT
        Collection<GrantedAuthority> authorities = Stream.concat(
                        new JwtGrantedAuthoritiesConverter().convert(source).stream(),
                        extractResourceRoles(source).stream())
                .collect(Collectors.toSet());

        log.debug("Extracted authorities: {}", authorities);

        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
            if (resourceAccess == null) {
                log.debug("No resource_access claim found in JWT");
                return Collections.emptySet();
            }

            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_KEY);
            if (clientAccess == null) {
                log.debug("No client access found for client: {}", CLIENT_KEY);
                return Collections.emptySet();
            }

            log.debug("Extracted client access: {}", clientAccess);

            Collection<String> resourceRoles = (Collection<String>) clientAccess.get(ROLES_KEY);
            if (resourceRoles == null) {
                log.debug("No roles found in client access");
                return Collections.emptySet();
            }

            log.debug("Extracted resource roles: {}", resourceRoles);

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