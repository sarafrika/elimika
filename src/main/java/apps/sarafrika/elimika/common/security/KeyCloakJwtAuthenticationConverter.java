package apps.sarafrika.elimika.common.security;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class KeyCloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Value("${app.keycloak.admin.clientId}")
    private String CLIENT_KEY;


    private static final String RESOURCE_ACCESS = "resource_access";

    private static final String ROLES_KEY = "roles";

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt source) {
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
